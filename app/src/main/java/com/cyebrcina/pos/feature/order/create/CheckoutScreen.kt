package com.cyebrcina.pos.feature.order.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppTextField
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.payment.model.TerminalStatus

/** Matches Figma's "Payment Order": order summary + totals card on the left, payment method on the right. */
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOrderComplete: () -> Unit,
    viewModel: NewOrderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.completedOrder) {
        if (state.completedOrder != null) onOrderComplete()
    }

    Scaffold(topBar = { PosTopBar(title = "Payment Order", onBack = onBack, navIcon = Icons.AutoMirrored.Filled.ArrowBack) }) { padding ->
        Row(Modifier.fillMaxSize().padding(padding).padding(Spacing.lg)) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(PosColors.Surface).padding(Spacing.sm),
            ) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(state.cart, key = { it.lineId }) { item ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PosColors.White).padding(Spacing.xs), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("${item.quantity}× ${item.product.name}", style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral13)
                                val extras = listOfNotNull(item.size?.label) + item.addOns.map { it.name }
                                if (extras.isNotEmpty()) Text(extras.joinToString(", "), style = PosTextStyles.bodyXSmallRegular, color = PosColors.TextSecondary)
                            }
                            Text(item.lineTotal.asCurrency(), style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral13)
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PosColors.SurfaceAlt).padding(Spacing.sm),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sub Total", style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral13)
                        Text(state.subtotal.asCurrency(), style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral13)
                    }
                    Spacer(Modifier.height(Spacing.xxs))
                    HorizontalDivider(color = PosColors.Border)
                    Spacer(Modifier.height(Spacing.xxs))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL", style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral13)
                        Text(state.total.asCurrency(), style = PosTextStyles.h5, color = PosColors.Neutral13)
                    }
                }
            }

            Spacer(Modifier.width(Spacing.lg))

            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Text("Payment Method", style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral13)
                Spacer(Modifier.height(Spacing.xs))
                SegmentedTabBar(
                    options = listOf(TillPaymentMethod.CASH to "Cash", TillPaymentMethod.CARD to "Card", TillPaymentMethod.QR to "QR"),
                    selected = state.paymentMethod,
                    onSelected = viewModel::onPaymentMethodChanged,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.lg))

                when (state.paymentMethod) {
                    TillPaymentMethod.CASH -> CashPaymentSection(state, viewModel)
                    TillPaymentMethod.CARD -> CardPaymentSection(state, viewModel)
                    TillPaymentMethod.QR -> QrPaymentSection(state, viewModel)
                }

                state.submitError?.let {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(it, style = PosTextStyles.bodySmallMedium, color = PosColors.Warning500)
                }
            }
        }
    }
}

@Composable
private fun CashPaymentSection(state: NewOrderUiState, viewModel: NewOrderViewModel) {
    AppTextField(
        value = state.cashTendered,
        onValueChange = viewModel::onCashTenderedChange,
        label = "Cash Tendered",
        placeholder = state.total.asCurrency(),
        keyboardType = KeyboardType.Decimal,
    )
    Spacer(Modifier.height(Spacing.xs))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        cashPresets(state.total).forEach { amount ->
            CashPresetChip(
                label = if (amount == state.total) "Exact" else amount.asCurrency(),
                onClick = { viewModel.onCashTenderedChange("%.2f".format(amount)) },
            )
        }
    }
    Spacer(Modifier.height(Spacing.sm))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Change", style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral13)
        Text(state.change.asCurrency(), style = PosTextStyles.bodyMediumSemibold, color = PosColors.Blue500)
    }
    Spacer(Modifier.height(Spacing.lg))
    FlowPrimaryButton(
        text = if (state.isSubmitting) "Submitting…" else "Confirm Cash Payment",
        onClick = viewModel::confirmCashPayment,
        enabled = state.canConfirmCashPayment && !state.isSubmitting,
        loading = state.isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CardPaymentSection(state: NewOrderUiState, viewModel: NewOrderViewModel) {
    Box(
        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(17.dp)).background(PosColors.ImagePlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.terminalStatus == TerminalStatus.CONNECTING || state.terminalStatus == TerminalStatus.AWAITING_CARD || state.terminalStatus == TerminalStatus.PROCESSING) {
                CircularProgressIndicator(color = PosColors.Blue500)
            } else {
                Box(Modifier.height(60.dp).width(60.dp).clip(CircleShape).background(PosColors.Blue100), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CreditCard, contentDescription = null, tint = PosColors.Blue500)
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(terminalStatusLabel(state.terminalStatus), style = PosTextStyles.bodySmallMedium, color = PosColors.TextSecondary)
        }
    }
    Spacer(Modifier.height(Spacing.lg))
    state.cardChargeError?.let {
        Text(it, style = PosTextStyles.bodySmallMedium, color = PosColors.Warning500)
        Spacer(Modifier.height(Spacing.sm))
    }
    // Only ever shown when the last attempt failed specifically because the
    // provider isn't configured (not a real decline) — see chargeCard()'s
    // own comment on that distinction.
    if (state.cardChargeSdkNotConfigured) {
        Text(
            "No real card terminal is set up yet. You can record this as paid by card without it, or try again once it's configured.",
            style = PosTextStyles.bodyXSmallRegular,
            color = PosColors.TextSecondary,
        )
        Spacer(Modifier.height(Spacing.xs))
        FlowSecondaryButton(
            text = "Record as Card (manual)",
            onClick = viewModel::submitManualCardPayment,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))
    }
    FlowPrimaryButton(
        text = if (state.isSubmitting) "Submitting…" else "Charge ${state.total.asCurrency()}",
        onClick = viewModel::chargeCard,
        enabled = !state.isChargingCard && !state.isSubmitting,
        loading = state.isChargingCard || state.isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * NOT YET LIVE server-side — see BACKEND_ORDER_CREATE_SPEC.md's "QR payment" section. Creates the
 * order unpaid, shows the scan-to-pay QR, and waits for [NewOrderViewModel]'s pending-orders
 * watcher to detect the order's `paymentStatus` flipping to "PAID" (navigation to Order Successful
 * happens automatically via the same `completedOrder` mechanism Cash/Card use).
 */
