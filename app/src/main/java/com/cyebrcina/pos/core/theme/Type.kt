package com.cyebrcina.pos.core.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cyebrcina.pos.R

/**
 * Manrope, bundled locally (res/font) from the open-source variable font, instanced into
 * the static weights the design system actually uses.
 */
val ManropeFontFamily = FontFamily(
    Font(R.font.manrope_light, FontWeight.Light),
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

/**
 * Full type scale pulled from the "Design Style / Typography" Figma page (node 18:9983):
 * Heading H1–H6 and Body Large/Medium/Small/XSmall, each in Regular/Medium/Semibold.
 * Kept alongside (not just folded into) the Material3 [Typography] below, because screens
 * frequently need an exact kit style (e.g. "Body Medium Semibold") that doesn't map cleanly
 * onto Material3's 13 fixed slots.
 */
@Immutable
object PosTextStyles {
    private fun style(size: Int, weight: FontWeight, lineHeight: Int) = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
    )

    val h1 = style(48, FontWeight.SemiBold, 58)
    val h2 = style(40, FontWeight.SemiBold, 48)
    val h3 = style(32, FontWeight.SemiBold, 45)
    val h4 = style(24, FontWeight.SemiBold, 36)
    val h5 = style(20, FontWeight.SemiBold, 28)
    val h6 = style(18, FontWeight.SemiBold, 25)

    val bodyLargeSemibold = style(18, FontWeight.SemiBold, 28)
    val bodyLargeMedium = style(18, FontWeight.Medium, 28)
    val bodyLargeRegular = style(18, FontWeight.Normal, 28)

    val bodyMediumSemibold = style(16, FontWeight.SemiBold, 25)
    val bodyMediumMedium = style(16, FontWeight.Medium, 26)
    val bodyMediumRegular = style(16, FontWeight.Normal, 26)

    val bodySmallSemibold = style(14, FontWeight.SemiBold, 22)
    val bodySmallMedium = style(14, FontWeight.Medium, 22)
    val bodySmallRegular = style(14, FontWeight.Normal, 22)

    val bodyXSmallSemibold = style(12, FontWeight.SemiBold, 19)
    val bodyXSmallMedium = style(12, FontWeight.Medium, 19)
    val bodyXSmallRegular = style(12, FontWeight.Normal, 19)
}

val PosNovaTypography = Typography(
    headlineLarge = PosTextStyles.h1,
    headlineMedium = PosTextStyles.h2,
    headlineSmall = PosTextStyles.h3,
    titleLarge = PosTextStyles.h4,
    titleMedium = PosTextStyles.h5,
    titleSmall = PosTextStyles.h6,
    bodyLarge = PosTextStyles.bodyLargeMedium,
    bodyMedium = PosTextStyles.bodyMediumMedium,
    bodySmall = PosTextStyles.bodySmallMedium,
    labelLarge = PosTextStyles.bodySmallSemibold,
    labelMedium = PosTextStyles.bodyXSmallMedium,
    labelSmall = PosTextStyles.bodyXSmallRegular,
)
