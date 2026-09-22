package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val deviceId: String? = null,
)

@Serializable
data class DeviceInfo(
    val id: String,
    val name: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    val device: DeviceInfo,
    val storeName: String,
    val primaryColor: String,
    val logoUrl: String? = null,
    val customerDisplay: CustomerDisplayConfig? = null,
    val cardTerminal: CardTerminalConfig? = null,
    /** See Admin's Settings -> Feature Management. Absent/missing keys mean "on" — the backend
     * always resolves every known key with its own default before sending, so a key is only ever
     * missing here against an app build that knows about a newer feature than the backend does. */
    val features: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class ErrorResponse(
    val error: String? = null,
)
