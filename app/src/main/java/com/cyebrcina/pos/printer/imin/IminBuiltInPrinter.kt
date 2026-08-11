package com.cyebrcina.pos.printer.imin

import android.content.Context
import com.cyebrcina.pos.printer.model.PrintAlign
import com.cyebrcina.pos.printer.model.PrintCommand
import com.cyebrcina.pos.printer.model.PrintDocument
import com.cyebrcina.pos.printer.model.PrintTextSize
import com.cyebrcina.pos.printer.model.PrinterPaperSize
import com.imin.printer.INeoPrinterCallback
import com.imin.printer.InitPrinterCallback
import com.imin.printer.PrinterHelper
import com.imin.printer.enums.Align
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wraps `com.imin.printer.PrinterHelper` (from `com.github.iminsoftware:IminPrinterLibrary`,
 * see app/build.gradle.kts) for the D4's built-in thermal printer.
 *
 * Verified directly against Imin's own open-source repo
 * (github.com/iminsoftware/IminPrinterLibrary) — their hosted docs at oss-sg.imin.sg link out to
 * PDFs this environment couldn't open, so `PrinterHelper.java` / `INeoPrinterService` (the AIDL
 * interface) were read as the actual source of truth instead of guessing from the docs page.
 * Real API, but **not exercised on physical D4 hardware from here** — two specific things are
 * inferred rather than documented and worth confirming on-device:
 *  - Alignment ints (`printTextWithAli`, `printQrCodeWithAlign`, ...) are assumed to match
 *    `com.imin.printer.enums.Align`'s ordinal (DEFAULT=0, LEFT=1, CENTER=2, RIGHT=3) — the
 *    source uses a raw `int` param with no documented mapping, this is inferred from the enum's
 *    name and where it's used.
 *  - `getPrinterStatus()`'s return value has no status-code enum anywhere in the library, so it
 *    isn't used here at all — connection state is tracked from `InitPrinterCallback` only
 *    (bound/not bound), not fine-grained states like paper-out.
 */
@Singleton
class IminBuiltInPrinter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile
    private var isConnected = false

    @Volatile
    private var paperSize = PrinterPaperSize.MM_58

    fun setPaperSize(paperSize: PrinterPaperSize) {
        this.paperSize = paperSize
    }

    suspend fun connect(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val callback = object : InitPrinterCallback {
            override fun onConnected() {
                isConnected = true
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }

            override fun onDisconnected() {
                isConnected = false
                if (continuation.isActive) {
                    continuation.resume(Result.failure(IllegalStateException("Imin printer service disconnected")))
                }
            }
        }
        val bindSubmitted = PrinterHelper.getInstance().initPrinterService(context, callback)
        if (!bindSubmitted && continuation.isActive) {
            continuation.resume(
                Result.failure(IllegalStateException("Couldn't bind com.imin.printerservice — is it installed/running on this device?")),
            )
        }
    }

    fun disconnect() {
        PrinterHelper.getInstance().deInitPrinterService(context)
        isConnected = false
    }

    suspend fun print(document: PrintDocument): Result<Unit> {
        if (!isConnected) {
            val connected = connect()
            if (connected.isFailure) return Result.failure(connected.exceptionOrNull() ?: IllegalStateException("Not connected"))
        }
        for (command in document) {
            val ok = when (command) {
                is PrintCommand.Text -> printText(command)
                is PrintCommand.Row -> {
                    val rightWidth = 8
                    val leftWidth = paperSize.charsPerLine - rightWidth
                    printText(PrintCommand.Text(command.left.padEnd(leftWidth) + command.right.padStart(rightWidth), bold = command.bold))
                }
                is PrintCommand.HighlightBox -> {
                    val rightWidth = 8
                    val leftWidth = paperSize.charsPerLine - rightWidth
                    // No verified "inverse/reverse print" primitive in PrinterHelper's public API
                    // (unlike GS B on ESC/POS — see EscPosEncoder) — approximated with bold + the
                    // largest size step rather than guessing at an undocumented call that could
                    // fail silently or crash on real hardware.
                    printText(
                        PrintCommand.Text(
                            command.left.padEnd(leftWidth) + command.right.padStart(rightWidth),
                            size = PrintTextSize.XLARGE,
                            bold = true,
                        ),
                    )
                }
                PrintCommand.Divider -> printText(PrintCommand.Text("-".repeat(paperSize.charsPerLine)))
                is PrintCommand.QrCode -> printQrCode(command)
                is PrintCommand.FeedLines -> {
                    repeat(command.lines) { PrinterHelper.getInstance().printAndLineFeed() }
                    true
                }
                PrintCommand.Cut -> {
                    PrinterHelper.getInstance().partialCut()
                    true
                }
            }
            if (!ok) return Result.failure(IllegalStateException("Imin printer rejected a print command: $command"))
        }
        return Result.success(Unit)
    }

    /**
     * `openDrawer(fd)` in the underlying AIDL is fire-and-forget (no result callback), so this
     * can only report whether the call was issued, not whether the drawer physically opened.
     */
    suspend fun openCashDrawer(): Result<Unit> {
        if (!isConnected) {
            val connected = connect()
            if (connected.isFailure) return connected
        }
        return runCatching { PrinterHelper.getInstance().openDrawer() }
    }

    private suspend fun printText(text: PrintCommand.Text): Boolean {
        PrinterHelper.getInstance().setFontBold(text.bold)
        val scale = when (text.size) {
            PrintTextSize.SMALL, PrintTextSize.NORMAL -> 1
            PrintTextSize.LARGE -> 2
            PrintTextSize.XLARGE -> 3
        }
        PrinterHelper.getInstance().setFontMultiple(scale, scale)
        return awaitRunResult { callback ->
            PrinterHelper.getInstance().printTextWithAli(text.text + "\n", text.align.toIminAlign(), callback)
        }
    }

    private suspend fun printQrCode(qr: PrintCommand.QrCode): Boolean {
        // sizeDp is a dp hint from the printer-agnostic model; Imin's setQrCodeSize unit isn't
        // documented, so this is a rough dp->their-unit approximation, not a verified mapping.
        PrinterHelper.getInstance().setQrCodeSize((qr.sizeDp / 20).coerceIn(3, 16))
        return awaitRunResult { callback ->
            PrinterHelper.getInstance().printQrCodeWithAlign(qr.content, Align.CENTER.ordinal, callback)
        }
    }

    private fun PrintAlign.toIminAlign(): Int = when (this) {
        PrintAlign.LEFT -> Align.LEFT.ordinal
        PrintAlign.CENTER -> Align.CENTER.ordinal
        PrintAlign.RIGHT -> Align.RIGHT.ordinal
    }

    private suspend fun awaitRunResult(action: (INeoPrinterCallback) -> Unit): Boolean =
        suspendCancellableCoroutine { continuation ->
            val callback = object : INeoPrinterCallback() {
                override fun onRunResult(isSuccess: Boolean) {
                    if (continuation.isActive) continuation.resume(isSuccess)
                }

                override fun onReturnString(result: String?) = Unit

                override fun onRaiseException(code: Int, msg: String?) {
                    if (continuation.isActive) continuation.resume(false)
                }

                override fun onPrintResult(code: Int, msg: String?) = Unit
            }
            action(callback)
        }
}
