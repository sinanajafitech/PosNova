package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ClockRequest(val pin: String)

@Serializable
data class StaffInfo(val id: String, val name: String)

/** Real, live. `action` is "in" or "out" — whichever the PIN toggled to. */
@Serializable
data class ClockResponse(val ok: Boolean = false, val action: String? = null, val staff: StaffInfo? = null)

/** One row from `GET /api/device/staff` — the active roster shown on the till's "Select Staff"
 * grid. No PIN data included; the PIN is entered separately and checked via
 * [VerifyStaffPinRequest]. */
@Serializable
data class DeviceStaffMember(val id: String, val name: String, val role: String? = null)

@Serializable
data class StaffListResponse(val staff: List<DeviceStaffMember> = emptyList())

/** `POST /api/device/staff/verify-pin` — confirms [pin] belongs to [staffId], identify-only
 * (unlike [ClockRequest], never touches shift/clock status). */
@Serializable
data class VerifyStaffPinRequest(val staffId: String, val pin: String)

@Serializable
data class VerifyStaffPinResponse(val ok: Boolean = false, val staff: StaffInfo? = null)
