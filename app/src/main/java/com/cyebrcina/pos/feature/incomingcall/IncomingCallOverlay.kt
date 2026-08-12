package com.cyebrcina.pos.feature.incomingcall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.feature.order.create.FlowPrimaryButton
import com.cyebrcina.pos.feature.order.create.FlowSecondaryButton

private fun formatPhoneForDisplay(phone: String): String = if (phone.length == 10) "0$phone" else phone

/**
 * Mounted once at the top of [com.cyebrcina.pos.core.navigation.MainGraphHost], alongside
 * [com.cyebrcina.pos.feature.waitercall.WaiterCallOverlay], so it can pop up over whichever tab
 * staff happen to be on. Known callers just get a Dismiss; unknown callers can be named/annotated
 * right there so they're recognized on their next call — same capability as Admin's own popup.
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
                modifier = Modifier.width(380.dp).padding(Spacing.lg),
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
                    if (current.isKnown) "Recognized caller" else "Not in the system — add their details below",
                    style = PosTextStyles.bodyMediumRegular,
                    color = PosColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(Spacing.lg))
                if (current.isKnown) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Name", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                        Text(current.callerName ?: "—", style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral13)
                        Spacer(Modifier.height(Spacing.xs))
                        Text("Address", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                        Text(current.address ?: "—", style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral13)
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        placeholder = { Text("Usual order, delivery notes, etc.") },
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
