package com.cyebrcina.pos.feature.order.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cyebrcina.pos.core.components.AppTextField
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing

/** Matches Figma's "Popup Create New Order": tabbar (order type) + customer/guest/table inputs + CTA. */
@Composable
fun StartOrderDialog(
    initialGuestCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (TillOrderType, String, Int, String?) -> Unit,
) {
    var type by remember { mutableStateOf(TillOrderType.DINE_IN) }
    var customerName by remember { mutableStateOf("") }
    var guestCount by remember { mutableIntStateOf(initialGuestCount) }
    var table by remember { mutableStateOf<String?>(null) }
    var showChooseTable by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = PosColors.White) {
            Column(Modifier.width(500.dp).padding(Spacing.lg)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Create New Order", style = PosTextStyles.h3, color = PosColors.Neutral13)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = PosColors.Neutral13)
                    }
                }
                Spacer(Modifier.height(Spacing.sm))

                SegmentedTabBar(
                    options = listOf(TillOrderType.DINE_IN to "Dine In", TillOrderType.COLLECTION to "Collection"),
                    selected = type,
                    onSelected = { selected -> type = selected; if (selected == TillOrderType.COLLECTION) table = null },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.sm))

                AppTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = "Customer Name",
                    placeholder = "Walk-in",
                )
                Spacer(Modifier.height(Spacing.sm))

                Column(Modifier.fillMaxWidth()) {
                    Text("Guest", style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral10)
                    Spacer(Modifier.height(Spacing.xxxs))
                    Row(
                        Modifier.fillMaxWidth().height(48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        QuantityStepper(
                            quantity = guestCount,
                            onIncrement = { guestCount++ },
                            onDecrement = { if (guestCount > 1) guestCount-- },
                        )
                        Text("$guestCount Person${if (guestCount == 1) "" else "s"}", style = PosTextStyles.bodyMediumRegular, color = PosColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(Spacing.sm))

                if (type == TillOrderType.DINE_IN) {
                    PillSelector(
                        label = table ?: "Select table",
                        onClick = { showChooseTable = true },
                        modifier = Modifier.fillMaxWidth(),
                        trailing = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = PosColors.TextSecondary) },
                    )
                    Spacer(Modifier.height(Spacing.sm))
                }

                FlowPrimaryButton(
                    text = "Create Order",
                    onClick = { onConfirm(type, customerName, guestCount, table) },
                    enabled = type == TillOrderType.COLLECTION || !table.isNullOrBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showChooseTable) {
        ChooseTableDialog(
            selectedTable = table,
            guestCount = guestCount,
            customerName = customerName.ifBlank { "Walk-in" },
            onDismiss = { showChooseTable = false },
            onConfirm = { selected -> table = selected; showChooseTable = false },
        )
    }
}
