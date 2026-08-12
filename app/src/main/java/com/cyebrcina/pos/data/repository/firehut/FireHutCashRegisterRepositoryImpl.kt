package com.cyebrcina.pos.data.repository.firehut

import com.cyebrcina.pos.data.remote.FireHutDeviceApi
import com.cyebrcina.pos.data.remote.errorMessageOrDefault
import com.cyebrcina.pos.data.remote.model.CashRegisterSessionDto
import com.cyebrcina.pos.data.remote.model.CloseRegisterRequest
import com.cyebrcina.pos.data.remote.model.OpenRegisterRequest
import com.cyebrcina.pos.data.repository.CashRegisterRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class FireHutCashRegisterRepositoryImpl @Inject constructor(
    private val api: FireHutDeviceApi,
    private val json: Json,
) : CashRegisterRepository {

    override suspend fun current(): Result<CashRegisterSessionDto?> = runCatching {
        val response = api.currentRegisterSession()
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body()?.session
    }.recoverCatching { cause ->
        throw if (cause is IOException) IOException("Couldn't reach the server — check your connection", cause) else cause
    }

    override suspend fun open(openingFloat: Double, staffId: String?): Result<CashRegisterSessionDto?> = runCatching {
        val response = api.openRegister(OpenRegisterRequest(openingFloat, staffId))
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body()?.session
    }.recoverCatching { cause ->
        throw if (cause is IOException) IOException("Couldn't reach the server — check your connection", cause) else cause
    }

    override suspend fun close(countedCash: Double, staffId: String?): Result<CashRegisterSessionDto?> = runCatching {
        val response = api.closeRegister(CloseRegisterRequest(countedCash, staffId))
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body()?.session
    }.recoverCatching { cause ->
        throw if (cause is IOException) IOException("Couldn't reach the server — check your connection", cause) else cause
    }
}
