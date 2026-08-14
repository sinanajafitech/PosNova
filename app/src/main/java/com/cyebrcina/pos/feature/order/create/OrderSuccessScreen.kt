package com.cyebrcina.pos.feature.order.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.core.util.asDateTime
import com.cyebrcina.pos.core.util.toInstantOrNull

/** Matches Figma's "Order Successful" card: check icon, order number blurb, payment summary, CTAs. */
@Composable
fun OrderSuccessScreen(
    onNewOrder: () -> Unit,
    viewModel: NewOrderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val order = state.completedOrder
    val queuedNumber = state.queuedOfflineOrderNumber

    Box(Modifier.fillMaxSize().background(PosColors.Surface), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(PosColors.White)
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.height(100.dp).width(100.dp).clip(CircleShape)
                    .background(if (queuedNumber != null) PosColors.Warning500 else PosColors.SuccessAccent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = PosColors.White, modifier = Modifier.height(42.dp))
            }
            Spacer(Modifier.height(Spacing.md))
            Text(if (queuedNumber != null) "Order Saved — Offline" else "Order Successful", style = PosTextStyles.h3, color = PosColors.Neutral13)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = if (queuedNumber != null) {
                    "Order #$queuedNumber has been sent to the kitchen and saved on this till — it'll be submitted automatically once you're back online."
                } else {
                    "Order #${order?.number ?: "—"} will be processed in the kitchen, wait for the order ready to serve, thank you :)"
                },
                style = PosTextStyles.bodyMediumMedium,
                color = PosColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.lg))

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PosColors.Surface).padding(Spacing.sm),
            ) {
                SuccessRow("Payment Method", state.paymentMethod.name.lowercase().replaceFirstChar { it.uppercase() })
                Spacer(Modifier.height(Spacing.xs))
                SuccessRow("Payment Time", order?.createdAt?.toInstantOrNull()?.asDateTime() ?: if (queuedNumber != null) "Just now" else "—")
                Spacer(Modifier.height(Spacing.xs))
                HorizontalDivider(color = PosColors.Border)
                Spacer(Modifier.height(Spacing.xs))
                SuccessRow("TOTAL", (order?.total ?: state.total).asCurrency(), emphasize = true)
            }

            state.printWarning?.let {
                Spacer(Modifier.height(Spacing.sm))
                Text(it, style = PosTextStyles.bodyXSmallMedium, color = PosColors.Warning500, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }

            Spacer(Modifier.height(Spacing.lg))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FlowSecondaryButton(text = "New Order", onClick = { viewModel.startNewOrder(); onNewOrder() }, modifier = Modifier.weight(1f))
                FlowPrimaryButton(text = "Print Bills", onClick = viewModel::reprintCompletedOrder, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SuccessRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (emphasize) PosTextStyles.bodyMediumSemibold else PosTextStyles.bodySmallMedium, color = PosColors.Neutral13)
        Text(value, style = if (emphasize) PosTextStyles.h6 else PosTextStyles.bodySmallSemibold, color = PosColors.Neutral13)
    }
}
