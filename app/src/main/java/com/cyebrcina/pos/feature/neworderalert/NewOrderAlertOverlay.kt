package com.cyebrcina.pos.feature.neworderalert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.feature.order.create.FlowPrimaryButton
import com.cyebrcina.pos.feature.order.create.FlowSecondaryButton

private fun formatItems(order: DeviceOrder): String =
    order.items.joinToString(", ") { "${it.quantity}x ${it.productName}${it.sizeLabel?.let { s -> " ($s)" } ?: ""}" }

/**
 * Mounted once at the top of [com.cyebrcina.pos.core.navigation.MainGraphHost], alongside
 * [com.cyebrcina.pos.feature.incomingcall.IncomingCallOverlay]/
 * [com.cyebrcina.pos.feature.waitercall.WaiterCallOverlay], so a new order pops up over whichever
 * tab staff are already on rather than only being visible from the Dashboard tab.
 */
@Composable
fun NewOrderAlertOverlay(viewModel: NewOrderAlertViewModel = hiltViewModel(), onViewOrder: (orderId: String) -> Unit = {}) {
    val order by viewModel.current.collectAsStateWithLifecycle()
    val current = order ?: return

    Dialog(
        onDismissRequest = viewModel::dismissCurrent,
        properties = DialogProperties(dismissOnBackPress = false),
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = PosColors.White) {
            Column(
                modifier = Modifier.width(380.dp).heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(PosColors.SuccessAccent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = PosColors.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(Spacing.sm))
                Text("New Order", style = PosTextStyles.h5, color = PosColors.Neutral13, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    "#${current.number} · ${current.typeLabel}",
                    style = PosTextStyles.bodyMediumRegular,
                    color = PosColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(Spacing.lg))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Customer", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                    Text(current.customerName, style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral13)

                    current.tableLabel?.let { table ->
                        Spacer(Modifier.height(Spacing.xs))
                        Text("Table", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                        Text(table, style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral13)
                    }

                    current.deliveryAddress?.let { address ->
                        Spacer(Modifier.height(Spacing.xs))
                        Text("Address", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                        Text(address, style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral13)
                    }

                    if (current.items.isNotEmpty()) {
                        Spacer(Modifier.height(Spacing.xs))
                        Text("Items", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                        Text(formatItems(current), style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral13)
                    }

                    Spacer(Modifier.height(Spacing.xs))
                    Text("Total", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                    Text(current.total.asCurrency(), style = PosTextStyles.h6, color = PosColors.Blue500)
                }

                Spacer(Modifier.height(Spacing.lg))
                Row(modifier = Modifier.fillMaxWidth()) {
                    FlowSecondaryButton(text = "Dismiss", onClick = viewModel::dismissCurrent, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(Spacing.sm))
                FlowPrimaryButton(
                    text = "View Order",
                    onClick = {
                        val orderId = current.id
                        viewModel.dismissCurrent()
                        onViewOrder(orderId)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
