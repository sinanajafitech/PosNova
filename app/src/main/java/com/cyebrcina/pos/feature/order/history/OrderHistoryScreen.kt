package com.cyebrcina.pos.feature.order.history

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppCard
import com.cyebrcina.pos.core.components.EmptyState
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.components.StatusBadge
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.core.util.toBadge
import com.cyebrcina.pos.data.remote.model.DeviceOrder

@Composable
fun OrderHistoryScreen(
    onOpenOrder: (orderId: String) -> Unit,
    viewModel: OrderHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            PosTopBar(
                title = "Order History",
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = PosColors.Neutral9)
                    }
                },
            )
        },
    ) { padding ->
        if (state.orders.isEmpty() && !state.isLoading) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(icon = Icons.Filled.History, title = "No orders yet", description = "Accepted orders will show up here.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                items(state.orders, key = { it.id }) { order ->
                    HistoryOrderRow(order = order, onClick = { onOpenOrder(order.id) })
                }
            }
        }
    }
}

@Composable
private fun HistoryOrderRow(order: DeviceOrder, onClick: () -> Unit) {
    val presentation = order.type.toBadge()
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(order.number, style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12)
                Text(order.customerName, style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(order.total.asCurrency(), style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12)
                StatusBadge(text = presentation.label, tone = presentation.tone)
            }
        }
    }
}
