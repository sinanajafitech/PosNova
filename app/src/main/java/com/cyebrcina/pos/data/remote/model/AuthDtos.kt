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
)

@Serializable
data class ErrorResponse(
    val error: String? = null,
)
