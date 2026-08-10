package com.cyebrcina.pos.data.model

/** The logged-in device's session — one shared credential per till, not a per-cashier account. */
data class DeviceSession(
    val token: String,
    val deviceId: String,
    val deviceName: String,
    val storeName: String,
    val primaryColor: String,
    val logoUrl: String?,
)
