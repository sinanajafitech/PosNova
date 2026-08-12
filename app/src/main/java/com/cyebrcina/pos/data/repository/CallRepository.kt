package com.cyebrcina.pos.data.repository

import com.cyebrcina.pos.data.remote.model.DeviceCall

interface CallRepository {
    suspend fun getCalls(): Result<List<DeviceCall>>
}
