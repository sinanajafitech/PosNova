package com.cyebrcina.pos.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cyebrcina.pos.data.model.DeviceSession
import com.cyebrcina.pos.data.remote.model.LoginResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "device_session")

/**
 * Persists the device's 180-day session (token + store branding) across app restarts — the API
 * expects a till to "just stay logged in" (see openapi.yaml). Backed by Preferences DataStore.
 */
@Singleton
class DeviceSessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val STORE_NAME = stringPreferencesKey("store_name")
        val PRIMARY_COLOR = stringPreferencesKey("primary_color")
        val LOGO_URL = stringPreferencesKey("logo_url")
    }

    val session = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val token = prefs[Keys.TOKEN] ?: return@map null
            DeviceSession(
                token = token,
                deviceId = prefs[Keys.DEVICE_ID].orEmpty(),
                deviceName = prefs[Keys.DEVICE_NAME].orEmpty(),
                storeName = prefs[Keys.STORE_NAME].orEmpty(),
                primaryColor = prefs[Keys.PRIMARY_COLOR] ?: "#01565B",
                logoUrl = prefs[Keys.LOGO_URL],
            )
        }

    suspend fun save(response: LoginResponse) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = response.token
            prefs[Keys.DEVICE_ID] = response.device.id
            prefs[Keys.DEVICE_NAME] = response.device.name
            prefs[Keys.STORE_NAME] = response.storeName
            prefs[Keys.PRIMARY_COLOR] = response.primaryColor
            response.logoUrl?.let { prefs[Keys.LOGO_URL] = it }
        }
    }

    suspend fun updateBranding(storeName: String, primaryColor: String, logoUrl: String?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STORE_NAME] = storeName
            prefs[Keys.PRIMARY_COLOR] = primaryColor
            if (logoUrl != null) prefs[Keys.LOGO_URL] = logoUrl
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    /**
     * Synchronous read for the OkHttp auth interceptor, which isn't suspend. Safe to block here:
     * interceptors already run on OkHttp's background dispatcher, never the main thread.
     */
    fun currentTokenBlocking(): String? = runBlocking { session.first()?.token }
}
