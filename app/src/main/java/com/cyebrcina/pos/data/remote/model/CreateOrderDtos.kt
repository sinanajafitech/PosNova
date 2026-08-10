package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

/**
 * NOT YET IMPLEMENTED SERVER-SIDE — see BACKEND_ORDER_CREATE_SPEC.md at repo root. Exists so the
 * Android client is ready the moment `POST /api/device/orders` is added to the real backend;
 * until then, calling it will genuinely 404.
 */
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
    val notes: String? = null,
    val items: List<CreateOrderItemRequest>,
    val payment: CreateOrderPayment,
)

@Serializable
data class CreateOrderResponse(
    val order: DeviceOrder,
)
