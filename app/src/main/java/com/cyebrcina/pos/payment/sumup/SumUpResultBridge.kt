package com.cyebrcina.pos.payment.sumup

import android.app.Activity
import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * SumUp's checkout SDK is Activity-result based (it launches its own full-screen checkout
 * Activity and hands the result back via `onActivityResult`), which doesn't fit a plain suspend
 * function on its own. This bridges that callback into a coroutine.
 *
 * [MainActivity] registers itself here in `onStart`/clears in `onStop` (same lifecycle-scoped
 * pattern as [com.cyebrcina.pos.customerdisplay.CustomerDisplayManager]) and forwards its
 * `onActivityResult` for [SUMUP_CHECKOUT_REQUEST_CODE] into [deliverResult].
 * [SumUpPaymentService] calls [launchForResult] to start SumUp's checkout Intent and suspend
 * until that result arrives.
 */
@Singleton
class SumUpResultBridge @Inject constructor() {

    private var hostActivity: Activity? = null
    private var pending: CancellableContinuation<Pair<Int, Intent?>>? = null

    fun attach(activity: Activity) {
        hostActivity = activity
    }

    fun detach() {
        hostActivity = null
    }

    /** Launches [intent] via `startActivityForResult` on the attached Activity and suspends for its result. */
    suspend fun launchForResult(intent: Intent): Pair<Int, Intent?> {
        val activity = hostActivity ?: return Activity.RESULT_CANCELED to null
        return suspendCancellableCoroutine { continuation ->
            pending = continuation
            continuation.invokeOnCancellation { pending = null }
            @Suppress("DEPRECATION")
            activity.startActivityForResult(intent, SUMUP_CHECKOUT_REQUEST_CODE)
        }
    }

    fun deliverResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != SUMUP_CHECKOUT_REQUEST_CODE) return
        pending?.resume(resultCode to data)
        pending = null
    }

    companion object {
        const val SUMUP_CHECKOUT_REQUEST_CODE = 1001
    }
}
