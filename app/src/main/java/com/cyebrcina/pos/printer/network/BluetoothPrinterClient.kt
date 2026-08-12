package com.cyebrcina.pos.printer.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

/**
 * Opens an RFCOMM (SPP) socket to a *paired* Bluetooth ESC/POS printer by MAC address, writes
 * the bytes, and closes — the kitchen-printer counterpart to [NetworkPrinterClient]'s
 * one-connection-per-job pattern. Deliberately independent of
 * [com.cyebrcina.pos.printer.imin.IminPrinterService]'s own Bluetooth connection (a persistent,
 * stateful connection to the *main* printer), so a kitchen printer somewhere else in the
 * building can never interfere with, or be interfered with by, the till's own receipt printer's
 * connection state.
 */
@Singleton
class BluetoothPrinterClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    @SuppressLint("MissingPermission")
    suspend fun send(address: String, bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val adapter = bluetoothManager.adapter ?: throw IllegalStateException("Bluetooth isn't available on this device")
            val device = adapter.getRemoteDevice(address)
            device.createRfcommSocketToServiceRecord(SPP_UUID).use { socket ->
                socket.connect()
                socket.outputStream.apply {
                    write(bytes)
                    flush()
                }
            }
            Unit
        }.onFailure { Log.e(TAG, "Bluetooth print to $address failed", it) }
    }

    private companion object {
        const val TAG = "BluetoothPrinterClient"
    }
}
