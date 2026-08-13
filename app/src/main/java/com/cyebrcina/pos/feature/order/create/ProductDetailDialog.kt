package com.cyebrcina.pos.feature.order.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.cyebrcina.pos.core.components.AppTextField
import com.cyebrcina.pos.core.image.imageModel
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.data.remote.model.MenuAddOn
import com.cyebrcina.pos.data.remote.model.MenuModifierGroup
import com.cyebrcina.pos.data.remote.model.MenuProduct
import com.cyebrcina.pos.data.remote.model.MenuProductSize

private data class ResolvedModifierGroup(
    val group: MenuModifierGroup,
    val addOns: List<MenuAddOn>,
)

/** Matches Figma's "Popup Detail Menu": image, category tag, name/description, price + qty stepper, Add to cart CTA. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailDialog(
    product: MenuProduct,
    addOns: List<MenuAddOn>,
    modifierGroups: List<MenuModifierGroup>,
    onDismiss: () -> Unit,
    onAddToCart: (MenuProductSize?, List<MenuAddOn>, Int, String?) -> Unit,
) {
    var selectedSize by remember(product.id) { mutableStateOf(product.sizes.firstOrNull()) }
    var selectedAddOns by remember(product.id) { mutableStateOf<Set<MenuAddOn>>(emptySet()) }
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    var notes by remember(product.id) { mutableStateOf("") }

    val groupsForProduct = remember(product.id, addOns, modifierGroups) {
        modifierGroups
            .filter { it.id in product.modifierGroupIds }
            .map { group -> ResolvedModifierGroup(group, addOns.filter { it.modifierGroupId == group.id }) }
            .filter { it.addOns.isNotEmpty() }
    }
    val ungroupedAddOns = remember(addOns) { addOns.filter { it.modifierGroupId == null } }
    val incompleteGroup = groupsForProduct.find { resolved ->
        selectedAddOns.count { it in resolved.addOns } < resolved.group.minSelect
    }

    val unitPrice = (selectedSize?.price ?: product.price) + selectedAddOns.sumOf { it.price }
    val total = unitPrice * quantity

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = PosColors.White) {
            Column(Modifier.width(500.dp).heightIn(max = 760.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Detail Menu", style = PosTextStyles.h3, color = PosColors.Neutral13)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = PosColors.Neutral13)
                    }
                }

                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PosColors.ImagePlaceholder),
                    ) {
                        if (product.imageUrl != null) {
                            AsyncImage(
                                model = imageModel(product.imageUrl),
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)),
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))

                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(percent = 50)).background(PosColors.Blue100).padding(horizontal = Spacing.sm, vertical = Spacing.xxxs),
                    ) {
                        Text(product.itemType?.name ?: "Menu", style = PosTextStyles.bodyXSmallSemibold, color = PosColors.Blue500)
                    }
                    Spacer(Modifier.height(Spacing.xs))

                    Text(product.name, style = PosTextStyles.h4, color = PosColors.Neutral13)
                    product.description?.let {
                        Spacer(Modifier.height(Spacing.xxs))
                        Text(it, style = PosTextStyles.bodyMediumRegular, color = PosColors.TextSecondary)
                    }

                    if (product.sizes.isNotEmpty()) {
                        Spacer(Modifier.height(Spacing.sm))
                        Text("Size", style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral10)
                        Spacer(Modifier.height(Spacing.xxs))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                            product.sizes.forEach { size ->
                                SelectableChip(
                                    label = "${size.label} · ${size.price.asCurrency()}",
                                    selected = selectedSize?.id == size.id,
                                    onClick = { selectedSize = size },
                                )
                            }
                        }
                    }

                    groupsForProduct.forEach { resolved ->
                        val selectedCount = selectedAddOns.count { it in resolved.addOns }
                        val isRequired = resolved.group.minSelect > 0
                        val isIncomplete = selectedCount < resolved.group.minSelect
                        Spacer(Modifier.height(Spacing.sm))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxxs)) {
                            Text(resolved.group.name, style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral10)
                            if (isRequired) {
                                Text(
                                    if (isIncomplete) "· required" else "· selected",
                                    style = PosTextStyles.bodyXSmallSemibold,
                                    color = if (isIncomplete) PosColors.Danger else PosColors.Success500,
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.xxs))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                            resolved.addOns.forEach { addOn ->
                                val isSelected = addOn in selectedAddOns
                                SelectableChip(
                                    label = "${addOn.name} · +${addOn.price.asCurrency()}",
                                    selected = isSelected,
                                    onClick = {
                                        selectedAddOns = when {
                                            isSelected -> selectedAddOns - addOn
                                            resolved.group.maxSelect == 1 -> selectedAddOns - resolved.addOns.toSet() + addOn
                                            resolved.group.maxSelect != null && selectedCount >= resolved.group.maxSelect -> selectedAddOns
                                            else -> selectedAddOns + addOn
                                        }
                                    },
                                )
                            }
                        }
                    }

                    if (ungroupedAddOns.isNotEmpty()) {
                        Spacer(Modifier.height(Spacing.sm))
                        Text("Add-ons", style = PosTextStyles.bodySmallSemibold, color = PosColors.Neutral10)
                        Spacer(Modifier.height(Spacing.xxs))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                            ungroupedAddOns.forEach { addOn ->
                                val isSelected = addOn in selectedAddOns
                                SelectableChip(
                                    label = "${addOn.name} · +${addOn.price.asCurrency()}",
                                    selected = isSelected,
                                    onClick = {
                                        selectedAddOns = if (isSelected) selectedAddOns - addOn else selectedAddOns + addOn
                                    },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(Spacing.sm))
                    AppTextField(value = notes, onValueChange = { notes = it }, label = "Notes", placeholder = "e.g. no onions")
                    Spacer(Modifier.height(Spacing.sm))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(unitPrice.asCurrency(), style = PosTextStyles.h4, color = PosColors.Blue500)
                        QuantityStepper(quantity = quantity, onIncrement = { quantity++ }, onDecrement = { if (quantity > 1) quantity-- })
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }

                incompleteGroup?.let {
                    Text(
                        "Choose an option for \"${it.group.name}\" before adding to cart.",
                        style = PosTextStyles.bodyXSmallSemibold,
                        color = PosColors.Danger,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xxxs),
                    )
                }
                Box(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                    FlowPrimaryButton(
                        text = "Add to cart (${total.asCurrency()})",
                        onClick = { onAddToCart(selectedSize, selectedAddOns.toList(), quantity, notes.ifBlank { null }) },
                        enabled = incompleteGroup == null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) PosColors.Blue500 else PosColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxxs)) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = PosColors.White, modifier = Modifier.height(14.dp))
            Text(label, style = PosTextStyles.bodyXSmallSemibold, color = if (selected) PosColors.White else PosColors.Neutral12)
        }
    }
}