@Composable
private fun QrPaymentSection(state: NewOrderUiState, viewModel: NewOrderViewModel) {
    val qr = state.qrPayment
    Box(
        modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(17.dp)).background(PosColors.ImagePlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        when {
            qr.link != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    coil.compose.AsyncImage(
                        model = com.cyebrcina.pos.core.image.imageModel(qr.link.qrCodeDataUrl),
                        contentDescription = "Payment QR code",
                        modifier = Modifier.size(200.dp),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        CircularProgressIndicator(modifier = Modifier.height(14.dp), strokeWidth = 2.dp, color = PosColors.Blue500)
                        Text("Waiting for customer to pay…", style = PosTextStyles.bodySmallMedium, color = PosColors.TextSecondary)
                    }
                }
            }
            qr.isGenerating -> CircularProgressIndicator(color = PosColors.Blue500)
            qr.error != null -> Text(qr.error, style = PosTextStyles.bodySmallMedium, color = PosColors.Warning500, modifier = Modifier.padding(Spacing.sm))
            else -> Text("Tap below to create a scan-to-pay QR code", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
        }
    }
    Spacer(Modifier.height(Spacing.lg))
    when {
        qr.link != null -> FlowSecondaryButton(text = "Cancel", onClick = viewModel::cancelQrPayment, modifier = Modifier.fillMaxWidth())
        qr.error != null -> FlowPrimaryButton(
            text = "Retry",
            onClick = { if (qr.order != null) viewModel.retryPaymentLink() else viewModel.generateQrPayment() },
            modifier = Modifier.fillMaxWidth(),
        )
        else -> FlowPrimaryButton(
            text = "Generate QR Code",
            onClick = viewModel::generateQrPayment,
            enabled = !qr.isGenerating,
            loading = qr.isGenerating,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun terminalStatusLabel(status: TerminalStatus): String = when (status) {
    TerminalStatus.DISCONNECTED -> "Terminal ready — tap Charge to start"
    TerminalStatus.CONNECTING -> "Connecting to terminal…"
    TerminalStatus.READY -> "Terminal connected"
    TerminalStatus.AWAITING_CARD -> "Tap, insert, or swipe card"
    TerminalStatus.PROCESSING -> "Processing payment…"
    TerminalStatus.ERROR -> "Terminal error — try again"
}

private val NewOrderUiState.isChargingCard: Boolean
    get() = terminalStatus == TerminalStatus.CONNECTING || terminalStatus == TerminalStatus.AWAITING_CARD || terminalStatus == TerminalStatus.PROCESSING

/** Exact total, plus the next £5/£10/£20/£50 note above it — standard cash-drawer quick amounts. */
private fun cashPresets(total: Double): List<Double> {
    if (total <= 0.0) return emptyList()
    val notes = listOf(5.0, 10.0, 20.0, 50.0)
    val roundedUp = notes.map { note -> kotlin.math.ceil(total / note) * note }.filter { it >= total }
    return (listOf(total) + roundedUp).distinct().sorted().take(5)
}

@Composable
private fun CashPresetChip(label: String, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = PillShape,
        color = PosColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, PosColors.Border),
    ) {
        Text(
            text = label,
            style = PosTextStyles.bodySmallSemibold,
            color = PosColors.Neutral13,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        )
    }
}
