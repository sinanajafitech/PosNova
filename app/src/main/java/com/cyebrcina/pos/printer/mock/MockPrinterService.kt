package com.cyebrcina.pos.printer.mock

import android.util.Log
import com.cyebrcina.pos.printer.PrinterService
import com.cyebrcina.pos.printer.model.DiscoveredPrinter
import com.cyebrcina.pos.printer.model.PrinterConnection
import com.cyebrcina.pos.printer.model.PrintCommand
import com.cyebrcina.pos.printer.model.PrintDocument
import com.cyebrcina.pos.printer.model.PrinterPaperSize
import com.cyebrcina.pos.printer.model.PrinterStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * No-hardware printer used in debug builds and on the emulator: logs a formatted rendering of
 * every print job instead of talking to real hardware, so the whole app (including print flows)
 * is testable without an Imin device.
 */
@Singleton
class MockPrinterService @Inject constructor() : PrinterService {

    private val _status = MutableStateFlow(PrinterStatus.UNKNOWN)
    override val status: StateFlow<PrinterStatus> = _status

    private val _discoveredPrinters = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    override val discoveredPrinters: StateFlow<List<DiscoveredPrinter>> = _discoveredPrinters.asStateFlow()

    override suspend fun startDiscovery() {
        delay(500)
        _discoveredPrinters.value = listOf(
            DiscoveredPrinter("Mock Bluetooth Printer", PrinterConnection.Bluetooth("00:11:22:33:44:55")),
            DiscoveredPrinter("Mock USB Printer", PrinterConnection.Usb(1234)),
            DiscoveredPrinter("Built-in Printer", PrinterConnection.BuiltIn)
        )
    }

    override suspend fun stopDiscovery() {
        _discoveredPrinters.value = emptyList()
    }

    override suspend fun selectPrinter(printer: DiscoveredPrinter) {
        _status.value = PrinterStatus.READY
        Log.d(TAG, "Selected printer: ${printer.name}")
    }

    override suspend fun connect(): Result<Unit> {
        delay(200)
        _status.value = PrinterStatus.READY
        return Result.success(Unit)
    }

    override suspend fun print(document: PrintDocument): Result<Unit> {
        if (_status.value != PrinterStatus.READY) connect()
        delay(400)
        Log.d(TAG, renderForLog(document))
        return Result.success(Unit)
    }

    override suspend fun openCashDrawer(): Result<Unit> {
        delay(150)
        Log.d(TAG, "[cash drawer kicked]")
        return Result.success(Unit)
    }

    private var paperSize = PrinterPaperSize.MM_58

    override fun setPaperSize(paperSize: PrinterPaperSize) {
        this.paperSize = paperSize
    }

    private fun renderForLog(document: PrintDocument): String = buildString {
        val rightWidth = 12
        val leftWidth = paperSize.charsPerLine - rightWidth
        appendLine("──────── MOCK PRINT JOB (${paperSize.name}) ────────")
        document.forEach { command ->
            when (command) {
                is PrintCommand.Text -> appendLine((if (command.bold) "**" else "") + command.text + (if (command.bold) "**" else ""))
                is PrintCommand.Row -> appendLine("${command.left.padEnd(leftWidth)}${command.right.padStart(rightWidth)}")
                is PrintCommand.Divider -> appendLine("-".repeat(paperSize.charsPerLine))
                is PrintCommand.QrCode -> appendLine("[QR: ${command.content}]")
                is PrintCommand.FeedLines -> repeat(command.lines) { appendLine() }
                PrintCommand.Cut -> appendLine("[cut]")
            }
        }
        append("─────────────────────────────────")
    }

    private companion object {
        const val TAG = "MockPrinterService"
    }
}
