package com.cyebrcina.pos.feature.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.BadgeTone
import com.cyebrcina.pos.core.components.EmptyState
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.components.StatusBadge
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asDateTime
import com.cyebrcina.pos.core.util.toInstantOrNull
import com.cyebrcina.pos.data.remote.model.DeviceCall
import com.cyebrcina.pos.data.remote.model.DeviceCallOrder

private fun formatPhoneForDisplay(phone: String): String = if (phone.length == 10) "0$phone" else phone

/**
 * Recent calls for the restaurant's phone line — mirrors Admin's own Calls page, over
 * `GET /api/device/calls`. Tapping any row opens [OrderListDialog], listing the order(s) that
 * call turned into (via the Incoming Call popup's "Take Order"), or a "no orders" message.
 */
@Composable
fun CallsScreen(
    onOpenOrder: (orderId: String) -> Unit,
    viewModel: CallsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var callShowingOrders by remember { mutableStateOf<DeviceCall?>(null) }

    Scaffold(
        topBar = {
            PosTopBar(
                title = "Calls",
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = PosColors.Neutral9)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(PosColors.Surface).padding(Spacing.sm)) {
            if (state.errorMessage != null) {
                Text(
                    state.errorMessage.orEmpty(),
                    style = PosTextStyles.bodySmallRegular,
                    color = PosColors.Warning500,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
            }

            if (state.calls.isEmpty() && !state.isLoading) {
                EmptyState(
                    icon = Icons.Filled.Phone,
                    title = "No calls yet",
                    description = "Incoming calls will show up here.",
                    modifier = Modifier.fillMaxWidth(),
                )
                return@Scaffold
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PosColors.White),
            ) {
                LazyColumn {
                    items(state.calls, key = { it.id }) { call ->
                        CallRow(call, onClick = { callShowingOrders = call })
                        HorizontalDivider(color = PosColors.Border)
                    }
                }
            }
        }
    }

    callShowingOrders?.let { call ->
        OrderListDialog(
            call = call,
            onDismiss = { callShowingOrders = null },
            onOpenOrder = { orderId ->
                callShowingOrders = null
                onOpenOrder(orderId)
            },
        )
    }
}

@Composable
private fun CallRow(call: DeviceCall, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm, horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(formatPhoneForDisplay(call.phone), style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral12)
            val subtitle = listOfNotNull(call.callerName, call.address).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = PosTextStyles.bodyXSmallRegular, color = PosColors.TextSecondary)
            }
        }
        StatusBadge(
            text = if (call.isKnown) "Known" else "Unknown",
            tone = if (call.isKnown) BadgeTone.SUCCESS else BadgeTone.NEUTRAL,
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            call.receivedAt.toInstantOrNull()?.asDateTime() ?: call.receivedAt,
            style = PosTextStyles.bodySmallRegular,
            color = PosColors.TextSecondary,
            modifier = Modifier.width(140.dp),
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = "View orders", tint = PosColors.Blue500)
    }
}

/** Lists the order(s) a call turned into (almost always exactly one, sometimes none) —
 * tapping one navigates to the existing [com.cyebrcina.pos.feature.order.detail.OrderDetailScreen]. */
@Composable
private fun OrderListDialog(call: DeviceCall, onDismiss: () -> Unit, onOpenOrder: (orderId: String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = PosColors.Surface) {
            Column(Modifier.width(420.dp).padding(Spacing.lg)) {
                Text(
                    "Orders from ${formatPhoneForDisplay(call.phone)}",
                    style = PosTextStyles.h6,
                    color = PosColors.Neutral12,
                )
                Spacer(Modifier.height(Spacing.sm))
                if (call.orders.isEmpty()) {
                    Text(
                        "No orders for this call.",
                        style = PosTextStyles.bodySmallRegular,
                        color = PosColors.TextSecondary,
                    )
                } else {
                    call.orders.forEach { order ->
                        OrderListRow(order, onClick = { onOpenOrder(order.id) })
                        HorizontalDivider(color = PosColors.Border)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderListRow(order: DeviceCallOrder, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(order.number, style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral12)
        Icon(Icons.Filled.ChevronRight, contentDescription = "View order", tint = PosColors.Blue500)
    }
}
