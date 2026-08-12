package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceCallOrder(val id: String, val number: String)

/** Real, live. One row from `GET /api/device/calls` — same PhoneCallLog rows Admin's own
 * Calls page lists. [orders] is almost always 0 or 1 items — a call turns into at most one
 * order via the Incoming Call popup's "Take Order". */
@Serializable
data class DeviceCall(
    val id: String,
    val phone: String,
    val callerName: String? = null,
    val address: String? = null,
    val isKnown: Boolean = false,
    val receivedAt: String,
    val orders: List<DeviceCallOrder> = emptyList(),
)

@Serializable
data class CallsResponse(val calls: List<DeviceCall> = emptyList())
