package com.cyebrcina.pos.data.repository

import com.cyebrcina.pos.data.remote.model.ClockResponse

interface StaffRepository {
    suspend fun clock(pin: String): Result<ClockResponse>
}
