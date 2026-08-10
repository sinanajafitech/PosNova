package com.cyebrcina.pos.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * Border-radius scale from the "Design Style / Spacing, Borders, Shadows" Figma page
 * (node 18:10484): xs=4, sm=8, md=10, lg=12, xl=16, 2xl=20, 3xl=24, 4xl=30.
 */
@Immutable
object PosRadius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 10.dp
    val lg = 12.dp
    val xl = 16.dp
    val xxl = 20.dp
    val xxxl = 24.dp
    val xxxxl = 30.dp
}

val PosNovaShapes = Shapes(
    extraSmall = RoundedCornerShape(PosRadius.xs),
    small = RoundedCornerShape(PosRadius.sm),
    medium = RoundedCornerShape(PosRadius.lg),
    large = RoundedCornerShape(PosRadius.xl),
    extraLarge = RoundedCornerShape(PosRadius.xxxl),
)
