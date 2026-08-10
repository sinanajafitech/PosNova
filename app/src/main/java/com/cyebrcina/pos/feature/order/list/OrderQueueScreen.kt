package com.cyebrcina.pos.feature.order.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppCard
import com.cyebrcina.pos.core.components.BadgeTone
import com.cyebrcina.pos.core.components.EmptyState
import com.cyebrcina.pos.core.components.StatusBadge
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.core.util.toBadge
import com.cyebrcina.pos.data.remote.model.DeviceOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderQueueScreen(
    onOpenOrder: (orderId: String) -> Unit,
    viewModel: OrderQueueViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.session?.storeName?.ifBlank { "Orders" } ?: "Orders", style = PosTextStyles.h6, color = PosColors.Neutral12)
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.acceptingOrders == false) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    StatusBadge(text = "Store closed to new orders", tone = BadgeTone.WARNING)
                }
            }
            if (state.pendingOrders.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No orders waiting",
                    description = "New orders will pop up here the moment they come in.",
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                ) {
                    items(state.pendingOrders, key = { it.id }) { order ->
                        PendingOrderRow(order = order, onClick = { onOpenOrder(order.id) })
                    }
                }
            }
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
