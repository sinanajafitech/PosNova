package com.cyebrcina.pos.data.repository.firehut

import com.cyebrcina.pos.data.remote.FireHutDeviceApi
import com.cyebrcina.pos.data.remote.errorMessageOrDefault
import com.cyebrcina.pos.data.remote.model.SavePhoneContactRequest
import com.cyebrcina.pos.data.repository.PhoneContactRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class FireHutPhoneContactRepositoryImpl @Inject constructor(
    private val api: FireHutDeviceApi,
    private val json: Json,
) : PhoneContactRepository {

    override suspend fun saveContact(phone: String, name: String, address: String, notes: String): Result<Unit> = runCatching {
        val response = api.savePhoneContact(SavePhoneContactRequest(phone, name, address, notes))
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
    }.recoverCatching { cause ->
        throw if (cause is IOException) IOException("Couldn't reach the server — check your connection", cause) else cause
    }
}
