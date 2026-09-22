package com.cyebrcina.pos.printer.imin

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.cyebrcina.pos.BuildConfig
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
 *  - `initPrinterService(context, callback)` takes no package argument — the actual bind target
 *    is hardcoded inside the third-party SDK (`NeoPrinterManager.bindService()` in
 *    IminPrinterLibrary V2.0.0.19), not chosen by anything below. `POSSIBLE_PACKAGES` /
 *    `findInstalledPrinterPackage()` exist purely to make failure messages honest about what's
 *    actually on the device (several firmware generations ship the printer service under
 *    different package names) — finding a package here does NOT change which one the SDK binds
 *    to, so don't read a "Found" state as "this is now working."
 */
@Singleton
class IminBuiltInPrinter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "IminBuiltInPrinter"

    companion object {
        private val POSSIBLE_PACKAGES = listOf(
            "com.imin.printerservice",
            "com.imin.printer.service",
            "com.imin.print",
            "com.imin.library",
            "com.imin.printer.service.v2",
        )
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
     * candidate packages directly via PackageManager so the failure message tells the truth. */
    private sealed class PrinterServicePackageState {
        data class Found(val packageName: String, val versionName: String?) : PrinterServicePackageState()
        data object NotFound : PrinterServicePackageState()
        data class CheckFailed(val reason: String) : PrinterServicePackageState()
    }

    private fun findInstalledPrinterPackage(): PrinterServicePackageState {
        if (BuildConfig.DEBUG) logInstalledCandidatePackages()
        for (pkg in POSSIBLE_PACKAGES) {
            try {
                val info = context.packageManager.getPackageInfo(pkg, 0)
                return PrinterServicePackageState.Found(pkg, info.versionName)
            } catch (e: PackageManager.NameNotFoundException) {
                continue
            } catch (e: Exception) {
                // Most likely this app's own <queries> declaration is missing/wrong for this
                // candidate — without it, PackageManager can't even answer "is it installed"
                // truthfully, let alone bind to it.
                return PrinterServicePackageState.CheckFailed(e.message ?: e.javaClass.simpleName)
            }
        }
        return PrinterServicePackageState.NotFound
    }

    /** Debug-only: which of the candidate packages getPackageInfo() can actually see. Only
     * covers packages declared in the manifest's <queries> — this app does not request
     * QUERY_ALL_PACKAGES (Play Store restricts that permission without an approved use case). */
    private fun logInstalledCandidatePackages() {
        for (pkg in POSSIBLE_PACKAGES) {
            val versionName = try {
                context.packageManager.getPackageInfo(pkg, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
            Log.i(TAG, "candidate package $pkg -> ${if (versionName != null) "installed ($versionName)" else "not visible"}")
        }
    }

    suspend fun connect(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val packageState = findInstalledPrinterPackage()
        Log.i(TAG, "connect: candidate package check -> $packageState")

        val callback = object : InitPrinterCallback {
            override fun onConnected() {
                isConnected = true
                Log.i(TAG, "connect: bound to printer service")
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }

            override fun onDisconnected() {
                isConnected = false
                Log.w(TAG, "connect: printer service disconnected")
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
                    "None of ${POSSIBLE_PACKAGES.joinToString()} are installed on this device — this hardware may use a printer service package this app doesn't know about yet."
                is PrinterServicePackageState.Found ->
                    "${packageState.packageName} is installed (version ${packageState.versionName ?: "unknown"}) but the SDK's bind still failed — likely a permission or signature mismatch, not a missing app."
                is PrinterServicePackageState.CheckFailed ->
                    "Couldn't check which printer service package is installed (${packageState.reason})."
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
