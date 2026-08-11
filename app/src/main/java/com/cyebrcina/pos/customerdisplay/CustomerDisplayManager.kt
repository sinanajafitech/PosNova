package com.cyebrcina.pos.customerdisplay

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import com.cyebrcina.pos.data.repository.AuthRepository
import com.cyebrcina.pos.data.repository.OrderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Detects the Imin D4's second, customer-facing display and shows a [CustomerDisplayPresentation]
 * on it. Call [start] from the hosting Activity's onStart and [stop] from onStop so the
 * presentation's lifetime tracks the activity — Android's [android.app.Presentation] is meant to
 * be scoped to the activity that owns it, not to the process. Safe to call on single-screen
 * devices/emulators: if no secondary display is present, [start] is a no-op.
 */
@Singleton
class CustomerDisplayManager @Inject constructor(
    authRepository: AuthRepository,
    orderRepository: OrderRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<CustomerDisplayState>(CustomerDisplayState.Idle)
    val state: StateFlow<CustomerDisplayState> = _state.asStateFlow()

    /** Store-wide branding, independent of [state] — see [CustomerDisplayBranding]. */
    val branding: StateFlow<CustomerDisplayBranding> = combine(
        authRepository.session,
        orderRepository.customerDisplay,
    ) { session, config ->
        CustomerDisplayBranding(
            storeName = session?.storeName.orEmpty(),
            logoUrl = session?.logoUrl,
            backgroundUrl = config?.backgroundUrl,
            backgroundOpacity = config?.backgroundOpacity ?: 100,
            idlePromoVideoUrl = config?.idlePromoVideoUrl,
        )
    }.stateIn(scope, SharingStarted.Eagerly, CustomerDisplayBranding())

    private var displayManager: DisplayManager? = null
    private var presentation: CustomerDisplayPresentation? = null
    private var hostContext: Context? = null

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = attachIfSecondaryAvailable()
        override fun onDisplayRemoved(displayId: Int) {
            presentation?.dismiss()
            presentation = null
        }
        override fun onDisplayChanged(displayId: Int) = Unit
    }

    fun start(activityContext: Context) {
        hostContext = activityContext
        val manager = activityContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager = manager
        manager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        attachIfSecondaryAvailable()
    }

    fun stop() {
        displayManager?.unregisterDisplayListener(listener)
        presentation?.dismiss()
        presentation = null
        displayManager = null
        hostContext = null
    }

    fun update(newState: CustomerDisplayState) {
        _state.value = newState
    }

    private fun attachIfSecondaryAvailable() {
        if (presentation != null) return
        val context = hostContext ?: return
        val manager = displayManager ?: return
        val secondary = manager.displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY } ?: return
        presentation = CustomerDisplayPresentation(context, secondary, state, branding).also {
            runCatching { it.show() }
        }
    }
}
