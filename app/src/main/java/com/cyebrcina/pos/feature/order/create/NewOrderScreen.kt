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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyListItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
                CategoryRow(state, viewModel)
                Spacer(Modifier.height(Spacing.xs))
                when {
                    state.isLoadingMenu -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PosColors.Blue500) }
                    state.menuError != null -> EmptyState(
                        icon = Icons.Filled.ShoppingBag,
                        title = "Couldn't load the menu",
                        description = state.menuError.orEmpty(),
                    )
                    else -> ProductGrid(state, viewModel)
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

@Composable
private fun CategoryRow(state: NewOrderUiState, viewModel: NewOrderViewModel) {
    LazyRow(
        contentPadding = PaddingValues(Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        item {
            CategoryChip(
                icon = "🍽️",
                name = "All Menu",
                count = state.categories.sumOf { it.products.size },
                selected = state.selectedCategoryId == null,
                onClick = { viewModel.onCategorySelected(null) },
            )
        }
        lazyListItems(state.categories, key = { it.id }) { category ->
            CategoryChip(
                icon = category.name.take(1).ifBlank { "•" },
                name = category.name,
                count = category.products.size,
                selected = state.selectedCategoryId == category.id,
                onClick = { viewModel.onCategorySelected(category.id) },
            )
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
