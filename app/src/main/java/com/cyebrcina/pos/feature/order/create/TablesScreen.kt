package com.cyebrcina.pos.feature.order.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import com.cyebrcina.pos.data.remote.model.RestaurantTableStatus

private fun RestaurantTableStatus.toVisualState(): TableTileVisualState = when (this) {
    RestaurantTableStatus.AVAILABLE -> TableTileVisualState.AVAILABLE
    RestaurantTableStatus.RESERVED -> TableTileVisualState.RESERVED
    RestaurantTableStatus.CLEANING -> TableTileVisualState.CLEANING
    RestaurantTableStatus.OUT_OF_SERVICE -> TableTileVisualState.OUT_OF_SERVICE
    RestaurantTableStatus.OCCUPIED -> TableTileVisualState.OCCUPIED
}

/**
 * Real table set synced live from Admin (`GET /api/device/tables`) — status refreshes whenever
 * any order event comes in over the socket, same as [TablesViewModel] documents. A locally held
 * new-order draft (started here, not yet sent) is a separate, till-only concept layered on top:
 * it takes visual priority over the real status since it's the more actionable thing for staff
 * ("resume this" beats "this table happens to be free/occupied right now").
 * [ChooseTableDialog] (reached mid-order) still shows the synthetic table set — see
 * `TableLayout.kt`'s own doc comment for why.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TablesScreen(
    onNavigateToNewOrder: () -> Unit,
    viewModel: TablesViewModel = hiltViewModel(),
) {
    val heldOrders by viewModel.heldOrders.collectAsStateWithLifecycle()
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val heldByTable = heldOrders.filter { it.tableLabel != null }.associateBy { it.tableLabel }
    val heldWithoutTable = heldOrders.filter { it.tableLabel == null }

    Scaffold(
        topBar = {
            PosTopBar(
                title = "Tables",
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = PosColors.Neutral9)
                    }
                },
            )
        },
    ) { padding ->
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

            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                LegendDot(color = PosColors.Border, label = "Available")
                LegendDot(color = PosColors.Danger, label = "Occupied")
                LegendDot(color = PosColors.Info500, label = "Reserved")
                LegendDot(color = PosColors.Neutral7, label = "Cleaning")
                LegendDot(color = PosColors.Neutral9, label = "Out of Service")
                LegendDot(color = PosColors.Pending500, label = "Held (this till)")
            }
            Spacer(Modifier.height(Spacing.sm))
            HorizontalDivider(color = PosColors.Border)
            Spacer(Modifier.height(Spacing.sm))

            if (tables.isEmpty()) {
                Text(
                    "No tables set up yet — add them in Admin → Dine-In → Tables.",
                    style = PosTextStyles.bodySmallRegular,
                    color = PosColors.TextSecondary,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    tables.forEach { table ->
                        val held = heldByTable[table.number]
                        val option = TableOption(label = table.number, seats = table.seats, id = table.id)
                        TableTile(
                            table = option,
                            state = if (held != null) TableTileVisualState.HELD else table.status.toVisualState(),
                            badge = held?.let { "${it.itemCount} item${if (it.itemCount == 1) "" else "s"} · ${it.total.asCurrency()}" },
                            onClick = {
                                if (held != null) {
                                    viewModel.resumeHeldOrder(held.id)
                                    onNavigateToNewOrder()
                                } else if (table.status == RestaurantTableStatus.AVAILABLE) {
                                    viewModel.startAtTable(table.number)
                                    onNavigateToNewOrder()
                                }
                                // Occupied/Reserved/Cleaning/Out of Service tables aren't tappable
                                // to start a new order — they're informational until freed up.
                            },
                        )
                    }
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
