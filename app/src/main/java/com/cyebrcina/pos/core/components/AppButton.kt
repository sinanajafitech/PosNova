package com.cyebrcina.pos.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosNovaShapes
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing

enum class AppButtonSize { LARGE, MEDIUM }

/**
 * Primary filled action button, matching the kit's Button component (pill/rounded, MEDIUM/LARGE
 * sizing) — used where [com.cyebrcina.pos.feature.order.create.FlowPrimaryButton]'s fixed 54dp
 * pill doesn't fit (e.g. AlertDialog's compact button row in [ConfirmDialog]). Defaults to the
 * app's teal brand color (Login/Splash's deliberate "outside the till flow" cue); pass [color]
 * to match the app-wide blue accent instead.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    size: AppButtonSize = AppButtonSize.LARGE,
    color: Color = PosColors.Primary500,
    disabledColor: Color = PosColors.Primary200,
    leadingIcon: (@Composable RowScope.() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(if (size == AppButtonSize.LARGE) 52.dp else 44.dp),
        enabled = enabled && !loading,
        shape = PosNovaShapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = PosColors.White,
            disabledContainerColor = disabledColor,
            disabledContentColor = PosColors.White,
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
    ) {
        ButtonContent(text, loading, PosColors.White, leadingIcon)
    }
}

/** Secondary outlined button, for lower-emphasis actions (cancel, back, secondary flows). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    size: AppButtonSize = AppButtonSize.LARGE,
    color: Color = PosColors.Primary500,
    leadingIcon: (@Composable RowScope.() -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(if (size == AppButtonSize.LARGE) 52.dp else 44.dp),
        enabled = enabled && !loading,
        shape = PosNovaShapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
    ) {
        ButtonContent(text, loading, color, leadingIcon)
    }
}

/** Text-only, lowest-emphasis action (e.g. "Forgot password?"). */
@Composable
fun TertiaryTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = PosColors.Primary500,
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(text = text, style = PosTextStyles.bodySmallSemibold, color = color)
    }
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    loading: Boolean,
    contentColor: Color,
    leadingIcon: (@Composable RowScope.() -> Unit)?,
) {
    if (loading) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
        }
    } else {
        leadingIcon?.invoke(this)
        Text(
            text = text,
            style = PosTextStyles.bodyMediumSemibold,
            modifier = Modifier.padding(start = if (leadingIcon != null) Spacing.xxs else 0.dp),
        )
    }
}
