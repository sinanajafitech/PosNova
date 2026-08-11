package com.cyebrcina.pos.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import kotlin.math.abs

/**
 * Matches the stat-card shape used on Figma's Dashboard and Report screens (node 12111:90374 /
 * 12111:94291): icon badge + title, big value, divider, trend chip. Shared between
 * [com.cyebrcina.pos.feature.order.list.OrderQueueScreen] and
 * [com.cyebrcina.pos.feature.report.ReportScreen] so the two screens' stat rows stay visually
 * identical rather than drifting into two hand-rolled copies.
 */
@Composable
fun StatCard(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    value: String,
    trend: Double?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    positiveIsBad: Boolean = false,
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(PosColors.White).padding(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = PosColors.White, modifier = Modifier.size(18.dp))
            }
            Text(title, style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral12)
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.xxxs)) {
            Text(value, style = PosTextStyles.h4, color = PosColors.Neutral13)
            subtitle?.let { Text(it, style = PosTextStyles.bodySmallRegular, color = PosColors.TextSecondary) }
        }
        Spacer(Modifier.height(Spacing.xs))
        HorizontalDivider(color = PosColors.Border)
        Spacer(Modifier.height(Spacing.xs))
        if (trend != null) {
            val isGood = if (positiveIsBad) trend <= 0 else trend >= 0
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (isGood) PosColors.TrendPositiveBg else PosColors.TrendNegativeBg)
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xxxs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    if (trend >= 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (isGood) PosColors.Success500 else PosColors.Danger,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    "${"%.1f".format(abs(trend))}% vs yesterday",
                    style = PosTextStyles.bodyXSmallSemibold,
                    color = if (isGood) PosColors.Success500 else PosColors.Danger,
                )
            }
        } else {
            Text("No data for yesterday", style = PosTextStyles.bodyXSmallRegular, color = PosColors.TextSecondary)
        }
    }
}
