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
)

@Serializable
data class MenuCategory(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val products: List<MenuProduct> = emptyList(),
)

@Serializable
data class MenuAddOn(
    val id: String,
    val name: String,
    val price: Double,
)

@Serializable
data class MenuResponse(
    val categories: List<MenuCategory> = emptyList(),
    val addOns: List<MenuAddOn> = emptyList(),
)
