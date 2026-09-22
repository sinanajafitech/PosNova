package com.cyebrcina.pos.feature

/** Known feature keys from Admin's Settings -> Feature Management (see the backend's
 * src/lib/features.ts FEATURE_CATALOG) — kept as plain string constants rather than an enum since
 * the source of truth for what a key means, and what its default is, lives server-side. */
object FeatureKeys {
    const val CALLS = "calls"
    const val QR_PAYMENT = "qrPayment"
    const val CUSTOMER_DISPLAY = "customerDisplay"
    const val INVENTORY_EIGHTY_SIX = "inventoryEightySix"
}

/** A key missing from the map (before the first poll lands, or an app build newer than the
 * backend it's talking to) is treated as enabled — matches the backend's own default-enabled
 * fallback in isFeatureEnabled(), so client and server never disagree about an unset key. */
fun Map<String, Boolean>.isFeatureEnabled(key: String): Boolean = this[key] ?: true
