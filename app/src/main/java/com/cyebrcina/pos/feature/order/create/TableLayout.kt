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
 * [ChooseTableDialog] (picking a table mid-order) still uses this synthetic set — `tableLabel` on
 * a [com.cyebrcina.pos.data.remote.model.DeviceOrder] is just a free-text string with no per-table
 * identity, so there's nothing real to pick from there. [TablesScreen] (the standalone Tables tab)
 * uses real data instead — see [com.cyebrcina.pos.data.repository.TableRepository] — but shares
 * this same [TableTile] rendering.
 */
internal data class TableOption(val label: String, val seats: Int, val id: String? = null)

internal val syntheticTables = listOf(
    TableOption("1", 2), TableOption("2", 2), TableOption("3", 6), TableOption("4", 2),
    TableOption("5", 2), TableOption("6", 4), TableOption("7", 4), TableOption("8", 6),
    TableOption("9", 4), TableOption("10", 4), TableOption("11", 6), TableOption("12", 2),
)

/** AVAILABLE/RESERVED/CLEANING/OUT_OF_SERVICE/OCCUPIED mirror Admin's real
 * RestaurantTableStatus (plus the derived OCCUPIED) — same color language as
 * the dine-in floor-plan page in Admin (green/red/gray/near-black) so
 * status means the same thing in both places. SELECTED and HELD stay
 * local-only concepts: SELECTED is [ChooseTableDialog]'s in-progress pick,
 * HELD is a locally-parked new-order draft, neither of which Admin knows
 * about. */
internal enum class TableTileVisualState { AVAILABLE, SELECTED, HELD, RESERVED, CLEANING, OUT_OF_SERVICE, OCCUPIED }

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
        TableTileVisualState.OCCUPIED -> TableColors(PosColors.Warning50, PosColors.Warning200, PosColors.Warning200, PosColors.Danger, PosColors.White)
        TableTileVisualState.RESERVED -> TableColors(PosColors.Info50, PosColors.Info200, PosColors.Info200, PosColors.Info500, PosColors.White)
        TableTileVisualState.CLEANING -> TableColors(PosColors.Neutral3, PosColors.Neutral5, PosColors.Neutral5, PosColors.Neutral7, PosColors.White)
        TableTileVisualState.OUT_OF_SERVICE -> TableColors(PosColors.Neutral4, PosColors.Neutral9, PosColors.Neutral9, PosColors.Neutral9, PosColors.White)
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
