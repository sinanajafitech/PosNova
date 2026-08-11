package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ReceiptItem(
    val label: String,
    val qty: Int,
    val lineTotal: Double,
    val addOns: List<String> = emptyList(),
    val removedIngredients: List<String> = emptyList(),
    val notes: String? = null,
)

@Serializable
data class ReceiptData(
    val number: String,
    val storeName: String,
    val createdAt: String,
    val typeLabel: String,
    val customerName: String,
    val customerPhone: String? = null,
    val deliveryAddress: String? = null,
    val items: List<ReceiptItem> = emptyList(),
    val itemsSubtotal: Double,
    val otherCharges: Double,
    val couponCode: String? = null,
    val discountAmount: Double = 0.0,
    val total: Double,
    val paymentLabel: String? = null,
    val prefs: ReceiptPrefs? = null,
)

@Serializable
data class KitchenTicketItem(
    val label: String,
    val qty: Int,
    val addOns: List<String> = emptyList(),
    val removedIngredients: List<String> = emptyList(),
    val notes: String? = null,
)

@Serializable
data class KitchenTicketPrefs(
    val showBranchName: Boolean = false,
    val showOrderType: Boolean = true,
    val showAddOns: Boolean = true,
    val showRemovedIngredients: Boolean = true,
    val showNotes: Boolean = true,
    /** "MM_58" or "MM_80" — parse via [com.cyebrcina.pos.printer.model.PrinterPaperSize]. */
    val paperSize: String? = null,
    /** "ESC" or "POS" — parse via [com.cyebrcina.pos.printer.model.PrintMode]. */
    val printMode: String? = null,
)

@Serializable
data class KitchenTicketData(
    val number: String,
    val storeName: String,
    val createdAt: String,
    val typeLabel: String,
    val branchName: String? = null,
    val deliveryAddress: String? = null,
    val items: List<KitchenTicketItem> = emptyList(),
    val prefs: KitchenTicketPrefs? = null,
)
