package com.cyebrcina.pos.customerdisplay

/**
 * What the D4's customer-facing secondary screen shows *right now*, on top of the store-wide
 * [CustomerDisplayBranding] (logo/background/promo video) that applies regardless of which of
 * these is active. Most Fire Hut orders arrive pre-built/paid from the website (no cart to
 * mirror), but the till order-taking flow
 * ([com.cyebrcina.pos.feature.order.create.NewOrderViewModel]) does build a cart on-device — this
 * package stays independent of that feature package (customer display is a lower-level module),
 * so [BuildingOrder] uses a plain [CustomerDisplayLineItem] rather than the feature's `CartItem`.
 */
sealed interface CustomerDisplayState {
    /** Nothing else to show — [CustomerDisplayScreen] plays the idle promo video here instead,
     * if [CustomerDisplayBranding.idlePromoVideoUrl] is set. */
    data object Idle : CustomerDisplayState

    data class NewOrderReceived(val orderNumber: String) : CustomerDisplayState

    data class BuildingOrder(
        val items: List<CustomerDisplayLineItem>,
        val total: Double,
    ) : CustomerDisplayState
}

data class CustomerDisplayLineItem(val name: String, val quantity: Int, val lineTotal: Double)
