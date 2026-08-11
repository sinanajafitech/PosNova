package com.cyebrcina.pos.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cyebrcina.pos.printer.model.DiscoveredPrinter
import com.cyebrcina.pos.printer.model.PrinterConnection
import com.cyebrcina.pos.printer.network.DEFAULT_NETWORK_PRINTER_PORT
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.printerDataStore by preferencesDataStore(name = "printer_settings")

data class KitchenPrinterSettings(
    val enabled: Boolean = false,
    val name: String = "Kitchen",
    val host: String = "",
    val port: Int = DEFAULT_NETWORK_PRINTER_PORT,
)

/**
 * Persists the selected main printer (receipt/customer printer — whichever of
 * Bluetooth/USB/built-in/network the cashier picked in Settings) and a separate, optional
 * network-only kitchen printer, across app restarts. Previously nothing here was saved at all —
 * [com.cyebrcina.pos.printer.PrinterService]'s `selectedPrinter` was just an in-memory var, so
 * every app relaunch meant reselecting the printer from scratch.
 */
@Singleton
class PrinterSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val MAIN_TYPE = stringPreferencesKey("main_printer_type") // BLUETOOTH | USB | BUILTIN | NETWORK
        val MAIN_NAME = stringPreferencesKey("main_printer_name")
        val MAIN_ADDRESS = stringPreferencesKey("main_printer_address") // BT mac address, or network host
        val MAIN_USB_ID = intPreferencesKey("main_printer_usb_id")
        val MAIN_PORT = intPreferencesKey("main_printer_port") // network only

        val KITCHEN_ENABLED = booleanPreferencesKey("kitchen_printer_enabled")
        val KITCHEN_NAME = stringPreferencesKey("kitchen_printer_name")
        val KITCHEN_HOST = stringPreferencesKey("kitchen_printer_host")
        val KITCHEN_PORT = intPreferencesKey("kitchen_printer_port")
    }

    private val prefsFlow = context.printerDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    /** Null if no main printer has ever been selected. */
    val mainPrinter: Flow<DiscoveredPrinter?> = prefsFlow.map { prefs ->
        val name = prefs[Keys.MAIN_NAME] ?: return@map null
        val connection = when (prefs[Keys.MAIN_TYPE]) {
            "BLUETOOTH" -> prefs[Keys.MAIN_ADDRESS]?.let { PrinterConnection.Bluetooth(it) }
            "USB" -> prefs[Keys.MAIN_USB_ID]?.let { PrinterConnection.Usb(it) }
            "BUILTIN" -> PrinterConnection.BuiltIn
            "NETWORK" -> prefs[Keys.MAIN_ADDRESS]?.let { host ->
                PrinterConnection.Network(host, prefs[Keys.MAIN_PORT] ?: DEFAULT_NETWORK_PRINTER_PORT)
            }
            else -> null
        } ?: return@map null
        DiscoveredPrinter(name, connection)
    }

    suspend fun saveMainPrinter(printer: DiscoveredPrinter) {
        context.printerDataStore.edit { prefs ->
            prefs[Keys.MAIN_NAME] = printer.name
            when (val connection = printer.connection) {
                is PrinterConnection.Bluetooth -> {
                    prefs[Keys.MAIN_TYPE] = "BLUETOOTH"
                    prefs[Keys.MAIN_ADDRESS] = connection.address
                }
                is PrinterConnection.Usb -> {
                    prefs[Keys.MAIN_TYPE] = "USB"
                    prefs[Keys.MAIN_USB_ID] = connection.deviceId
                }
                PrinterConnection.BuiltIn -> {
                    prefs[Keys.MAIN_TYPE] = "BUILTIN"
                }
                is PrinterConnection.Network -> {
                    prefs[Keys.MAIN_TYPE] = "NETWORK"
                    prefs[Keys.MAIN_ADDRESS] = connection.host
                    prefs[Keys.MAIN_PORT] = connection.port
                }
            }
        }
    }

    val kitchenPrinter: Flow<KitchenPrinterSettings> = prefsFlow.map { prefs ->
        KitchenPrinterSettings(
            enabled = prefs[Keys.KITCHEN_ENABLED] ?: false,
            name = prefs[Keys.KITCHEN_NAME] ?: "Kitchen",
            host = prefs[Keys.KITCHEN_HOST] ?: "",
            port = prefs[Keys.KITCHEN_PORT] ?: DEFAULT_NETWORK_PRINTER_PORT,
        )
    }

    suspend fun saveKitchenPrinter(settings: KitchenPrinterSettings) {
        context.printerDataStore.edit { prefs ->
            prefs[Keys.KITCHEN_ENABLED] = settings.enabled
            prefs[Keys.KITCHEN_NAME] = settings.name
            prefs[Keys.KITCHEN_HOST] = settings.host
            prefs[Keys.KITCHEN_PORT] = settings.port
        }
    }
}
