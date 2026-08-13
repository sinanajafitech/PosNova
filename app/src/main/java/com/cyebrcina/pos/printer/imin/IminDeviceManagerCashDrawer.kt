package com.cyebrcina.pos.printer.imin

import android.content.Context
import android.util.Log
import com.device.manager.sdk.DeviceManager
import com.device.manager.server.aidl.IAsyncCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wraps `com.device.manager.sdk.DeviceManager` (from
 * `com.github.iminsoftware:IminDeviceLibrary`, see app/build.gradle.kts) — a SEPARATE service
 * from Imin's printer SDK ([IminBuiltInPrinter]). Needed because on at least one real field unit
 * the cash drawer is wired to a mainboard-level port (physically next to the power input, not
 * the printer) controlled entirely through this SDK — confirmed via `adb shell dumpsys package
 * com.imin.iotdeviceservice`, which showed a real bindable service (`IoTMainService`), unlike
 * that same unit's printer packages, which had none at all. Sending the standard ESC/POS
 * drawer-kick command to that unit's printer (over USB, confirmed otherwise working for
 * receipts) reached the printer successfully but never opened the drawer — because the drawer
 * was never wired to the printer in the first place.
 *
 * The exact method name and JSON payload are verified against Imin's own official docs
 * (oss-sg.imin.sg/docs/en/SDK.html, "Open Cash Drawer" section under Device Operations), which
 * gives this literal code sample:
 * ```
 * peripheral.addProperty("openCashBox", true);
 * mDeviceManager.sendAMCommandAsyn(new Gson().toJson(controlBean), new IAsyncCallback.Stub() {
 *     public void onResult(String result) { Log.d(TAG, result); }
 * });
 * ```
 * The AIDL interface package (`com.device.manager.server.aidl`, distinct from the Java wrapper's
 * own `com.device.manager.sdk` package) is confirmed from the library's own source tree, not
 * guessed.
 *
 * **Not exercised on physical hardware from here** — no JDK in this environment to build or run
 * against a real device — needs a real device pass before this can be fully trusted. In
 * particular, `DeviceManager.getDeviceManager()`'s internal binding (which the library says
 * differs by Android version — a local `A13DeviceService` path below API 34, a real AIDL bind
 * above it) hasn't been independently confirmed to reach this exact unit's `IoTMainService`
 * component specifically; that's inferred from the library being designed to abstract over
 * exactly this kind of device/API-level fragmentation, not verified line-by-line.
 */
@Singleton
class IminDeviceManagerCashDrawer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "IminDeviceManagerCashDrawer"

    // The exact payload from Imin's own docs sample — a single fixed literal, so built by hand
    // rather than pulling in a JSON library just for this one call.
    private val openCashBoxJson = """{"peripheralConfig":{"openCashBox":true}}"""

    suspend fun openCashDrawer(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        // Everything here — including DeviceManager.getDeviceManager() possibly returning null
        // (a real possibility from Kotlin's perspective calling into a Java platform type with
        // no nullability annotation to trust) — is wrapped in one broad catch rather than just
        // RemoteException, so an unexpected failure fails this leg gracefully and lets
        // IminPrinterService fall back to the printer-connection path, instead of crashing the
        // whole drawer-open call outright.
        try {
            val deviceManager = DeviceManager.getDeviceManager(context)
                ?: throw IllegalStateException("DeviceManager.getDeviceManager() returned null")

            deviceManager.sendAMCommandAsyn(
                openCashBoxJson,
                object : IAsyncCallback.Stub() {
                    override fun onResult(result: String?) {
                        // The SDK's own docs sample only logs this result with no
                        // success/failure branching shown — matched as closely as possible
                        // since the response schema on failure isn't documented anywhere
                        // found. A callback firing at all (vs. an outright exception, caught
                        // below) is treated as success.
                        Log.i(TAG, "openCashDrawer: result=$result")
                        if (continuation.isActive) continuation.resume(Result.success(Unit))
                    }
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "openCashDrawer: failed", e)
            if (continuation.isActive) continuation.resume(Result.failure(e))
        }
    }
}
