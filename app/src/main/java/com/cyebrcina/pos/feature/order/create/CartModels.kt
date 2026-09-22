package com.cyebrcina.pos.feature.order.create

import com.cyebrcina.pos.data.remote.model.MenuAddOn
import com.cyebrcina.pos.data.remote.model.MenuProduct
import com.cyebrcina.pos.data.remote.model.MenuProductSize
import java.util.UUID
import kotlin.math.roundToLong

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

/** Summing several Double line totals can leave a sub-penny drift (e.g. many `.10`/`.20` items) —
 * round to whole pennies before comparing two money amounts so a cash total that matches on
 * paper doesn't fail an exact `>=` check by a fraction of a penny. */
fun Double.roundToCents(): Double = (this * 100).roundToLong() / 100.0
