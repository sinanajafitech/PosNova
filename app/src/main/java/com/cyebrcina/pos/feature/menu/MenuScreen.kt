package com.cyebrcina.pos.feature.menu

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.data.remote.model.ItemType
import com.cyebrcina.pos.data.remote.model.MenuAddOn
import com.cyebrcina.pos.data.remote.model.MenuCategory
import com.cyebrcina.pos.data.remote.model.MenuProduct

@Composable
fun MenuScreen(viewModel: MenuViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            PosTopBar(
                title = "Menu",
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = PosColors.Neutral9)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AppTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = "",
                placeholder = "Search menu…",
                modifier = Modifier.padding(Spacing.sm),
            )

            if (state.errorMessage != null) {
                Text(state.errorMessage.orEmpty(), style = PosTextStyles.bodySmallRegular, color = PosColors.Warning500, modifier = Modifier.padding(horizontal = Spacing.sm))
            }

            if (!state.isLoading && state.visibleCategories.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.RestaurantMenu,
                    title = if (state.searchQuery.isBlank()) "No menu items" else "No matches",
                    description = if (state.searchQuery.isBlank()) "Pull to refresh once items are added in Admin." else "Try a different search term.",
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    state.visibleCategories.forEach { category ->
                        item(key = "header-${category.id}") { CategoryHeader(category) }
                        items(category.products, key = { it.id }) { product -> ProductRow(product) }
                    }
                    if (state.addOns.isNotEmpty()) {
                        item(key = "addons-header") {
                            Text("Add-ons", style = PosTextStyles.h6, color = PosColors.Neutral12, modifier = Modifier.padding(top = Spacing.xs))
                        }
                        items(state.addOns, key = { "addon-${it.id}" }) { addOn -> AddOnRow(addOn) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: MenuCategory) {
    Column {
        Text(category.name, style = PosTextStyles.h6, color = PosColors.Neutral12)
        category.description?.let { Text(it, style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7) }
    }
}

@Composable
private fun ProductRow(product: MenuProduct) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Text(
                        product.name,
                        style = PosTextStyles.bodySmallSemibold,
                        color = PosColors.Neutral12,
                        textDecoration = if (!product.available) TextDecoration.LineThrough else null,
                    )
                    product.itemType?.let { ItemTypeBadge(it) }
                    if (!product.available) StatusBadge(text = "86'd", tone = BadgeTone.WARNING)
                    if (product.isFeatured) StatusBadge(text = "Featured", tone = BadgeTone.PENDING)
                }
                product.description?.let {
                    Spacer(Modifier.height(Spacing.xxxs))
                    Text(it, style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7)
                }
                if (product.allergens.isNotEmpty()) {
                    Text("Allergens: ${product.allergens.joinToString(", ")}", style = PosTextStyles.bodyXSmallRegular, color = PosColors.Warning500)
                }
                if (product.sizes.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.xxxs))
                    product.sizes.forEach { size ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(size.label, style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral8)
                            Text(size.price.asCurrency(), style = PosTextStyles.bodyXSmallMedium, color = PosColors.Neutral8)
                        }
                    }
                }
            }
            if (product.sizes.isEmpty()) {
                Text(product.price.asCurrency(), style = PosTextStyles.bodySmallSemibold, color = PosColors.Blue500)
            }
        }
    }
}

@Composable
private fun ItemTypeBadge(itemType: ItemType) {
    val (label, tone) = when (itemType) {
        ItemType.VEG -> "Veg" to BadgeTone.SUCCESS
        ItemType.NON_VEG -> "Non-Veg" to BadgeTone.WARNING
        ItemType.EGG -> "Egg" to BadgeTone.PENDING
        ItemType.DRINK -> "Drink" to BadgeTone.INFO
        ItemType.HALAL -> "Halal" to BadgeTone.SUCCESS
        ItemType.OTHER -> "Other" to BadgeTone.NEUTRAL
    }
    StatusBadge(text = label, tone = tone)
}

@Composable
private fun AddOnRow(addOn: MenuAddOn) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(addOn.name, style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral12)
            Text(addOn.price.asCurrency(), style = PosTextStyles.bodySmallSemibold, color = PosColors.Blue500)
        }
    }
}
