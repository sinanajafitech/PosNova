package com.cyebrcina.pos.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cyebrcina.pos.payment.model.PaymentProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.terminalDataStore by preferencesDataStore(name = "terminal_settings")

/** Persists which [PaymentProvider] is active, so [com.cyebrcina.pos.payment.SelectedPaymentTerminalService]
 * knows which real implementation to delegate to. Defaults to [PaymentProvider.MOCK] — the same
 * safe default [com.cyebrcina.pos.di.PaymentModule] used before this was selectable. */
@Singleton
class TerminalSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val PROVIDER = stringPreferencesKey("provider")
    }

    val selectedProvider: Flow<PaymentProvider> = context.terminalDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> PaymentProvider.entries.find { it.name == prefs[Keys.PROVIDER] } ?: PaymentProvider.MOCK }

    suspend fun setSelectedProvider(provider: PaymentProvider) {
        context.terminalDataStore.edit { it[Keys.PROVIDER] = provider.name }
    }
}
