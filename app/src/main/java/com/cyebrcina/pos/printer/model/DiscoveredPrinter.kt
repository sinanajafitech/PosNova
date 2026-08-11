package com.cyebrcina.pos.printer.model

sealed class PrinterConnection {
    data class Bluetooth(val address: String) : PrinterConnection()
    data class Usb(val deviceId: Int) : PrinterConnection()
    data object BuiltIn : PrinterConnection()
    /** A network ESC/POS printer, addressed by IP/hostname — not something [startDiscovery]
     * scans for (unlike Bluetooth pairing/USB), always entered manually since there's no
     * reliable local-network printer discovery protocol to rely on here. */
    data class Network(val host: String, val port: Int) : PrinterConnection()
}

data class DiscoveredPrinter(
    val name: String,
    val connection: PrinterConnection,
)
