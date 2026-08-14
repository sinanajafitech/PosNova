package com.cyebrcina.pos.data.repository.firehut

import com.cyebrcina.pos.data.local.StaffCacheStore
import com.cyebrcina.pos.data.remote.FireHutDeviceApi
import com.cyebrcina.pos.data.remote.errorMessageOrDefault
import com.cyebrcina.pos.data.remote.model.ClockRequest
import com.cyebrcina.pos.data.remote.model.ClockResponse
import com.cyebrcina.pos.data.remote.model.DeviceStaffMember
import com.cyebrcina.pos.data.remote.model.VerifyStaffPinRequest
import com.cyebrcina.pos.data.remote.model.VerifyStaffPinResponse
import com.cyebrcina.pos.data.repository.StaffRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class FireHutStaffRepositoryImpl @Inject constructor(
    private val api: FireHutDeviceApi,
    private val json: Json,
    private val staffCacheStore: StaffCacheStore,
) : StaffRepository {

    override suspend fun clock(pin: String): Result<ClockResponse> = runCatching {
        val response = api.clockStaff(ClockRequest(pin))
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body() ?: throw IllegalStateException("Empty response")
    }.recoverCatching { cause -> throw mapNetworkError(cause) }

    override suspend fun list(): Result<List<DeviceStaffMember>> = runCatching {
        try {
            val response = api.staffList()
            if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
            val staff = response.body()?.staff ?: emptyList()
            staffCacheStore.save(staff)
            staff
        } catch (e: IOException) {
            // A cold, offline launch would otherwise leave the Select Staff screen with no
            // names to show at all — see SelectStaffViewModel and StaffCacheStore's doc comment.
            staffCacheStore.load() ?: throw e
        }
    }.recoverCatching { cause -> throw mapNetworkError(cause) }

    override suspend fun verifyPin(staffId: String, pin: String): Result<VerifyStaffPinResponse> = runCatching {
        val response = api.verifyStaffPin(VerifyStaffPinRequest(staffId, pin))
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body() ?: throw IllegalStateException("Empty response")
    }.recoverCatching { cause -> throw mapNetworkError(cause) }

    private fun mapNetworkError(cause: Throwable): Throwable =
        if (cause is IOException) IOException("Couldn't reach the server — check your connection", cause) else cause
}
