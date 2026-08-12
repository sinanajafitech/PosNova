package com.cyebrcina.pos.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppCard
import com.cyebrcina.pos.core.components.AppTextField
import com.cyebrcina.pos.core.components.BadgeTone
import com.cyebrcina.pos.core.components.EmptyState
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.components.StatusBadge
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.data.remote.model.MenuCategory
import com.cyebrcina.pos.data.remote.model.MenuProduct

/**
 * The till's "86 Board" — tap the switch on any item to mark it sold out or bring it back,
 * live-synced with Admin's own Menu page and every other till (see ToolsViewModel).
 */
@Composable
fun ToolsScreen(viewModel: ToolsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            PosTopBar(
                title = "Tools",
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = PosColors.Neutral9)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "86 Board",
                style = PosTextStyles.h6,
                color = PosColors.Neutral12,
                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            )
            AppTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = "",
                placeholder = "Search menu…",
                modifier = Modifier.padding(horizontal = Spacing.sm),
            )

            if (state.errorMessage != null) {
                Text(
                    state.errorMessage.orEmpty(),
                    style = PosTextStyles.bodySmallRegular,
                    color = PosColors.Warning500,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                )
            }

            if (!state.isLoading && state.visibleCategories.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Handyman,
                    title = if (state.searchQuery.isBlank()) "No menu items" else "No matches",
                    description = if (state.searchQuery.isBlank()) "Pull to refresh once items are added in Admin." else "Try a different search term.",
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    state.visibleCategories.forEach { category ->
                        item(key = "header-${category.id}") { CategoryHeader(category) }
                        items(category.products, key = { it.id }) { product ->
                            SoldOutRow(
                                product = product,
                                isToggling = product.id in state.togglingProductIds,
                                onToggle = { viewModel.toggleSoldOut(product.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: MenuCategory) {
    Text(category.name, style = PosTextStyles.h6, color = PosColors.Neutral12)
}

@Composable
private fun SoldOutRow(product: MenuProduct, isToggling: Boolean, onToggle: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Text(
                        product.name,
                        style = PosTextStyles.bodySmallSemibold,
                        color = PosColors.Neutral12,
                        textDecoration = if (!product.available) TextDecoration.LineThrough else null,
                    )
                    if (!product.available) StatusBadge(text = "86'd", tone = BadgeTone.WARNING)
                }
                Spacer(Modifier.height(Spacing.xxxs))
                Text(
                    if (product.available) "Available" else "Sold out",
                    style = PosTextStyles.bodyXSmallRegular,
                    color = PosColors.Neutral7,
                )
            }
            Switch(
                checked = product.available,
                onCheckedChange = { onToggle() },
                enabled = !isToggling,
                colors = SwitchDefaults.colors(checkedTrackColor = PosColors.Blue500),
            )
        }
    }
}
