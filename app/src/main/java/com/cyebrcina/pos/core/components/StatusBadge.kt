package com.cyebrcina.pos.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles

/** Semantic tone, one per color scale extracted from the kit's palette. */
enum class BadgeTone(val container: Color, val content: Color) {
    SUCCESS(PosColors.Success50, PosColors.Success500),
    WARNING(PosColors.Warning50, PosColors.Warning500),
    PENDING(PosColors.Pending50, PosColors.Pending500),
    INFO(PosColors.Info50, PosColors.Info500),
    NEUTRAL(PosColors.Neutral3, PosColors.Neutral9),
}

@Composable
fun StatusBadge(text: String, tone: BadgeTone, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = PosTextStyles.bodyXSmallSemibold,
        color = tone.content,
        modifier = modifier
            .background(tone.container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
