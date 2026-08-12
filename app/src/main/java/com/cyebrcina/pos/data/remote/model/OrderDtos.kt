package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
enum class DeviceOrderType { DELIVERY, COLLECTION, DINE_IN }

@Serializable
data class DeviceOrderItem(
    val id: String,
    val productName: String,
    val sizeLabel: String? = null,
    val quantity: Int,
    val addOnNames: List<String> = emptyList(),
    val lineTotal: Double,
)

@Serializable
data class DeviceOrder(
    val id: String,
    val number: String,
    val type: DeviceOrderType,
    val typeLabel: String,
    val createdAt: String,
    val customerName: String,
    val customerPhone: String? = null,
    val deliveryAddress: String? = null,
    val tableLabel: String? = null,
    val estimatedTime: String? = null,
    val notes: String? = null,
    val items: List<DeviceOrderItem> = emptyList(),
    val itemsSubtotal: Double,
    val otherCharges: Double,
    val total: Double,
    /**
     * NOT YET LIVE server-side — see BACKEND_ORDER_CREATE_SPEC.md's "QR payment" section. Proposed
     * "PENDING" | "PAID" | "FAILED" field so the till can tell whether a QR-created order has
     * actually been paid yet. Null on every order type until the server implements this.
     */
    val paymentStatus: String? = null,
)

@Serializable
data class ReceiptPrefs(
    val showCustomerName: Boolean = true,
    val showPhone: Boolean = true,
    val showDeliveryAddress: Boolean = true,
    val showPaymentLabel: Boolean = true,
    val showPriceBreakdown: Boolean = true,
    val showThankYouFooter: Boolean = true,
    val headerFontSize: Int = 24,
    val orderBoxFontSize: Int = 16,
    val sectionTitleFontSize: Int = 14,
    val addressFontSize: Int = 12,
    val itemFontSize: Int = 14,
    val footerFontSize: Int = 12,
    /** "MM_58" or "MM_80" — parse via [com.cyebrcina.pos.printer.model.PrinterPaperSize]. */
    val paperSize: String? = null,
    /** "ESC" or "POS" — parse via [com.cyebrcina.pos.printer.model.PrintMode]. */
    val printMode: String? = null,
)

/** The D4's secondary customer-facing screen — background image behind the idle/order content,
 * its opacity (0-100), and an optional looping promo video shown instead of the idle branding
 * screen when there's nothing else to display. Set from Admin -> Settings -> Devices ->
 * Customer Display. */
@Serializable
data class CustomerDisplayConfig(
    val backgroundUrl: String? = null,
    val backgroundOpacity: Int = 100,
    val idlePromoVideoUrl: String? = null,
)

/** Which card terminal provider every till uses, set centrally in Admin -> Settings -> Card
 * Terminal instead of on the till itself. [provider] matches
 * [com.cyebrcina.pos.payment.model.PaymentProvider]'s name exactly. Only ever carries
 * client-safe credentials (a publishable/affiliate key, merchant/location id) — never a
 * server-only secret. */
@Serializable
data class CardTerminalConfig(
    val provider: String = "MOCK",
    val apiKey: String? = null,
    val merchantId: String? = null,
    val locationId: String? = null,
    val extra: String? = null,
    // Whether the till may offer "Record as Card (manual)" when the
    // selected provider's SDK isn't wired up yet — set centrally in
    // Admin's Card Terminal settings, defaults true there too.
    val manualFallbackAllowed: Boolean = true,
)

@Serializable
data class PendingOrdersResponse(
    val orders: List<DeviceOrder> = emptyList(),
    val storeName: String,
    val primaryColor: String,
    val logoUrl: String? = null,
    val receiptPrefs: ReceiptPrefs? = null,
    val notificationSoundUrl: String? = null,
    val customerDisplay: CustomerDisplayConfig? = null,
    val cardTerminal: CardTerminalConfig? = null,
)

@Serializable
data class OrderHistoryResponse(
    val orders: List<DeviceOrder> = emptyList(),
)

@Serializable
data class AcceptOrderResponse(
    val ok: Boolean = false,
    val printWarnings: List<String> = emptyList(),
)

@Serializable
data class RejectOrderResponse(
    val ok: Boolean = false,
)
