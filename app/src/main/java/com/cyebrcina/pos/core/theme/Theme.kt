package com.cyebrcina.pos.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Light scheme maps directly onto the extracted palette. The kit's file does contain a
 * "Dark Mode" section (node 4184:20104 / 12113:174751) but its screens weren't pulled node
 * by node for exact fill values in this pass, so this dark scheme is a reasonable derivation
 * from the same brand colors (Material3 dark-theme conventions: desaturated dark surfaces,
 * the same teal/semantic hues), not a 1:1 extraction. Worth revisiting against the Figma
 * Dark Mode section if pixel-exact dark styling matters.
 */
private val LightColors = lightColorScheme(
    primary = PosColors.Primary500,
    onPrimary = PosColors.White,
    primaryContainer = PosColors.Primary50,
    onPrimaryContainer = PosColors.Primary500,
    secondary = PosColors.Primary400,
    onSecondary = PosColors.White,
    secondaryContainer = PosColors.Primary100,
    onSecondaryContainer = PosColors.Primary500,
    tertiary = PosColors.Info500,
    onTertiary = PosColors.White,
    tertiaryContainer = PosColors.Info50,
    onTertiaryContainer = PosColors.Info500,
    background = PosColors.Neutral2,
    onBackground = PosColors.Neutral12,
    surface = PosColors.White,
    onSurface = PosColors.Neutral12,
    surfaceVariant = PosColors.Neutral3,
    onSurfaceVariant = PosColors.Neutral8,
    outline = PosColors.Neutral5,
    outlineVariant = PosColors.Neutral4,
    error = PosColors.Warning500,
    onError = PosColors.White,
    errorContainer = PosColors.Warning50,
    onErrorContainer = PosColors.Warning500,
)

private val DarkColors = darkColorScheme(
    primary = PosColors.Primary200,
    onPrimary = PosColors.Neutral12,
    primaryContainer = PosColors.Primary400,
    onPrimaryContainer = PosColors.Primary50,
    secondary = PosColors.Primary300,
    onSecondary = PosColors.Neutral12,
    secondaryContainer = PosColors.Primary400,
    onSecondaryContainer = PosColors.Primary50,
    tertiary = PosColors.Info300,
    onTertiary = PosColors.Neutral12,
    background = PosColors.Neutral12,
    onBackground = PosColors.Neutral3,
    surface = PosColors.Neutral11,
    onSurface = PosColors.Neutral3,
    surfaceVariant = PosColors.Neutral10,
    onSurfaceVariant = PosColors.Neutral6,
    outline = PosColors.Neutral8,
    outlineVariant = PosColors.Neutral9,
    error = PosColors.Warning300,
    onError = PosColors.Neutral12,
    errorContainer = PosColors.Warning400,
    onErrorContainer = PosColors.Warning50,
)

@Composable
fun PosNovaTheme(
    useDarkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        typography = PosNovaTypography,
        shapes = PosNovaShapes,
        content = content,
    )
}
