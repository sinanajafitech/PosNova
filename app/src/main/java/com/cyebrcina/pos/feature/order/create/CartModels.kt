package com.cyebrcina.pos.feature.order.create

import com.cyebrcina.pos.data.remote.model.MenuAddOn
import com.cyebrcina.pos.data.remote.model.MenuProduct
import com.cyebrcina.pos.data.remote.model.MenuProductSize
import java.util.UUID

/** Which of the API's [com.cyebrcina.pos.data.remote.model.DeviceOrderType] a till order can use. */
enum class TillOrderType { DINE_IN, COLLECTION }

data class CartItem(
    val lineId: String = UUID.randomUUID().toString(),
    val product: MenuProduct,
    val size: MenuProductSize? = null,
    val addOns: List<MenuAddOn> = emptyList(),
    val quantity: Int = 1,
    val notes: String? = null,
) {
    val unitPrice: Double get() = (size?.price ?: product.price) + addOns.sumOf { it.price }
    val lineTotal: Double get() = unitPrice * quantity
}

fun List<CartItem>.subtotal(): Double = sumOf { it.lineTotal }
