package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ClockRequest(val pin: String)

@Serializable
data class StaffInfo(val id: String, val name: String)

/** Real, live. `action` is "in" or "out" — whichever the PIN toggled to. */
@Serializable
data class ClockResponse(val ok: Boolean = false, val action: String? = null, val staff: StaffInfo? = null)
