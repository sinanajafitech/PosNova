package com.cyebrcina.pos.feature.order.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppCard
import com.cyebrcina.pos.core.components.ConfirmDialog
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.components.PrimaryButton
import com.cyebrcina.pos.core.components.SecondaryButton
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.core.util.toBadge
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.data.remote.model.DeviceOrderItem
import com.cyebrcina.pos.core.components.StatusBadge
import com.cyebrcina.pos.payment.model.TerminalStatus
import com.cyebrcina.pos.printer.model.PrintJobState
import kotlinx.coroutines.flow.collectLatest

@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    onActioned: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                OrderDetailEvent.Accepted, OrderDetailEvent.Rejected -> onActioned()
            }
        }
    }

    val order = state.order

    Scaffold(
        topBar = { PosTopBar(title = order?.number ?: "Order", onBack = onBack, navIcon = Icons.AutoMirrored.Filled.ArrowBack) },
    ) { padding ->
        if (order == null) {
            Column(Modifier.fillMaxSize().padding(padding)) {}
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(Spacing.sm)) {
                item { OrderSummaryCard(order) }
                item { Spacer(Modifier.height(Spacing.sm)) }
                item { Text("Items", style = PosTextStyles.h6, color = PosColors.Neutral12) }
                item { Spacer(Modifier.height(Spacing.xxs)) }
                items(order.items) { item -> OrderItemRow(item) }
                item { Spacer(Modifier.height(Spacing.sm)) }
                item { OrderTotalsCard(order) }
                if (state.printJobState != PrintJobState.IDLE) {
                    item { Spacer(Modifier.height(Spacing.sm)) }
                    item { PrintStatusRow(state.printJobState, state.printWarning) }
                }
                if (state.canTakePayment) {
                    item { Spacer(Modifier.height(Spacing.sm)) }
                    item {
                        TakePaymentCard(
                            terminalStatus = state.terminalStatus,
                            isCharging = state.isChargingCard,
                            error = state.paymentError,
                            onCharge = viewModel::chargeCard,
                            onCancel = viewModel::cancelCharge,
                            isLoadingQr = state.paymentQr.isLoading,
                            onShowPaymentQr = viewModel::requestPaymentQr,
                        )
                    }
                }
                if (state.errorMessage != null) {
                    item { Spacer(Modifier.height(Spacing.xxs)) }
                    item { Text(state.errorMessage.orEmpty(), style = PosTextStyles.bodyXSmallMedium, color = PosColors.Warning500) }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (state.isPending) {
                    SecondaryButton(text = "Reject", onClick = viewModel::onRejectRequested, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Accept & Print",
                        onClick = viewModel::accept,
                        loading = state.isProcessing,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    SecondaryButton(
                        text = "Reprint Receipt + Ticket",
                        onClick = viewModel::reprint,
                        loading = state.printJobState == PrintJobState.PRINTING,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.Print, contentDescription = null) },
                    )
                }
            }
        }
    }

    if (state.showRejectConfirm) {
        ConfirmDialog(
            title = "Reject this order?",
            message = "The customer will be notified this order can't be fulfilled. This can't be undone.",
            confirmLabel = "Reject Order",
            onConfirm = viewModel::confirmReject,
            onDismiss = viewModel::onRejectDismissed,
        )
    }

    if (state.paymentQr.link != null || state.paymentQr.error != null) {
        PaymentQrDialog(state = state.paymentQr, onDismiss = viewModel::dismissPaymentQr)
    }
}

@Composable
private fun OrderSummaryCard(order: DeviceOrder) {
    val presentation = order.type.toBadge()
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(order.number, style = PosTextStyles.h6, color = PosColors.Neutral12)
                Text(order.customerName, style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral7)
            }
            StatusBadge(text = presentation.label, tone = presentation.tone)
        }
        Spacer(Modifier.height(Spacing.xxs))
        order.customerPhone?.let { Text(it, style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral8) }
        order.deliveryAddress?.let {
            Spacer(Modifier.height(Spacing.xxxs))
            Text(it, style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral8)
        }
        order.tableLabel?.let {
            Spacer(Modifier.height(Spacing.xxxs))
            Text("Table $it", style = PosTextStyles.bodySmallSemibold, color = PosColors.Primary500)
        }
        order.estimatedTime?.let {
            Spacer(Modifier.height(Spacing.xxxs))
            Text("ETA $it", style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7)
        }
        order.notes?.let {
            Spacer(Modifier.height(Spacing.xxs))
            Text("Note: $it", style = PosTextStyles.bodySmallMedium, color = PosColors.Warning500)
        }
    }
}

