package com.cyebrcina.pos.feature.waitercall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.feature.order.create.FlowPrimaryButton

/**
 * Mounted once at the top of [com.cyebrcina.pos.core.navigation.MainGraphHost] so it can pop up
 * over whichever tab staff happen to be on. Requires an explicit tap to dismiss — no
 * outside-tap/back-button dismissal — since this is meant to be noticed and acted on, not
 * swallowed accidentally.
 */
@Composable
fun WaiterCallOverlay(viewModel: WaiterCallViewModel = hiltViewModel()) {
    val event by viewModel.current.collectAsStateWithLifecycle()
    val current = event ?: return

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = PosColors.White) {
            Column(
                modifier = Modifier.width(380.dp).padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(PosColors.Danger),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = PosColors.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Table ${current.tableNumber} needs you",
                    style = PosTextStyles.h5,
                    color = PosColors.Neutral13,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    "A customer at this table has called for a waiter.",
                    style = PosTextStyles.bodyMediumRegular,
                    color = PosColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.lg))
                FlowPrimaryButton(
                    text = "Acknowledge",
                    onClick = viewModel::acknowledgeCurrent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
