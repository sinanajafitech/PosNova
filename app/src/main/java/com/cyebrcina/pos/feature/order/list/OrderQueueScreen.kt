package com.cyebrcina.pos.feature.order.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppCard
import com.cyebrcina.pos.core.components.BadgeTone
import com.cyebrcina.pos.core.components.EmptyState
import com.cyebrcina.pos.core.components.StatCard
import com.cyebrcina.pos.core.components.StatusBadge
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosRadius
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.core.util.toBadge
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.data.remote.model.ZReport
import com.cyebrcina.pos.feature.order.create.FlowPrimaryButton

/**
 * Matches Figma's "Dashboard" screen (node 12111:90374): stat cards + a quick-action + a top
 * products panel, above the app's actual job of surfacing pending orders live. Figma's stat
 * cards (Total Sales/Total Product Sales/Total Customers/Net Profit) assume data this API
 * doesn't expose (item counts, customer counts) — remapped to real `ZReport` fields, same as
 * `ReportScreen`, for today vs yesterday. Figma's own 7-day analytics chart is intentionally not
 * duplicated here — that's what the Report tab is for; Top Products (real, from order history)
 * fills that space instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderQueueScreen(
    onOpenOrder: (orderId: String) -> Unit,
    onAddNewOrder: () -> Unit,
    viewModel: OrderQueueViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.session?.storeName?.ifBlank { "Dashboard" } ?: "Dashboard", style = PosTextStyles.h6, color = PosColors.Neutral12)
                        Text("${state.pendingOrders.size} awaiting action", style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = PosColors.Neutral9)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PosColors.White),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(PosColors.Surface),
            contentPadding = PaddingValues(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Today", style = PosTextStyles.h5, color = PosColors.Neutral13)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        SquareIconButton(
                            icon = Icons.Filled.PointOfSale,
                            contentDescription = "Open Cash Drawer",
                            loading = state.isOpeningDrawer,
                            onClick = viewModel::openCashDrawer,
                        )
                        FlowPrimaryButton(
                            text = "Add New Order",
                            onClick = onAddNewOrder,
                            leadingIcon = { Icon(Icons.Filled.AddCircle, contentDescription = null, tint = PosColors.White, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
            }
            state.drawerFeedback?.let { feedback ->
                item {
                    Text(
                        feedback.message,
                        style = PosTextStyles.bodySmallSemibold,
                        color = if (feedback.isError) PosColors.Warning500 else PosColors.Success500,
                    )
                }
            }

            item { DashboardStatRow(state.stats.today, state) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TopProductsCard(state.stats.topProducts, modifier = Modifier.weight(1f))
                }
            }

            item {
                Column {
                    if (state.acceptingOrders == false) {
                        Row(Modifier.fillMaxWidth().padding(bottom = Spacing.xs), horizontalArrangement = Arrangement.Center) {
                            StatusBadge(text = "Store closed to new orders", tone = BadgeTone.WARNING)
                        }
                    }
                    Text("Pending Orders", style = PosTextStyles.h6, color = PosColors.Neutral12)
                }
            }

            if (state.pendingOrders.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.ReceiptLong,
                        title = "No orders waiting",
                        description = "New orders will pop up here the moment they come in.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(state.pendingOrders, key = { it.id }) { order ->
                    PendingOrderRow(order = order, onClick = { onOpenOrder(order.id) })
                }
            }
        }
    }
}

@Composable
private fun DashboardStatRow(report: ZReport?, state: OrderQueueUiState) {
    if (report == null) {
        Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            Text("Loading today's stats…", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StatCard(
            icon = Icons.Filled.AttachMoney,
            iconBg = PosColors.Blue500,
            title = "Total Sales",
            value = report.grossSales.asCurrency(),
            trend = state.trend(report.grossSales) { it.grossSales },
            modifier = Modifier.weight(1f),
        )
        StatCard(
            icon = Icons.Filled.ReceiptLong,
            iconBg = PosColors.SuccessAccent,
            title = "Orders",
            value = report.orderCount.toString(),
            trend = state.trend(report.orderCount.toDouble()) { it.orderCount.toDouble() },
            modifier = Modifier.weight(1f),
        )
        StatCard(
            icon = Icons.Filled.Undo,
            iconBg = PosColors.Amber,
            title = "Refunds",
            value = report.refundsTotal.asCurrency(),
            subtitle = "${report.refundsCount} orders",
            trend = state.trend(report.refundsTotal) { it.refundsTotal },
            positiveIsBad = true,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            icon = Icons.Filled.CreditCard,
            iconBg = PosColors.Cyan,
            title = "Net Sales",
            value = report.netSales.asCurrency(),
            trend = state.trend(report.netSales) { it.netSales },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TopProductsCard(products: List<TopProduct>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(20.dp)).background(PosColors.White).padding(Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(PosColors.Blue500), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Restaurant, contentDescription = null, tint = PosColors.White, modifier = Modifier.size(18.dp))
            }
            Text("Top Products", style = PosTextStyles.h6, color = PosColors.Neutral13)
        }
        Spacer(Modifier.height(Spacing.sm))
        if (products.isEmpty()) {
            Text("No recent order history yet", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
        } else {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PosColors.ImagePlaceholder).padding(Spacing.xs)) {
                Text("Product Name", style = PosTextStyles.bodyXSmallSemibold, color = PosColors.Neutral12, modifier = Modifier.weight(1f))
                Text("Times Ordered", style = PosTextStyles.bodyXSmallSemibold, color = PosColors.Neutral12)
            }
            products.forEach { product ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(PosColors.Blue50), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Restaurant, contentDescription = null, tint = PosColors.Blue500, modifier = Modifier.size(18.dp))
                        }
                        Text(product.name, style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral12)
                    }
                    Text("${product.timesOrdered} times", style = PosTextStyles.bodySmallSemibold, color = PosColors.TextSecondary)
                }
            }
        }
    }
}

/** A square (not pill-shaped) icon-only action button, matched to FlowPrimaryButton's
 * 54.dp height so it sits flush next to it. */
@Composable
private fun SquareIconButton(
    icon: ImageVector,
    contentDescription: String,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(PosRadius.lg))
            .background(if (loading) PosColors.Blue300 else PosColors.Blue500)
            .then(if (loading) Modifier else Modifier.clickable(onClick = onClick)),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = PosColors.White)
        } else {
            Icon(icon, contentDescription = contentDescription, tint = PosColors.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun PendingOrderRow(order: DeviceOrder, onClick: () -> Unit) {
    val presentation = order.type.toBadge()
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(order.number, style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12)
                Text(
                    "${order.items.sumOf { it.quantity }} items · ${order.customerName}",
                    style = PosTextStyles.bodyXSmallRegular,
                    color = PosColors.Neutral7,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(order.total.asCurrency(), style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12)
                StatusBadge(text = presentation.label, tone = presentation.tone)
            }
        }
    }
}
