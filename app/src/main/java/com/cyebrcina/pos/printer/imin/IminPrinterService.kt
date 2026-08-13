package com.cyebrcina.pos.printer.imin

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.cyebrcina.pos.printer.PrinterService
import com.cyebrcina.pos.printer.escpos.EscPosEncoder
import com.cyebrcina.pos.printer.escpos.EscPosRasterEncoder
import com.cyebrcina.pos.printer.graphic.ReceiptBitmapRenderer
import com.cyebrcina.pos.printer.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class IminPrinterService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val builtInPrinter: IminBuiltInPrinter,
) : PrinterService {

    private val TAG = "IminPrinterService"
    private val ACTION_USB_PERMISSION = "com.cyebrcina.pos.USB_PERMISSION"
    
    private val _status = MutableStateFlow(PrinterStatus.DISCONNECTED)
    override val status: StateFlow<PrinterStatus> = _status

    private val _discoveredPrinters = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    override val discoveredPrinters: StateFlow<List<DiscoveredPrinter>> = _discoveredPrinters.asStateFlow()

    private var selectedPrinter: DiscoveredPrinter? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var bluetoothSocket: BluetoothSocket? = null
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var usbConnection: UsbDeviceConnection? = null
    private var usbEndpoint: UsbEndpoint? = null

    private var networkSocket: Socket? = null

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { 
                            Log.d(TAG, "USB Permission granted for ${it.deviceName}")
                            serviceScope.launch { connect() }
                        }
                    } else {
                        Log.d(TAG, "USB Permission denied")
                        _status.value = PrinterStatus.ERROR
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun startDiscovery() {
        val printers = mutableListOf<DiscoveredPrinter>()
        printers.add(DiscoveredPrinter("Imin Built-in Printer", PrinterConnection.BuiltIn))

        bluetoothManager.adapter?.bondedDevices?.forEach { device ->
            printers.add(DiscoveredPrinter(device.name ?: "Unknown BT Device", PrinterConnection.Bluetooth(device.address)))
        }

        usbManager.deviceList.values.forEach { device ->
            if (isUsbPrinter(device)) {
                printers.add(DiscoveredPrinter(device.productName ?: "USB Printer", PrinterConnection.Usb(device.deviceId)))
            }
        }
        _discoveredPrinters.value = printers
    }

    private fun isUsbPrinter(device: UsbDevice): Boolean {
        for (i in 0 until device.interfaceCount) {
            if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_PRINTER) return true
        }
        return false
    }

    override suspend fun stopDiscovery() = Unit

    override suspend fun selectPrinter(printer: DiscoveredPrinter) {
        selectedPrinter = printer
        connect()
    }

    override suspend fun connect(): Result<Unit> {
        val printer = selectedPrinter ?: return Result.failure(IllegalStateException("No printer selected"))
        _status.value = PrinterStatus.BUSY
        return when (val conn = printer.connection) {
            is PrinterConnection.Bluetooth -> connectBluetooth(conn.address)
            is PrinterConnection.Usb -> connectUsb(conn.deviceId)
            is PrinterConnection.BuiltIn -> builtInPrinter.connect().also {
                _status.value = if (it.isSuccess) PrinterStatus.READY else PrinterStatus.ERROR
            }
            is PrinterConnection.Network -> connectNetwork(conn.host, conn.port)
        }
    }

    private suspend fun connectNetwork(host: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            networkSocket?.close()
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 5000)
            networkSocket = socket
            _status.value = PrinterStatus.READY
            Unit
        }.onFailure {
            Log.e(TAG, "Network connect to $host:$port failed", it)
            _status.value = PrinterStatus.ERROR
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectBluetooth(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            bluetoothSocket?.close()
            val device = bluetoothManager.adapter.getRemoteDevice(address)
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            bluetoothSocket = socket
            _status.value = PrinterStatus.READY
            Unit
        }.onFailure {
            Log.e(TAG, "BT connect failed", it)
            _status.value = PrinterStatus.ERROR
        }
    }

    private suspend fun connectUsb(deviceId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val device = usbManager.deviceList.values.find { it.deviceId == deviceId }
            ?: return@withContext Result.failure(Exception("USB device not found"))

        if (!usbManager.hasPermission(device)) {
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            )
            usbManager.requestPermission(device, permissionIntent)
            return@withContext Result.success(Unit) // Waiting for receiver
        }

        runCatching {
            val connection = usbManager.openDevice(device) ?: throw Exception("Failed to open USB device")
            val intf = findPrinterInterface(device) ?: throw Exception("No printer interface found")
            val endpoint = findPrinterEndpoint(intf) ?: throw Exception("No output endpoint found")
            connection.claimInterface(intf, true)
            usbConnection = connection
            usbEndpoint = endpoint
            _status.value = PrinterStatus.READY
            Unit
        }.onFailure {
            Log.e(TAG, "USB connect failed", it)
            _status.value = PrinterStatus.ERROR
        }
    }

    private fun findPrinterInterface(device: UsbDevice): UsbInterface? {
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == UsbConstants.USB_CLASS_PRINTER) return intf
        }
        return null
    }

    private fun findPrinterEndpoint(intf: UsbInterface): UsbEndpoint? {
        for (i in 0 until intf.endpointCount) {
            val ep = intf.getEndpoint(i)
            if (ep.direction == UsbConstants.USB_DIR_OUT) return ep
        }
        return null
    }

    private var paperSize = PrinterPaperSize.MM_58
    private var printMode = PrintMode.ESC

    override fun setPaperSize(paperSize: PrinterPaperSize) {
        this.paperSize = paperSize
        builtInPrinter.setPaperSize(paperSize)
    }

    override fun setPrintMode(printMode: PrintMode) {
        this.printMode = printMode
    }

    override suspend fun print(document: PrintDocument): Result<Unit> {
        val printer = selectedPrinter ?: return Result.failure(IllegalStateException("No printer selected"))

        // Reconnect if needed
        if (_status.value != PrinterStatus.READY) {
            connect()
            if (_status.value != PrinterStatus.READY) return Result.failure(Exception("Printer not ready"))
        }

        if (printer.connection is PrinterConnection.BuiltIn) {
            // No verified bitmap-print primitive in PrinterHelper's public API (see
            // HighlightBox's fallback above for the same limitation) — always ESC/text mode
            // here regardless of the admin-set printMode.
            return builtInPrinter.print(document)
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val bytes = if (printMode == PrintMode.POS) {
                    EscPosRasterEncoder.encode(ReceiptBitmapRenderer.render(document, paperSize))
                } else {
                    EscPosEncoder.encode(document, paperSize)
                }
                when (val connection = printer.connection) {
                    is PrinterConnection.Bluetooth -> {
                        val socket = bluetoothSocket ?: throw Exception("Not connected")
                        socket.outputStream.write(bytes)
                        socket.outputStream.flush()
                    }
                    is PrinterConnection.Usb -> {
                        val conn = usbConnection ?: throw Exception("USB not connected")
                        val ep = usbEndpoint ?: throw Exception("USB endpoint null")
                        conn.bulkTransfer(ep, bytes, bytes.size, 5000)
                    }
                    is PrinterConnection.Network -> {
                        val socket = networkSocket ?: throw Exception("Not connected")
                        socket.getOutputStream().write(bytes)
                        socket.getOutputStream().flush()
                    }
                    is PrinterConnection.BuiltIn -> error("unreachable — handled above")
                }
                Result.success(Unit)
            }.getOrElse {
                Log.e(TAG, "Print failed", it)
                Result.failure(it)
            }
        }
    }

    override suspend fun openCashDrawer(): Result<Unit> {
        // The D4's cash drawer kick-out circuit is wired through the device's
        // own built-in thermal printer — physically independent of whatever
        // printer (if any) is configured for receipts. Previously this
        // routed through `selectedPrinter`, which meant a till with no
        // printer selected yet failed outright ("No printer selected"), and
        // a till configured to print receipts to an external Bluetooth/USB/
        // network printer would kick that printer's port instead of the
        // D4's own — the wrong drawer, or no drawer at all, on real
        // hardware. Always goes through Imin's own SDK (openDrawer(), via
        // IminBuiltInPrinter) regardless of the selected receipt printer.
        return builtInPrinter.openCashDrawer()
    }
}
