package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
enum class ItemType { VEG, NON_VEG, EGG, DRINK, HALAL, OTHER }

@Serializable
data class MenuProductSize(
    val id: String,
    val label: String,
    val price: Double,
)

@Serializable
data class MenuProduct(
    val id: String,
    val name: String,
    val description: String? = null,
    /** Base price — the price to use when [sizes] is empty. */
    val price: Double,
    val imageUrl: String? = null,
    val itemType: ItemType? = null,
    val isFeatured: Boolean = false,
    /** false when the product's status is EIGHTY_SIXED (temporarily out). */
    val available: Boolean = true,
    val allergens: List<String> = emptyList(),
    val sizes: List<MenuProductSize> = emptyList(),
    /** Which [MenuModifierGroup]s (from [MenuResponse.modifierGroups]) apply
     * to this product — e.g. a required "Choose your base" group. Empty for
     * the vast majority of products, which just use the flat, global,
     * always-optional [MenuResponse.addOns] list as before. */
    val modifierGroupIds: List<String> = emptyList(),
)

@Serializable
data class MenuCategory(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    /** Hex string (e.g. "#2D71F7"), admin-set — see Category.color in Admin's schema. Shown
     * behind this category's tile on the till's category-select grid. */
    val color: String = "#2D71F7",
    val products: List<MenuProduct> = emptyList(),
)

@Serializable
data class MenuAddOn(
    val id: String,
    val name: String,
    val price: Double,
    /** Null = global, always-optional add-on shown on every product (the
     * pre-existing behavior). Set only when an admin has organized it into a
     * named, product-scoped group — see [MenuModifierGroup]. */
    val modifierGroupId: String? = null,
)

/** A required/limited-choice group of [MenuAddOn]s (matched by
 * [MenuAddOn.modifierGroupId]), attached to specific products via
 * [MenuProduct.modifierGroupIds] — e.g. "Choose your base" (minSelect: 1,
 * maxSelect: 1). "Required" is just minSelect >= 1, no separate flag. */
@Serializable
data class MenuModifierGroup(
    val id: String,
    val name: String,
    val minSelect: Int = 0,
    val maxSelect: Int? = null,
)

@Serializable
data class MenuResponse(
    val categories: List<MenuCategory> = emptyList(),
    val addOns: List<MenuAddOn> = emptyList(),
    val modifierGroups: List<MenuModifierGroup> = emptyList(),
)

/** Real, live. Response from POST api/device/menu/products/{id}/toggle-sold-out. */
@Serializable
data class ToggleSoldOutResponse(
    val id: String,
    val name: String,
    val available: Boolean,
)
