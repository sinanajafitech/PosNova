package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class StoreStatusResponse(
    val acceptingOrders: Boolean,
)

@Serializable
data class SetStoreStatusRequest(
    val acceptingOrders: Boolean,
)

@Serializable
data class SetStoreStatusResponse(
    val ok: Boolean = false,
    val acceptingOrders: Boolean,
)
