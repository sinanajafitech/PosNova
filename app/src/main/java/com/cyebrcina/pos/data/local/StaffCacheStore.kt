package com.cyebrcina.pos.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cyebrcina.pos.data.remote.model.DeviceStaffMember
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.staffCacheDataStore by preferencesDataStore(name = "staff_cache")

/**
 * Persists the last-successfully-fetched `GET /api/device/staff` roster across app restarts —
 * same tradeoff/pattern as [MenuCacheStore]. Lets the Select Staff screen (see
 * [com.cyebrcina.pos.feature.staffselect.SelectStaffViewModel]) still show real names to pick
 * from on a cold, offline launch, rather than a dead end that blocks the till from being used at
 * all until a connection comes back. No PIN data is ever part of [DeviceStaffMember], so nothing
 * sensitive ends up cached here.
 */
@Singleton
class StaffCacheStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private object Keys {
        val STAFF_JSON = stringPreferencesKey("staff_json")
    }

    private val cachedJson = context.staffCacheDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.STAFF_JSON] }

    suspend fun load(): List<DeviceStaffMember>? {
        val raw = cachedJson.first() ?: return null
        return runCatching { json.decodeFromString<List<DeviceStaffMember>>(raw) }.getOrNull()
    }

    suspend fun save(staff: List<DeviceStaffMember>) {
        context.staffCacheDataStore.edit { prefs -> prefs[Keys.STAFF_JSON] = json.encodeToString(staff) }
    }
}
