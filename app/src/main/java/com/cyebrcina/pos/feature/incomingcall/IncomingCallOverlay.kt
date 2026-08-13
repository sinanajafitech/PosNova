package com.cyebrcina.pos.feature.incomingcall

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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppTextField
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.data.remote.realtime.IncomingCallOrderItem
import com.cyebrcina.pos.feature.order.create.FlowPrimaryButton
import com.cyebrcina.pos.feature.order.create.FlowSecondaryButton
import java.util.Locale

private fun formatPhoneForDisplay(phone: String): String = if (phone.length == 10) "0$phone" else phone
private fun formatMoney(amount: Double): String = String.format(Locale.UK, "£%.2f", amount)
private fun formatOrderItems(items: List<IncomingCallOrderItem>): String =
    items.joinToString(", ") { "${it.quantity}x ${it.productName}${it.sizeLabel?.let { s -> " ($s)" } ?: ""}" }

/**
 * Mounted once at the top of [com.cyebrcina.pos.core.navigation.MainGraphHost], alongside
 * [com.cyebrcina.pos.feature.waitercall.WaiterCallOverlay], so it can pop up over whichever tab
 * staff happen to be on. Known callers see their full Customer Intelligence profile (order
 * history, usual order, addresses, delivery zone — see Admin's lib/customer-profile.ts, which
 * this all comes from over the socket); unknown callers can be named/annotated right there so
 * they're recognized on their next call — same capability as Admin's own popup.
 */
@Composable
fun IncomingCallOverlay(viewModel: IncomingCallViewModel = hiltViewModel(), onTakeOrder: () -> Unit = {}) {
    val event by viewModel.current.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val current = event ?: return

    var name by remember(current.phone) { mutableStateOf(current.callerName ?: "") }
    var address by remember(current.phone) { mutableStateOf(current.address ?: "") }
    var notes by remember(current.phone) { mutableStateOf("") }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = PosColors.White) {
            Column(
                modifier = Modifier.width(380.dp).heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(PosColors.Blue500),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, tint = PosColors.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    formatPhoneForDisplay(current.phone),
                    style = PosTextStyles.h5,
                    color = PosColors.Neutral13,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    if (current.isKnown) "🟢 Recognized caller" else "🟡 Not in the system — add their details below",
                    style = PosTextStyles.bodyMediumRegular,
                    color = PosColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(Spacing.lg))
                if (current.isKnown) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Name", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                        Text(current.callerName ?: "—", style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral13)

                        if (current.tags.isNotEmpty()) {
                            Spacer(Modifier.height(Spacing.xxs))
                            Text(current.tags.joinToString(" · "), style = PosTextStyles.bodySmallRegular, color = PosColors.Blue500)
                        }

                        if (current.orderCount > 0) {
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                "${current.orderCount} orders · ${formatMoney(current.totalSpent)} total",
                                style = PosTextStyles.bodySmallRegular,
                                color = PosColors.TextSecondary,
                            )
                        }

                        current.usualOrder?.takeIf { it.isNotEmpty() }?.let { usual ->
                            Spacer(Modifier.height(Spacing.xs))
                            Text("Usual Order", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                            Text(formatOrderItems(usual), style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral13)
                        }

                        current.lastOrder?.let { last ->
                            Spacer(Modifier.height(Spacing.xs))
                            Text("Last Order · #${last.number} · ${formatMoney(last.total)}", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                        }

                        val primaryAddress = current.addresses.firstOrNull()
                        Spacer(Modifier.height(Spacing.xs))
                        Text("Address", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                        Text(
                            primaryAddress?.let { listOfNotNull(it.line1, it.line2, it.postcode).joinToString(", ") }
                                ?: current.address
                                ?: "—",
                            style = PosTextStyles.bodyMediumSemibold,
                            color = PosColors.Neutral13,
                        )
                        if (current.addresses.size > 1) {
                            Text("+${current.addresses.size - 1} more saved", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                        }

                        current.deliveryZone?.let { zone ->
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                "${zone.name} · fee ${formatMoney(zone.fee)} · min ${formatMoney(zone.minOrder)}" +
                                    (zone.etaMinutes?.let { " · ~$it min" } ?: ""),
                                style = PosTextStyles.bodySmallRegular,
                                color = PosColors.TextSecondary,
                            )
                        }

                        current.notes?.takeIf { it.isNotBlank() }?.let { text ->
                            Spacer(Modifier.height(Spacing.xs))
                            Text("\"$text\"", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                        }
                    }
                } else {
                    AppTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Name",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    AppTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Address",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    AppTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Notes",
                        placeholder = "Usual order, delivery notes, etc.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(Spacing.lg))
                Row(modifier = Modifier.fillMaxWidth()) {
                    FlowSecondaryButton(
                        text = "Dismiss",
                        onClick = viewModel::dismissCurrent,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                    )
                    if (!current.isKnown) {
                        Spacer(Modifier.width(Spacing.sm))
                        FlowSecondaryButton(
                            text = "Save Customer",
                            onClick = { viewModel.saveContact(name, address, notes) },
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving,
                        )
                    }
                }
                if (current.lastOrder != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    FlowSecondaryButton(
                        text = "Repeat Last Order",
                        onClick = {
                            viewModel.repeatLastOrder()
                            onTakeOrder()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                FlowPrimaryButton(
                    text = "Take Order",
                    onClick = {
                        viewModel.takeOrder()
                        onTakeOrder()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                )
            }
        }
    }
}
