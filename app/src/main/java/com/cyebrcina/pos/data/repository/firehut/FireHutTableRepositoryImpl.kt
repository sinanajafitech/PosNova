package com.cyebrcina.pos.data.repository.firehut

import com.cyebrcina.pos.data.remote.FireHutDeviceApi
import com.cyebrcina.pos.data.remote.errorMessageOrDefault
import com.cyebrcina.pos.data.remote.model.RestaurantTableDto
import com.cyebrcina.pos.data.repository.TableRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class FireHutTableRepositoryImpl @Inject constructor(
    private val api: FireHutDeviceApi,
    private val json: Json,
) : TableRepository {

    override suspend fun getTables(): Result<List<RestaurantTableDto>> = runCatching {
        val response = api.tables()
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body()?.tables ?: throw IllegalStateException("Empty response")
    }.recoverCatching { cause ->
        throw if (cause is IOException) IOException("Couldn't reach the server — check your connection", cause) else cause
    }
}
