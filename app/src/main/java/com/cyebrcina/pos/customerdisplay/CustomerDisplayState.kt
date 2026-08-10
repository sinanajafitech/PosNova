package com.cyebrcina.pos.customerdisplay

/**
 * What the D4's customer-facing secondary screen shows. Most Fire Hut orders arrive pre-built/paid
 * from the website (no cart to mirror), but the till order-taking flow
 * ([com.cyebrcina.pos.feature.order.create.NewOrderViewModel]) does build a cart on-device — this
 * package stays independent of that feature package (customer display is a lower-level module),
 * so [BuildingOrder] uses a plain [CustomerDisplayLineItem] rather than the feature's `CartItem`.
 */
sealed interface CustomerDisplayState {
    data class Idle(val storeName: String, val logoUrl: String?) : CustomerDisplayState

    data class NewOrderReceived(val orderNumber: String, val storeName: String, val logoUrl: String?) : CustomerDisplayState

    data class BuildingOrder(
        val storeName: String,
        val logoUrl: String?,
        val items: List<CustomerDisplayLineItem>,
        val total: Double,
    ) : CustomerDisplayState
}

data class CustomerDisplayLineItem(val name: String, val quantity: Int, val lineTotal: Double)
