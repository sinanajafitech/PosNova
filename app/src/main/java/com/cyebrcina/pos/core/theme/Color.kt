package com.cyebrcina.pos.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette pulled directly from the POSNova Figma file's "Design Style / Colors" page
 * (file 7BKd90Y61sRQxvHiPnfAhr, node 18:9940) via the Figma REST API.
 */
object PosColors {

    // Neutral-DPOP
    val Neutral2 = Color(0xFFFCFCFC)
    val Neutral3 = Color(0xFFF5F5F5)
    val Neutral4 = Color(0xFFF0F0F0)
    val Neutral5 = Color(0xFFD9D9D9)
    val Neutral6 = Color(0xFFBFBFBF)
    val Neutral7 = Color(0xFF8C8C8C)
    val Neutral8 = Color(0xFF595959)
    val Neutral9 = Color(0xFF454545)
    val Neutral10 = Color(0xFF262626)
    val Neutral11 = Color(0xFF1F1F1F)
    val Neutral12 = Color(0xFF141414)
    val Neutral13 = Color(0xFF000000)

    // Primary-DPOP (also mirrored 1:1 as Secondary-DPOP in the source file)
    val Primary50 = Color(0xFFE6EEEF)
    val Primary100 = Color(0xFFB0CBCC)
    val Primary200 = Color(0xFF8AB1B4)
    val Primary300 = Color(0xFF558E91)
    val Primary400 = Color(0xFF34787C)
    val Primary500 = Color(0xFF01565B)

    // Success-DPOP
    val Success50 = Color(0xFFEEF9E8)
    val Success100 = Color(0xFFCAECB8)
    val Success200 = Color(0xFFB0E396)
    val Success300 = Color(0xFF8CD766)
    val Success400 = Color(0xFF75CF49)
    val Success500 = Color(0xFF53C31B)

    // Warning-DPOP.
    // NOTE: the source file's "50" swatch has a malformed hex token ("#faded", only 5 hex
    // digits — a typo in the original design file, confirmed against the raw Figma node
    // data). Substituted with a pale tint consistent with how every other scale's 50-step
    // relates to its 100-step (a near-white tint of the same hue).
    val Warning50 = Color(0xFFFFF0F0)
    val Warning100 = Color(0xFFFFC8C8)
    val Warning200 = Color(0xFFFFADAE)
    val Warning300 = Color(0xFFFE8889)
    val Warning400 = Color(0xFFFE7172)
    val Warning500 = Color(0xFFFE4D4F)

    // Pending-DPOP
    val Pending50 = Color(0xFFFFF7E8)
    val Pending100 = Color(0xFFFDE6B6)
    val Pending200 = Color(0xFFFDD993)
    val Pending300 = Color(0xFFFCC862)
    val Pending400 = Color(0xFFFBBD43)
    val Pending500 = Color(0xFFFAAD14)

    // Info-DPOP
    val Info50 = Color(0xFFE8F4FF)
    val Info100 = Color(0xFFB8DDFF)
    val Info200 = Color(0xFF95CCFF)
    val Info300 = Color(0xFF65B5FF)
    val Info400 = Color(0xFF47A6FF)
    val Info500 = Color(0xFF1990FF)

    val White = Color(0xFFFFFFFF)

    // Blue-DPOP — a second accent scale, distinct from Primary/Info, used throughout the
    // New Order / Choose Table / Payment Order screens (product pricing, selection states,
    // active pills). Originally missed in the first extraction pass (it showed up as raw
    // unnamed rectangle fills, not under a named token group) and added once the order-taking
    // screens' node data made the recurring 6-step scale unambiguous.
    val Blue50 = Color(0xFFEFF4FF)
    val Blue100 = Color(0xFFD5E3FD)
    val Blue200 = Color(0xFFABC6FC)
    val Blue300 = Color(0xFF81AAFA)
    val Blue400 = Color(0xFF578DF9)
    val Blue500 = Color(0xFF2D71F7)

    // Surface/border tones used directly as fills in the New Order / Choose Table / Payment
    // Order screens (not part of the named "Design Style" token scale, but recur consistently
    // across those screens' node fills).
    val Surface = Color(0xFFF6F8FA)
    val SurfaceAlt = Color(0xFFF8FAFB)
    val ImagePlaceholder = Color(0xFFECEFF3)
    val Border = Color(0xFFDFE1E7)
    val TextSecondary = Color(0xFF6F6F6F)
    val Danger = Color(0xFFDF1C41)
    val SuccessAccent = Color(0xFF40C4AA)

    // Report screen stat-card icon accents + trend-chip tones (node 12111:94291).
    val Amber = Color(0xFFFFBE4C)
    val Cyan = Color(0xFF33CFFF)
    val TrendPositiveBg = Color(0xFFEFFEFA)
    val TrendNegativeBg = Color(0xFFFFF0F3)
}
