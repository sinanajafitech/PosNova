package com.cyebrcina.pos.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cyebrcina.pos.data.remote.model.MenuResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.menuCacheDataStore by preferencesDataStore(name = "menu_cache")

/**
 * Persists the last-successfully-fetched `GET /api/device/menu` response across app restarts —
 * a cold, offline launch would otherwise start from an empty menu with nothing to browse (see
 * FireHutMenuRepositoryImpl, which previously only held this in an in-memory StateFlow, lost on
 * process death). One JSON blob in a single preference key, same tradeoff already accepted for
 * this app's other DataStore usages (small, infrequently-written, no per-row query needs).
 */
@Singleton
class MenuCacheStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private object Keys {
        val MENU_JSON = stringPreferencesKey("menu_json")
    }

    private val cachedJson = context.menuCacheDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.MENU_JSON] }

    /** Null if nothing has ever been cached, or the cached value doesn't parse (e.g. a shape
     * change across app versions) — either way, the caller just falls back to an empty menu
     * until the next successful network refresh, same as before this cache existed. */
    suspend fun load(): MenuResponse? {
        val raw = cachedJson.first() ?: return null
        return runCatching { json.decodeFromString<MenuResponse>(raw) }.getOrNull()
    }

    suspend fun save(response: MenuResponse) {
        val encoded = json.encodeToString(response)
        context.menuCacheDataStore.edit { prefs -> prefs[Keys.MENU_JSON] = encoded }
    }
}
