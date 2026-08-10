package com.cyebrcina.pos.printer.model

sealed class PrinterConnection {
    data class Bluetooth(val address: String) : PrinterConnection()
    data class Usb(val deviceId: Int) : PrinterConnection()
    data object BuiltIn : PrinterConnection()
}

data class DiscoveredPrinter(
    val name: String,
    val connection: PrinterConnection,
)
