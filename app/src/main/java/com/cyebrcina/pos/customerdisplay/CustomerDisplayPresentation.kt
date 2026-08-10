package com.cyebrcina.pos.customerdisplay

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.cyebrcina.pos.core.theme.PosNovaTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * Hosts the customer-facing Compose UI on the D4's secondary display. [android.app.Presentation]
 * extends [android.app.Dialog] but isn't itself a LifecycleOwner/ViewModelStoreOwner/
 * SavedStateRegistryOwner, so those are propagated from the hosting Activity's context onto this
 * window's decor view — the standard pattern for hosting Compose outside an Activity window.
 */
class CustomerDisplayPresentation(
    private val outerContext: Context,
    display: Display,
    private val state: StateFlow<CustomerDisplayState>,
) : Presentation(outerContext, display) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Presentations wrap the context, so we use the outerContext (Activity) to find the owners
        val lifecycleOwner = outerContext as? LifecycleOwner
        val viewModelStoreOwner = outerContext as? ViewModelStoreOwner
        val savedStateRegistryOwner = outerContext as? SavedStateRegistryOwner

        window?.decorView?.let { decorView ->
            lifecycleOwner?.let { decorView.setViewTreeLifecycleOwner(it) }
            viewModelStoreOwner?.let { decorView.setViewTreeViewModelStoreOwner(it) }
            savedStateRegistryOwner?.let { decorView.setViewTreeSavedStateRegistryOwner(it) }
        }

        setContentView(
            ComposeView(context).apply {
                setContent {
                    val currentState by state.collectAsState()
                    PosNovaTheme {
                        CustomerDisplayScreen(state = currentState)
                    }
                }
            },
        )
    }
}
