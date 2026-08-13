package com.cyebrcina.pos.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A dialog-style overlay confined to the current composition, not a separate platform `Window`
 * like [androidx.compose.ui.window.Dialog]/`AlertDialog` open. Those always create a modal
 * Android Window sized to cover the whole Activity for touch purposes (that's how their own
 * tap-outside-to-dismiss works) — which, on a screen mounted inside [com.cyebrcina.pos.core.navigation.MainScaffold]'s
 * content slot (i.e. every screen in the app, alongside the persistent NavigationRail/Bar), also
 * silently swallows taps on that rail/bar while the dialog is open: the tap never reaches the
 * nav item, it's consumed as "outside touch" and just dismisses the dialog instead. This is
 * confined to whatever `Modifier.fillMaxSize()` resolves to in its own composition slot — the
 * content area beside the rail, not the rail itself — so the rail/bar stays clickable regardless
 * of what's open on screen. Used by the New Order flow's dialogs
 * ([com.cyebrcina.pos.feature.order.create.ProductDetailDialog],
 * [com.cyebrcina.pos.feature.order.create.ChooseTableDialog],
 * [com.cyebrcina.pos.feature.order.create.StartOrderDialog]), the flow where this was first
 * reported ("click Dashboard while on New Order, nothing happens").
 */
@Composable
fun AppOverlayDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismissRequest,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Swallows taps on the card itself so they don't bubble up to the scrim's dismiss click.
        Box(modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = {})) {
            content()
        }
    }
}
