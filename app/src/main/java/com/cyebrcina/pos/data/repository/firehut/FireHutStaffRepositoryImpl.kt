package com.cyebrcina.pos.data.repository.firehut

import com.cyebrcina.pos.data.remote.FireHutDeviceApi
import com.cyebrcina.pos.data.remote.errorMessageOrDefault
import com.cyebrcina.pos.data.remote.model.ClockRequest
import com.cyebrcina.pos.data.remote.model.ClockResponse
import com.cyebrcina.pos.data.repository.StaffRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class FireHutStaffRepositoryImpl @Inject constructor(
    private val api: FireHutDeviceApi,
    private val json: Json,
) : StaffRepository {

    override suspend fun clock(pin: String): Result<ClockResponse> = runCatching {
        val response = api.clockStaff(ClockRequest(pin))
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body() ?: throw IllegalStateException("Empty response")
    }.recoverCatching { cause ->
        throw if (cause is IOException) IOException("Couldn't reach the server — check your connection", cause) else cause
    }
}
