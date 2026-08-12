package com.cyebrcina.pos.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.currentStaffDataStore by preferencesDataStore(name = "current_staff")

data class CurrentStaff(val id: String, val name: String)

/**
 * Which staff member is currently PIN-clocked-in on *this* till — a local
 * convenience label plus the id attached to any order this till submits
 * (Order.staffId), not the source of truth for shift history (that's the
 * server-side StaffClockEvent, same as Admin's clock-in kiosk). Persisted
 * so the till still shows who's on shift across a restart; can drift from
 * reality if that staff member clocks out from Admin's own kiosk instead
 * of this till — the next clock action here reconciles it.
 */
@Singleton
class CurrentStaffStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val STAFF_ID = stringPreferencesKey("current_staff_id")
        val STAFF_NAME = stringPreferencesKey("current_staff_name")
    }

    private val prefsFlow = context.currentStaffDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val currentStaff: Flow<CurrentStaff?> = prefsFlow.map { prefs ->
        val id = prefs[Keys.STAFF_ID] ?: return@map null
        val name = prefs[Keys.STAFF_NAME] ?: return@map null
        CurrentStaff(id, name)
    }

    suspend fun setCurrentStaff(staff: CurrentStaff?) {
        context.currentStaffDataStore.edit { prefs ->
            if (staff == null) {
                prefs.remove(Keys.STAFF_ID)
                prefs.remove(Keys.STAFF_NAME)
            } else {
                prefs[Keys.STAFF_ID] = staff.id
                prefs[Keys.STAFF_NAME] = staff.name
            }
        }
    }
}