@Composable
private fun OrderItemRow(item: DeviceOrderItem) {
    AppCard(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${item.quantity}x ${item.productName}" + (item.sizeLabel?.let { " ($it)" } ?: ""),
                    style = PosTextStyles.bodySmallMedium,
                    color = PosColors.Neutral12,
                )
                if (item.addOnNames.isNotEmpty()) {
                    Text("+ ${item.addOnNames.joinToString(", ")}", style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7)
                }
            }
            Text(item.lineTotal.asCurrency(), style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12)
        }
    }
}

@Composable
private fun OrderTotalsCard(order: DeviceOrder) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral7)
            Text(order.itemsSubtotal.asCurrency(), style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12)
        }
        if (order.otherCharges != 0.0) {
            Spacer(Modifier.height(Spacing.xxxs))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Delivery/other", style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral7)
                Text(order.otherCharges.asCurrency(), style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12)
            }
        }
        Spacer(Modifier.height(Spacing.xxxs))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral12)
            Text(order.total.asCurrency(), style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral12)
        }
    }
}

@Composable
private fun PrintStatusRow(state: PrintJobState, warning: String?) {
    when (state) {
        PrintJobState.SUCCESS -> Text("Receipt + ticket printed", style = PosTextStyles.bodySmallMedium, color = PosColors.Success500)
        PrintJobState.FAILED -> Text(warning ?: "Printing failed", style = PosTextStyles.bodySmallMedium, color = PosColors.Warning500)
        PrintJobState.PRINTING -> Text("Printing…", style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral7)
        PrintJobState.IDLE -> Unit
    }
}

@Composable
private fun TakePaymentCard(
    terminalStatus: TerminalStatus,
    isCharging: Boolean,
    error: String?,
    onCharge: () -> Unit,
    onCancel: () -> Unit,
    isLoadingQr: Boolean,
    onShowPaymentQr: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text("Take Payment", style = PosTextStyles.h6, color = PosColors.Neutral12)
        Spacer(Modifier.height(Spacing.xxs))
        when (terminalStatus) {
            TerminalStatus.AWAITING_CARD -> Text("Present, tap, or insert card", style = PosTextStyles.bodySmallMedium, color = PosColors.Primary500)
            TerminalStatus.PROCESSING -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp, color = PosColors.Primary500)
                    Text("Processing…", style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral9)
                }
            }
            else -> Text("Table order — collect card payment at the till", style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral7)
        }
        if (error != null) {
            Spacer(Modifier.height(Spacing.xxs))
            Text(error, style = PosTextStyles.bodyXSmallMedium, color = PosColors.Warning500)
        }
        Spacer(Modifier.height(Spacing.xs))
        if (isCharging) {
            SecondaryButton(text = "Cancel", onClick = onCancel, modifier = Modifier.fillMaxWidth())
        } else {
            PrimaryButton(
                text = "Charge Card",
                onClick = onCharge,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.CreditCard, contentDescription = null) },
            )
            Spacer(Modifier.height(Spacing.xxs))
            SecondaryButton(
                text = "Show Payment QR",
                onClick = onShowPaymentQr,
                loading = isLoadingQr,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.QrCode, contentDescription = null) },
            )
        }
    }
}

/** Full-screen scan-to-pay QR for a customer to scan and pay by card without the till handling any card details. */
@Composable
private fun PaymentQrDialog(state: PaymentQrState, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = PosColors.White,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Scan to Pay", style = PosTextStyles.h5, color = PosColors.Neutral12)
                Spacer(Modifier.height(Spacing.sm))
                when {
                    state.link != null -> {
                        coil.compose.AsyncImage(
                            model = com.cyebrcina.pos.core.image.imageModel(state.link.qrCodeDataUrl),
                            contentDescription = "Payment QR code",
                            modifier = Modifier.size(320.dp),
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Text("Ask the customer to scan with their phone camera", style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral7)
                    }
                    state.error != null -> {
                        Text(state.error, style = PosTextStyles.bodySmallMedium, color = PosColors.Warning500)
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                SecondaryButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
