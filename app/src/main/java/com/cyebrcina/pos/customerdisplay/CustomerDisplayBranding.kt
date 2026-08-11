package com.cyebrcina.pos.customerdisplay

/** Store-wide branding for the customer display, independent of whatever
 * per-order [CustomerDisplayState] is currently showing — combined from
 * [com.cyebrcina.pos.data.repository.AuthRepository] (storeName/logoUrl,
 * set at login) and [com.cyebrcina.pos.data.repository.OrderRepository]
 * (background/opacity/promo video, refreshed on the same 15s poll as
 * everything else) by [CustomerDisplayManager]. */
data class CustomerDisplayBranding(
    val storeName: String = "",
    val logoUrl: String? = null,
    val backgroundUrl: String? = null,
    val backgroundOpacity: Int = 100,
    val idlePromoVideoUrl: String? = null,
)
