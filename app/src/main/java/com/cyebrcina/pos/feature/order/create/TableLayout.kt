package com.cyebrcina.pos.feature.order.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * There's no floor-plan/table-management endpoint in `openapi.yaml` — `tableLabel` on a
 * [com.cyebrcina.pos.data.remote.model.DeviceOrder] is just a free-text string — so this is a
 * synthetic staff-facing table list (not backed by any per-table data from the server). Shared by
 * [ChooseTableDialog] and [TablesScreen] so both present the same table set.
 */
internal data class TableOption(val label: String, val seats: Int)

internal val syntheticTables = listOf(
    TableOption("1", 2), TableOption("2", 2), TableOption("3", 6), TableOption("4", 2),
    TableOption("5", 2), TableOption("6", 4), TableOption("7", 4), TableOption("8", 6),
    TableOption("9", 4), TableOption("10", 4), TableOption("11", 6), TableOption("12", 2),
)

internal enum class TableTileVisualState { AVAILABLE, SELECTED, HELD }

@Composable
internal fun TableTile(
    table: TableOption,
    state: TableTileVisualState,
    onClick: () -> Unit,
    badge: String? = null,
) {
    val (outerBg, chairColor, bodyColor, pillBg, pillText) = when (state) {
        TableTileVisualState.AVAILABLE -> TableColors(Color.Transparent, PosColors.Border, PosColors.Border, PosColors.Surface, PosColors.Neutral13)
        TableTileVisualState.SELECTED -> TableColors(PosColors.Blue50, PosColors.Blue100, PosColors.Blue100, PosColors.Blue500, PosColors.White)
        TableTileVisualState.HELD -> TableColors(PosColors.Pending50, PosColors.Pending200, PosColors.Pending200, PosColors.Pending500, PosColors.White)
    }
    val width = when (table.seats) {
        2 -> 130.dp
        4 -> 170.dp
        else -> 250.dp
    }

    Column(
        modifier = Modifier
            .width(width + 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(outerBg)
            .padding(8.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.width(width * 0.3f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(chairColor))
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier.width(width).height(120.dp).clip(RoundedCornerShape(20.dp)).background(bodyColor),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(pillBg).padding(horizontal = Spacing.xs, vertical = Spacing.xxxs),
            ) {
                Text("Table ${table.label}", style = PosTextStyles.bodySmallSemibold, color = pillText)
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(width * 0.3f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(chairColor))
        if (badge != null) {
            Spacer(Modifier.height(2.dp))
            Text(badge, style = PosTextStyles.bodyXSmallSemibold, color = PosColors.Pending500)
        }
    }
}

private data class TableColors(val outerBg: Color, val chairColor: Color, val bodyColor: Color, val pillBg: Color, val pillText: Color)

@Composable
internal fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxxs)) {
        Box(Modifier.height(10.dp).width(10.dp).clip(CircleShape).background(color))
        Text(label, style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral13)
    }
}
