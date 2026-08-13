package com.cyebrcina.pos.data.repository

import com.cyebrcina.pos.data.remote.model.ClockResponse
import com.cyebrcina.pos.data.remote.model.DeviceStaffMember
import com.cyebrcina.pos.data.remote.model.VerifyStaffPinResponse

interface StaffRepository {
    suspend fun clock(pin: String): Result<ClockResponse>

    /** The active staff roster for the "Select Staff" screen's grid. */
    suspend fun list(): Result<List<DeviceStaffMember>>

    /** Identify-only PIN check against a specific staff member — see
     * FireHutDeviceApi.verifyStaffPin. */
    suspend fun verifyPin(staffId: String, pin: String): Result<VerifyStaffPinResponse>
}
