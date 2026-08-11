package com.cyebrcina.pos.printer

import com.cyebrcina.pos.printer.model.DiscoveredPrinter
import com.cyebrcina.pos.printer.model.PrintDocument
import com.cyebrcina.pos.printer.model.PrintMode
import com.cyebrcina.pos.printer.model.PrinterPaperSize
import com.cyebrcina.pos.printer.model.PrinterStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over the physical printer.
 */
interface PrinterService {
    val status: StateFlow<PrinterStatus>
    val discoveredPrinters: StateFlow<List<DiscoveredPrinter>>

    suspend fun startDiscovery()
    suspend fun stopDiscovery()
    suspend fun selectPrinter(printer: DiscoveredPrinter)

    suspend fun connect(): Result<Unit>
    suspend fun print(document: PrintDocument): Result<Unit>
    suspend fun openCashDrawer(): Result<Unit>

    /** Admin-set paper width (`ReceiptPrefs`/kitchen-ticket `prefs.paperSize`) — affects column widths for [PrintCommand.Row]. */
    fun setPaperSize(paperSize: PrinterPaperSize)

    /** Admin-set print mode (`ReceiptPrefs`/kitchen-ticket `prefs.printMode`) — ESC (text
     * commands) or POS (rendered to a bitmap, sent as a raster image). Only affects
     * Bluetooth/USB/network targets; the built-in printer has no verified bitmap-print
     * primitive and always prints in ESC mode regardless of this. */
    fun setPrintMode(printMode: PrintMode)
}
