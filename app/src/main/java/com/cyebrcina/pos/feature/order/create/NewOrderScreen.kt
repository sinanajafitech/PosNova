package com.cyebrcina.pos.feature.order.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyListItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cyebrcina.pos.core.components.EmptyState
import com.cyebrcina.pos.core.image.imageModel
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.data.remote.model.MenuCategory
import com.cyebrcina.pos.data.remote.model.MenuProduct

/** Admin's category `color` is always a native `<input type="color">` value ("#RRGGBB"), but
 * parsed defensively since a hand-edited/legacy row could in principle hold anything. */
private fun parseCategoryColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(PosColors.Blue500)

@Composable
fun NewOrderScreen(
    onCheckout: () -> Unit,
    onExitFlow: () -> Unit,
    onHeld: () -> Unit,
    viewModel: NewOrderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { PosTopBar(title = "New Order") }) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (!state.isOnline) {
                    OfflineBanner(pendingOrderCount = state.pendingOrderCount, modifier = Modifier.padding(Spacing.sm))
                }
                Box(Modifier.weight(1f)) {
                    when {
                        state.isLoadingMenu -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PosColors.Blue500) }
                        // Only a full-screen blocker when there's truly nothing to show — a
                        // refresh failure with a cached/previously-loaded menu still on hand (see
                        // MenuCacheStore) falls through to browsing it instead, with the offline
                        // banner above as the only sign anything's wrong.
                        state.menuError != null && state.categories.isEmpty() -> EmptyState(
                            icon = Icons.Filled.ShoppingBag,
                            title = "Couldn't load the menu",
                            description = state.menuError.orEmpty(),
                        )
                        // Step 1: pick a category from the grid. Step 2 (below): browse its items.
                        state.selectedCategoryId == null -> CategoryGrid(state, viewModel)
                        else -> Column {
                            val selectedCategory = state.categories.firstOrNull { it.id == state.selectedCategoryId }
                            CategoryHeader(category = selectedCategory, onBack = { viewModel.onCategorySelected(null) })
                            Spacer(Modifier.height(Spacing.xs))
                            ProductGrid(state, viewModel)
                        }
                    }
                }
            }
            CartPanel(
                state = state,
                viewModel = viewModel,
                onCheckout = onCheckout,
                onHold = { viewModel.holdCurrentOrder(); onHeld() },
                modifier = Modifier.width(400.dp).fillMaxHeight().padding(Spacing.sm),
            )
        }
    }

    if (!state.hasStartedOrder) {
        StartOrderDialog(
            initialGuestCount = state.guestCount,
            onDismiss = onExitFlow,
            onConfirm = { type, name, guests, table -> viewModel.startOrder(type, name, guests, table) },
        )
    }

    state.detailProduct?.let { product ->
        ProductDetailDialog(
            product = product,
            addOns = state.addOns,
            modifierGroups = state.modifierGroups,
            onDismiss = viewModel::onDetailDismissed,
            onAddToCart = { size, addOns, qty, notes -> viewModel.addToCart(product, size, addOns, qty, notes) },
        )
    }

    if (state.showChooseTable) {
        ChooseTableDialog(
            selectedTable = state.tableLabel,
            guestCount = state.guestCount,
            customerName = state.customerName,
            onDismiss = viewModel::onChooseTableDismissed,
            onConfirm = viewModel::onTableSelected,
        )
    }
}

/** Step 1 of the New Order flow: pick a category before browsing its items — tapping a tile
 * drills into [ProductGrid] for just that category (see the `when` in [NewOrderScreen]). */
@Composable
private fun CategoryGrid(state: NewOrderUiState, viewModel: NewOrderViewModel) {
    if (state.categories.isEmpty()) {
        EmptyState(icon = Icons.Filled.Restaurant, title = "No categories", description = "Nothing on the menu yet.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 200.dp),
        contentPadding = PaddingValues(Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize(),
    ) {
        lazyGridItems(state.categories, key = { it.id }) { category ->
            CategoryTile(category = category, onClick = { viewModel.onCategorySelected(category.id) })
        }
    }
}

/** Matches [ProductTile]'s card proportions (r=20, white bg, thin border) so the two grid steps
 * feel like one continuous flow — the color swatch takes the image slot. */
@Composable
private fun CategoryTile(category: MenuCategory, onClick: () -> Unit) {
    val color = parseCategoryColor(category.color)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PosColors.White)
            .border(1.dp, PosColors.Neutral4, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (category.imageUrl != null) {
                AsyncImage(
                    model = imageModel(category.imageUrl),
                    contentDescription = category.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                )
            } else {
                Text(category.name.take(1).ifBlank { "•" }, style = PosTextStyles.h2, color = PosColors.White)
            }
        }
        Spacer(Modifier.height(Spacing.xxs))
        Text(category.name, style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral12, maxLines = 1)
        Spacer(Modifier.height(Spacing.xxxs))
        Text("${category.products.size} Items", style = PosTextStyles.bodyXSmallRegular, color = PosColors.TextSecondary)
    }
}

/** Step 2's header: back to the category grid, plus the category's own color/name so it's
 * clear which category is being browsed. */
@Composable
private fun CategoryHeader(category: MenuCategory?, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to categories", tint = PosColors.Neutral12)
        }
        if (category != null) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(parseCategoryColor(category.color)))
            Text(category.name, style = PosTextStyles.h6, color = PosColors.Neutral12)
            Text("${category.products.size} Items", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
        }
    }
}

