package com.cyebrcina.pos.feature.order.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency

/**
 * Dedicated table-picking entry point: tapping an available table jumps straight into the New
 * Order flow pre-filled with that table (skipping "Create New Order"'s type/table steps); tapping
 * a held table resumes the parked cart. [ChooseTableDialog] (reached from inside an order already
 * in progress) shares the same table set/styling via `TableLayout.kt`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TablesScreen(
    onNavigateToNewOrder: () -> Unit,
    viewModel: TablesViewModel = hiltViewModel(),
) {
    val heldOrders by viewModel.heldOrders.collectAsStateWithLifecycle()
    val heldByTable = heldOrders.filter { it.tableLabel != null }.associateBy { it.tableLabel }
    val heldWithoutTable = heldOrders.filter { it.tableLabel == null }

    Scaffold(topBar = { PosTopBar(title = "Tables") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.sm)) {
            if (heldWithoutTable.isNotEmpty()) {
                Text("Held Orders", style = PosTextStyles.h6, color = PosColors.Neutral12)
                Spacer(Modifier.height(Spacing.xs))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(heldWithoutTable, key = { it.id }) { held ->
                        HeldOrderCard(held) {
                            viewModel.resumeHeldOrder(held.id)
                            onNavigateToNewOrder()
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                LegendDot(color = PosColors.Border, label = "Available")
                LegendDot(color = PosColors.Pending500, label = "Held")
            }
            Spacer(Modifier.height(Spacing.sm))
            HorizontalDivider(color = PosColors.Border)
            Spacer(Modifier.height(Spacing.sm))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                syntheticTables.forEach { table ->
                    val held = heldByTable[table.label]
                    TableTile(
                        table = table,
                        state = if (held != null) TableTileVisualState.HELD else TableTileVisualState.AVAILABLE,
                        badge = held?.let { "${it.itemCount} item${if (it.itemCount == 1) "" else "s"} · ${it.total.asCurrency()}" },
                        onClick = {
                            if (held != null) viewModel.resumeHeldOrder(held.id) else viewModel.startAtTable(table.label)
                            onNavigateToNewOrder()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeldOrderCard(held: HeldOrder, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PosColors.Pending50)
            .clickable(onClick = onClick)
            .padding(Spacing.sm),
    ) {
        Text("Collection", style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral13)
        Text(held.customerName, style = PosTextStyles.bodyXSmallRegular, color = PosColors.TextSecondary)
        Spacer(Modifier.height(Spacing.xs))
        Text("${held.itemCount} item${if (held.itemCount == 1) "" else "s"} · ${held.total.asCurrency()}", style = PosTextStyles.bodyXSmallSemibold, color = PosColors.Pending500)
    }
}
