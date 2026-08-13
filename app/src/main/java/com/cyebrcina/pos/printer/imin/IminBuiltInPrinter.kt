package com.cyebrcina.pos.printer.imin

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wraps `com.imin.printer.PrinterHelper` (from `com.github.iminsoftware:IminPrinterLibrary`,
 * see app/build.gradle.kts) for the D4's built-in thermal printer.
 *
 * Verified directly against Imin's own open-source repo
 * (github.com/iminsoftware/IminPrinterLibrary) — their hosted docs at oss-sg.imin.sg link out to
 * PDFs this environment couldn't open, so `PrinterHelper.java` / `INeoPrinterService` (the AIDL
 * interface) were read as the actual source of truth instead of guessing from the docs page.
 * Real API, but **not exercised on physical D4 hardware from here** — several things are
 * inferred rather than confirmed on-device:
 *  - Alignment ints (`printTextWithAli`, `printQrCodeWithAlign`, ...) are assumed to match
 *    `com.imin.printer.enums.Align`'s ordinal (DEFAULT=0, LEFT=1, CENTER=2, RIGHT=3) — the
 *    source uses a raw `int` param with no documented mapping, this is inferred from the enum's
 *    name and where it's used.
 *  - `getPrinterStatus()`'s return value has no status-code enum anywhere in the library, so it
 *    isn't used here at all — connection state is tracked from `InitPrinterCallback` only
 *    (bound/not bound), not fine-grained states like paper-out.
 *  - **`PRINTER_SERVICE_PACKAGE`'s value comes from library V2.0.0.19's own hardcoded
 *    `NeoPrinterManager.bindService()` source — not confirmed against this specific unit's
 *    actual firmware.** On real hardware where the bind still fails after the `<queries>` fix
 *    in AndroidManifest.xml, `connect()` below now checks the package directly via
 *    PackageManager and reports whether it's genuinely absent vs present-but-refusing, since
 *    those need different fixes (wrong package name for this device/firmware generation, vs. a
 *    permission/signature mismatch) and guessing which one blind wastes a fix cycle each time.
 */
