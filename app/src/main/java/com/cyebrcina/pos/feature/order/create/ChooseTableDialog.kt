package com.cyebrcina.pos.feature.order.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.cyebrcina.pos.core.components.AppTextField
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing

/** Matches Figma's "Choose Table" popup — table set/tile styling shared with [TablesScreen] via [TableLayout.kt]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChooseTableDialog(
    selectedTable: String?,
    guestCount: Int,
    customerName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pending by remember(selectedTable) { mutableStateOf(selectedTable) }
    var customEntry by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = PosColors.Surface) {
            Column(Modifier.width(900.dp).padding(Spacing.lg)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Select Table", style = PosTextStyles.h4, color = PosColors.Neutral13)
                        Text("${syntheticTables.size} tables", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = PosColors.Neutral13)
                    }
                }
                Spacer(Modifier.height(Spacing.sm))

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    LegendDot(color = PosColors.Border, label = "Available")
                    LegendDot(color = PosColors.Blue500, label = "Selected")
                }
                Spacer(Modifier.height(Spacing.sm))
                HorizontalDivider(color = PosColors.Border)
                Spacer(Modifier.height(Spacing.sm))

                FlowRow(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    syntheticTables.forEach { table ->
                        TableTile(
                            table = table,
                            state = if (table.label == pending) TableTileVisualState.SELECTED else TableTileVisualState.AVAILABLE,
                            onClick = { pending = table.label },
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.sm))
                AppTextField(
                    value = customEntry,
                    onValueChange = { customEntry = it; if (it.isNotBlank()) pending = it },
                    label = "Other table",
                    placeholder = "e.g. Patio 2",
                )
                Spacer(Modifier.height(Spacing.sm))
                HorizontalDivider(color = PosColors.Border)
                Spacer(Modifier.height(Spacing.sm))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PosColors.White)
                        .padding(Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Box(
                            modifier = Modifier.height(56.dp).width(56.dp).clip(CircleShape).background(PosColors.Blue100),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = PosColors.Blue500)
                        }
                        Column {
                            Text("$guestCount Guest${if (guestCount == 1) "" else "s"}", style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral13)
                            Text(customerName, style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                        }
                    }
                    FlowPrimaryButton(
                        text = "Select Table",
                        enabled = !pending.isNullOrBlank(),
                        onClick = { pending?.let(onConfirm) },
                    )
                }
            }
        }
    }
}
