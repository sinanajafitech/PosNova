package com.cyebrcina.pos.data.repository.firehut

import android.provider.Settings
import android.content.Context
import com.cyebrcina.pos.data.local.DeviceSessionStore
import com.cyebrcina.pos.data.model.DeviceSession
import com.cyebrcina.pos.data.remote.FireHutDeviceApi
import com.cyebrcina.pos.data.remote.errorMessageOrDefault
import com.cyebrcina.pos.data.remote.model.LoginRequest
import com.cyebrcina.pos.data.remote.realtime.FireHutRealtimeManager
import com.cyebrcina.pos.data.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

@Singleton
class FireHutAuthRepositoryImpl @Inject constructor(
    private val api: FireHutDeviceApi,
    private val sessionStore: DeviceSessionStore,
    private val realtimeManager: FireHutRealtimeManager,
    private val json: Json,
    @ApplicationContext private val context: Context,
) : AuthRepository {

    override val session: Flow<DeviceSession?> = sessionStore.session

    override suspend fun login(username: String, password: String): Result<Unit> {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()

        return try {
            val response = api.login(LoginRequest(username = username, password = password, deviceId = androidId))
            if (!response.isSuccessful) {
                return Result.failure(IllegalArgumentException(response.errorMessageOrDefault(json)))
            }
            val body = response.body() ?: return Result.failure(IllegalStateException("Empty login response"))
            sessionStore.save(body)
            realtimeManager.connect(body.token)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(IOException("Couldn't reach the server — check your connection", e))
        }
    }

    override suspend fun logout() {
        realtimeManager.disconnect()
        sessionStore.clear()
    }
}