@Composable
private fun ProductGrid(state: NewOrderUiState, viewModel: NewOrderViewModel) {
    val products = state.visibleCategories.flatMap { it.products }
    if (products.isEmpty()) {
        EmptyState(icon = Icons.Filled.ShoppingBag, title = "No items", description = "Nothing available in this category.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 200.dp),
        contentPadding = PaddingValues(Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize(),
    ) {
        lazyGridItems(products, key = { it.id }) { product ->
            val cartQuantity = state.cart.filter { it.product.id == product.id }.sumOf { it.quantity }
            ProductTile(
                product = product,
                cartQuantity = cartQuantity,
                onTap = { viewModel.onProductTapped(product) },
            )
        }
    }
}

/** Matches Figma's product grid tile: 207x224, r=20, image thumb + name + blue price + qty stepper. */
@Composable
private fun ProductTile(product: MenuProduct, cartQuantity: Int, onTap: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PosColors.White)
            .border(1.dp, PosColors.Neutral4, RoundedCornerShape(20.dp))
            .clickable(onClick = onTap)
            .padding(Spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PosColors.ImagePlaceholder),
        ) {
            if (product.imageUrl != null) {
                AsyncImage(
                    model = imageModel(product.imageUrl),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                )
            }
            if (!product.available) {
                Box(Modifier.fillMaxSize().background(PosColors.White.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Text("86'd", style = PosTextStyles.bodySmallSemibold, color = PosColors.Warning500)
                }
            }
        }
        Spacer(Modifier.height(Spacing.xxs))
        Text(product.name, style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral12, maxLines = 1)
        Spacer(Modifier.height(Spacing.xxxs))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val price = product.sizes.minByOrNull { it.price }?.price ?: product.price
            Text(price.asCurrency(), style = PosTextStyles.bodyMediumSemibold, color = PosColors.Blue500)
            if (cartQuantity > 0) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(percent = 50)).background(PosColors.Blue50).padding(horizontal = Spacing.xxs, vertical = 2.dp),
                ) {
                    Text("×$cartQuantity", style = PosTextStyles.bodyXSmallSemibold, color = PosColors.Blue500)
                }
            }
        }
    }
}

@Composable
private fun CartPanel(
    state: NewOrderUiState,
    viewModel: NewOrderViewModel,
    onCheckout: () -> Unit,
    onHold: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(PosColors.Surface)
            .padding(Spacing.sm),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(state.customerName, style = PosTextStyles.h6, color = PosColors.Neutral12)
                Text("${state.itemCount} item${if (state.itemCount == 1) "" else "s"}", style = PosTextStyles.bodyXSmallRegular, color = PosColors.TextSecondary)
            }
            IconButton(onClick = viewModel::startNewOrder) {
                Icon(Icons.Filled.Close, contentDescription = "Clear order", tint = PosColors.TextSecondary)
            }
        }
        Spacer(Modifier.height(Spacing.sm))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            PillSelector(
                label = if (state.orderType == TillOrderType.DINE_IN) "Dine In" else "Collection",
                onClick = {
                    viewModel.onOrderTypeChanged(if (state.orderType == TillOrderType.DINE_IN) TillOrderType.COLLECTION else TillOrderType.DINE_IN)
                },
                modifier = Modifier.weight(1f),
                trailing = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = PosColors.TextSecondary, modifier = Modifier.size(18.dp)) },
            )
            if (state.orderType == TillOrderType.DINE_IN) {
                PillSelector(
                    label = state.tableLabel ?: "Select table",
                    onClick = viewModel::onChooseTableRequested,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        Column(Modifier.weight(1f)) {
            if (state.cart.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = Spacing.xl), contentAlignment = Alignment.Center) {
                    Text("Cart is empty — tap an item to add it", style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    lazyListItems(state.cart, key = { it.lineId }) { item ->
                        CartLineRow(item, onIncrement = { viewModel.incrementLine(item.lineId) }, onDecrement = { viewModel.decrementLine(item.lineId) })
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PosColors.SurfaceAlt)
                .padding(Spacing.sm),
        ) {
            TotalsRow("Sub Total", state.subtotal.asCurrency())
            Spacer(Modifier.height(Spacing.xxs))
            androidx.compose.material3.HorizontalDivider(color = PosColors.Border)
            Spacer(Modifier.height(Spacing.xxs))
            TotalsRow("TOTAL", state.total.asCurrency(), emphasize = true)
        }

        Spacer(Modifier.height(Spacing.sm))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            FlowSecondaryButton(
                text = "Hold",
                onClick = onHold,
                enabled = state.cart.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
            FlowPrimaryButton(
                text = "Place Order",
                onClick = onCheckout,
                enabled = state.canSubmit,
                modifier = Modifier.weight(2f),
            )
        }
    }
}

@Composable
private fun CartLineRow(item: CartItem, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PosColors.White).padding(Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.product.name, style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12, maxLines = 1)
            if (item.size != null || item.addOns.isNotEmpty()) {
                val extras = listOfNotNull(item.size?.label) + item.addOns.map { it.name }
                Text(extras.joinToString(", "), style = PosTextStyles.bodyXSmallRegular, color = PosColors.TextSecondary, maxLines = 1)
            }
            Text(item.lineTotal.asCurrency(), style = PosTextStyles.bodyXSmallSemibold, color = PosColors.Blue500)
        }
        QuantityStepper(quantity = item.quantity, onIncrement = onIncrement, onDecrement = onDecrement)
    }
}

@Composable
private fun TotalsRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (emphasize) PosTextStyles.bodyMediumSemibold else PosTextStyles.bodySmallMedium, color = PosColors.Neutral12)
        Text(value, style = if (emphasize) PosTextStyles.bodyMediumSemibold else PosTextStyles.bodySmallSemibold, color = PosColors.Neutral12)
    }
}
