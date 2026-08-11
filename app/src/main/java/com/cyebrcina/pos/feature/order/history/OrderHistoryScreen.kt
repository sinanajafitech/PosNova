package com.cyebrcina.pos.feature.order.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppTextField
import com.cyebrcina.pos.core.components.EmptyState
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.core.util.asDateTime
import com.cyebrcina.pos.core.util.exportTransactionsCsv
import com.cyebrcina.pos.core.util.toInstantOrNull
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.feature.order.create.FlowPrimaryButton
import com.cyebrcina.pos.feature.order.create.FlowSecondaryButton

private data class TableColumn(val header: String, val weight: Float)

private val columns = listOf(
    TableColumn("ID", 0.8f),
    TableColumn("Date & Time", 1.6f),
    TableColumn("Customer Name", 1.3f),
    TableColumn("Status", 1f),
    TableColumn("Total Payment", 1.1f),
    TableColumn("Orders", 2f),
    TableColumn("Action", 0.6f),
)

/**
 * Matches Figma's "Transaction" screen (node 12111:93379): a single wide table with search,
 * date filter, export, and pagination — over `GET /api/device/orders/history` (up to the API's
 * 100-order max). Figma's separate "Payment Status" column is dropped: `DeviceOrder` has no
 * reliable per-order payment-status field (a DINE_IN order can be accepted before it's paid), so
 * showing a "Paid" badge on every row would be a guess, not real data. "Status" stays as
 * "Completed" — genuinely true for everything /history returns (it excludes PENDING/CANCELLED).
 */
@Composable
fun OrderHistoryScreen(
    onOpenOrder: (orderId: String) -> Unit,
    viewModel: OrderHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            PosTopBar(
                title = "Transactions",
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = PosColors.Neutral9)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(PosColors.Surface).padding(Spacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                AppTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    label = "",
                    placeholder = "Search order # or customer",
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = PosColors.TextSecondary) },
                    modifier = Modifier.width(320.dp),
                )
                FlowPrimaryButton(
                    text = "Export",
                    onClick = { exportTransactionsCsv(context, state.filtered) },
                    enabled = state.filtered.isNotEmpty(),
                    leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null, tint = PosColors.White, modifier = Modifier.height(18.dp)) },
                )
            }
            Spacer(Modifier.height(Spacing.sm))

            if (state.errorMessage != null) {
                Text(state.errorMessage.orEmpty(), style = PosTextStyles.bodySmallRegular, color = PosColors.Warning500, modifier = Modifier.padding(bottom = Spacing.xs))
            }

            if (state.allOrders.isEmpty() && !state.isLoading) {
                EmptyState(icon = Icons.Filled.History, title = "No transactions yet", description = "Accepted orders will show up here.", modifier = Modifier.fillMaxWidth())
                return@Scaffold
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PosColors.White),
            ) {
                val scrollState = rememberScrollState()
                Column(Modifier.horizontalScroll(scrollState)) {
                    TableHeaderRow()
                    LazyColumn(Modifier.width(1100.dp)) {
                        items(state.pagedOrders, key = { it.id }) { order ->
                            TransactionRow(order, onClick = { onOpenOrder(order.id) })
                            HorizontalDivider(color = PosColors.Border)
                        }
                    }
                }
                if (state.filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(Spacing.lg), contentAlignment = Alignment.Center) {
                        Text("No transactions match your search", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(Spacing.sm))
            PaginationRow(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun TableHeaderRow() {
    Row(Modifier.width(1100.dp).background(PosColors.ImagePlaceholder).padding(vertical = Spacing.xs, horizontal = Spacing.sm)) {
        columns.forEach { col ->
            Text(col.header, style = PosTextStyles.bodyXSmallSemibold, color = PosColors.Neutral12, modifier = Modifier.weight(col.weight))
        }
    }
}

@Composable
private fun TransactionRow(order: DeviceOrder, onClick: () -> Unit) {
    val itemsSummary = order.items.joinToString(", ") { "${it.quantity}x ${it.productName}" }
    Row(
        Modifier.width(1100.dp).padding(vertical = Spacing.sm, horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(order.number, style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral12, modifier = Modifier.weight(columns[0].weight))
        Text(
            order.createdAt.toInstantOrNull()?.asDateTime() ?: order.createdAt,
            style = PosTextStyles.bodySmallRegular,
            color = PosColors.Neutral12,
            modifier = Modifier.weight(columns[1].weight),
        )
        Text(order.customerName, style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral12, modifier = Modifier.weight(columns[2].weight))
        Text("Completed", style = PosTextStyles.bodySmallSemibold, color = PosColors.SuccessAccent, modifier = Modifier.weight(columns[3].weight))
        Text(order.total.asCurrency(), style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12, modifier = Modifier.weight(columns[4].weight))
        Text(
            itemsSummary,
            style = PosTextStyles.bodySmallRegular,
            color = PosColors.TextSecondary,
            maxLines = 1,
            modifier = Modifier.weight(columns[5].weight),
        )
        Box(Modifier.weight(columns[6].weight)) {
            IconButton(onClick = onClick) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "View order", tint = PosColors.Blue500)
            }
        }
    }
}

@Composable
private fun PaginationRow(state: OrderHistoryUiState, viewModel: OrderHistoryViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text("Show", style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral12)
            listOf(10, 25, 50).forEach { size ->
                val selected = state.pageSize == size
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (selected) PosColors.Blue500 else PosColors.Surface)
                        .clickable { viewModel.onPageSizeChange(size) }
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                ) {
                    Text(
                        size.toString(),
                        style = PosTextStyles.bodyXSmallSemibold,
                        color = if (selected) PosColors.White else PosColors.Neutral12,
                    )
                }
                Spacer(Modifier.width(Spacing.xxxs))
            }
            Text("from ${state.filtered.size}", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            FlowSecondaryButton(text = "Prev", onClick = viewModel::previousPage, enabled = state.page > 0)
            Text("Page ${state.page + 1} of ${state.pageCount}", style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral12)
            FlowSecondaryButton(text = "Next", onClick = viewModel::nextPage, enabled = state.page < state.pageCount - 1)
        }
    }
}
