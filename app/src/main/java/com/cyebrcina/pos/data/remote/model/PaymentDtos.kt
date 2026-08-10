package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

/**
 * NOT YET IMPLEMENTED SERVER-SIDE — see BACKEND_CARD_PAYMENT_SPEC.md at repo root. This DTO/call
 * exists so the Android client is ready the moment `POST /api/device/orders/{id}/charge` is
 * added to the real backend; until then, calling it will genuinely 404.
 */
@Serializable
data class ChargeOrderRequest(
    val provider: String,
    val amount: Double,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
    val terminalReference: String? = null,
)

@Serializable
data class ChargeOrderResponse(
    val ok: Boolean = false,
)

/** Real, live — `POST /api/device/orders/{id}/payment-link` (see openapi.yaml). */
@Serializable
data class PaymentLinkResponse(
    val url: String,
    /** "data:image/png;base64,..." — ready to display directly. */
    val qrCodeDataUrl: String,
)