@Singleton
class IminBuiltInPrinter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "IminBuiltInPrinter"

    companion object {
        private const val PRINTER_SERVICE_PACKAGE = "com.imin.printerservice"
    }

    @Volatile
    private var isConnected = false

    @Volatile
    private var paperSize = PrinterPaperSize.MM_58

    fun setPaperSize(paperSize: PrinterPaperSize) {
        this.paperSize = paperSize
    }

    /** What a failed bind actually means — these need different fixes, and blindly reporting
     * "not installed" (the old message) was a guess I had no way to back up. This checks the
     * package directly via PackageManager so the failure message tells the truth instead. */
    private sealed class PrinterServicePackageState {
        data class Found(val versionName: String?) : PrinterServicePackageState()
        data object NotFound : PrinterServicePackageState()
        data class CheckFailed(val reason: String) : PrinterServicePackageState()
    }

    private fun checkPrinterServicePackage(): PrinterServicePackageState = try {
        val info = context.packageManager.getPackageInfo(PRINTER_SERVICE_PACKAGE, 0)
        PrinterServicePackageState.Found(info.versionName)
    } catch (e: PackageManager.NameNotFoundException) {
        PrinterServicePackageState.NotFound
    } catch (e: Exception) {
        // Most likely this app's own <queries> declaration is missing/wrong — without it,
        // PackageManager can't even answer "is it installed" truthfully, let alone bind to it.
        PrinterServicePackageState.CheckFailed(e.message ?: e.javaClass.simpleName)
    }

    suspend fun connect(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val packageState = checkPrinterServicePackage()
        Log.i(TAG, "connect: $PRINTER_SERVICE_PACKAGE package check -> $packageState")

        val callback = object : InitPrinterCallback {
            override fun onConnected() {
                isConnected = true
                Log.i(TAG, "connect: bound to $PRINTER_SERVICE_PACKAGE")
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }

            override fun onDisconnected() {
                isConnected = false
                Log.w(TAG, "connect: $PRINTER_SERVICE_PACKAGE disconnected")
                if (continuation.isActive) {
                    continuation.resume(Result.failure(IllegalStateException("Imin printer service disconnected")))
                }
            }
        }
        val bindSubmitted = PrinterHelper.getInstance().initPrinterService(context, callback)
        Log.i(TAG, "connect: bind request submitted=$bindSubmitted (package check was: $packageState)")
        if (!bindSubmitted && continuation.isActive) {
            // Three genuinely different problems that all surfaced as the same generic
            // message before — now the on-screen error tells you which one it actually is,
            // without needing logcat/adb access to find out.
            val message = when (packageState) {
                is PrinterServicePackageState.NotFound ->
                    "$PRINTER_SERVICE_PACKAGE genuinely isn't installed on this device — this hardware may use a different printer service package than this app expects."
                is PrinterServicePackageState.Found ->
                    "$PRINTER_SERVICE_PACKAGE is installed (version ${packageState.versionName ?: "unknown"}) but refused the connection — likely a permission or signature mismatch, not a missing app."
                is PrinterServicePackageState.CheckFailed ->
                    "Couldn't even check whether $PRINTER_SERVICE_PACKAGE is installed (${packageState.reason})."
            }
            continuation.resume(Result.failure(IllegalStateException(message)))
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
     * `openDrawer(fd)` in the underlying AIDL is `void` and swallows its own RemoteException
     * internally (PrinterHelper.java: catches, calls `e.printStackTrace()`, never rethrows) —
     * so a Kotlin-side `runCatching` around it can never observe a failure; it always looks
     * like it "worked" even when nothing happened. The only signal this SDK actually exposes
     * is `getOpenDrawerTimes()`, a counter that increments each time a kick genuinely reaches
     * the hardware — so this compares that counter before/after the call and treats "counter
     * didn't move" as the real failure signal, rather than reporting blind success.
     *
     * Also reconnects and retries once if the counter didn't move — the built-in printer
     * service can die (killed under memory pressure, or between a receipt print and a later
     * manual drawer-open) while `isConnected` still reads true from the last successful
     * connect, which would otherwise make every kick after that point a permanently silent
     * no-op with no way to recover short of restarting the app.
     */
    suspend fun openCashDrawer(): Result<Unit> {
        if (!isConnected) {
            val connected = connect()
            if (connected.isFailure) return connected
        }

        if (tryKickAndVerify()) return Result.success(Unit)

        Log.w(TAG, "openCashDrawer: drawer counter didn't move — reconnecting and retrying once")
        isConnected = false
        val reconnected = connect()
        if (reconnected.isFailure) {
            return Result.failure(
                reconnected.exceptionOrNull() ?: IllegalStateException("Couldn't reconnect to the printer service"),
            )
        }

        if (tryKickAndVerify()) return Result.success(Unit)

        Log.e(TAG, "openCashDrawer: still no change after reconnect — is the drawer cable plugged into the printer's RJ11/RJ12 port?")
        return Result.failure(
            IllegalStateException("The printer didn't confirm the drawer opened — check its cable is plugged into the RJ11/RJ12 port."),
        )
    }

    /** Fires the kick and confirms it via the before/after counter. -1 means the SDK itself
     * couldn't read the counter (not bound / dead service) — never treated as "it moved". */
    private suspend fun tryKickAndVerify(): Boolean {
        val before = PrinterHelper.getInstance().getOpenDrawerTimes()
        PrinterHelper.getInstance().openDrawer()
        // openDrawer() doesn't block on the hardware acknowledging the kick — give the AIDL
        // round-trip a moment before re-reading the counter.
        delay(150)
        val after = PrinterHelper.getInstance().getOpenDrawerTimes()
        Log.i(TAG, "openCashDrawer: drawer-open counter $before -> $after")
        return after >= 0 && after > before
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
