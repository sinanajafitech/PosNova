package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

/** Real, live — `POST /api/device/orders` on the real backend. */
@Serializable
data class CreateOrderItemRequest(
    val productId: String,
    val sizeId: String? = null,
    val quantity: Int,
    val addOnIds: List<String> = emptyList(),
    val notes: String? = null,
)

@Serializable
data class CreateOrderPayment(
    val method: String,
    val amount: Double,
    val cashTendered: Double? = null,
    val provider: String? = null,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    val terminalReference: String? = null,
)

@Serializable
data class CreateOrderRequest(
    val type: DeviceOrderType,
    val tableLabel: String? = null,
    val customerName: String,
    val customerPhone: String? = null,
    // Set when this order was started from the Incoming Call popup's "Take
    // Order" button — links the order back to that call on Admin's Calls
    // page. Validated server-side; an invalid id is silently dropped.
    val phoneCallLogId: String? = null,
    val notes: String? = null,
    val items: List<CreateOrderItemRequest>,
    val payment: CreateOrderPayment,
)

@Serializable
data class CreateOrderResponse(
    val order: DeviceOrder,
)
