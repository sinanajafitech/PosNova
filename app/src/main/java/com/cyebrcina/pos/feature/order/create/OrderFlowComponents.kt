package com.cyebrcina.pos.feature.order.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing

/** Fully-pill radius used throughout the New Order / Payment flow (Figma r=100). */
val PillShape = RoundedCornerShape(percent = 50)

/**
 * The New Order / Choose Table / Payment Order screens use a blue (#2D71F7) primary accent for
 * their CTAs, distinct from the app's global teal brand button ([com.cyebrcina.pos.core.components.PrimaryButton]) —
 * this matches the Blue-DPOP scale that recurs across pricing/selection state in this specific
 * screen set. The Figma button instances didn't expose an inline fill color for capture (likely a
 * shared fill style rather than inline paint), so the exact bg is inferred from the consistent
 * white-on-dark button label pattern rather than a captured hex value.
 */
@Composable
fun FlowPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(PillShape)
            .background(if (enabled) PosColors.Blue500 else PosColors.Blue200)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PosColors.White)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                leadingIcon?.invoke()
                Text(text, style = PosTextStyles.bodyMediumSemibold, color = PosColors.White)
            }
        }
    }
}

@Composable
fun FlowSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        enabled = enabled,
        shape = PillShape,
        border = BorderStroke(1.dp, PosColors.Blue500),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = PosColors.Blue500),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
    ) {
        Text(text, style = PosTextStyles.bodyMediumSemibold, color = PosColors.Blue500)
    }
}

/** Rounded pill "input" look (order type / table / payment-method selectors — Figma's 48-54dp F8FAFB pill inputs). */
@Composable
fun PillSelector(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(PillShape)
            .background(PosColors.SurfaceAlt)
            .border(1.dp, PosColors.Border, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = PosTextStyles.bodyMediumRegular, color = PosColors.TextSecondary)
        trailing?.invoke()
    }
}

/** Category filter chip (Figma: 74dp tall, radius 16, #EFF4FF bg, icon circle + name/count). */
@Composable
fun CategoryChip(
    icon: String,
    name: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) PosColors.Blue500 else PosColors.Blue50)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(if (selected) PosColors.White else PosColors.Blue100),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, style = PosTextStyles.bodyMediumRegular)
        }
        androidx.compose.foundation.layout.Column {
            Text(name, style = PosTextStyles.bodySmallSemibold, color = if (selected) PosColors.White else PosColors.Neutral12)
            Text("$count Items", style = PosTextStyles.bodyXSmallRegular, color = if (selected) PosColors.Blue100 else PosColors.TextSecondary)
        }
    }
}

/** Pill-shaped quantity stepper (Figma: F6F8FA bg, radius 100, minus/plus circular buttons). */
@Composable
fun QuantityStepper(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(PosColors.Surface)
            .border(1.dp, PosColors.ImagePlaceholder, PillShape)
            .padding(horizontal = Spacing.xxxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        StepperButton(icon = Icons.Filled.Remove, onClick = onDecrement, enabled = quantity > 0)
        Text(
            text = quantity.toString(),
            style = PosTextStyles.bodyMediumSemibold,
            color = PosColors.Neutral12,
            modifier = Modifier.padding(horizontal = Spacing.xxxs),
        )
        StepperButton(icon = Icons.Filled.Add, onClick = onIncrement, enabled = true, filled = true)
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, enabled: Boolean, filled: Boolean = false) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (filled) PosColors.Blue500 else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) PosColors.White else PosColors.Blue500, modifier = Modifier.size(16.dp))
    }
}

/** Two/three-way segmented pill tabbar (order-type, payment-method — Figma: F6F8FA track, active pill blue). */
@Composable
fun <T> SegmentedTabBar(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(PillShape)
            .background(PosColors.Surface)
            .padding(4.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(PillShape)
                    .background(if (isSelected) PosColors.Blue500 else Color.Transparent)
                    .clickable { onSelected(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = PosTextStyles.bodySmallSemibold,
                    color = if (isSelected) PosColors.White else PosColors.Neutral12,
                )
            }
        }
    }
}
