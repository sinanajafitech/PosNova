package com.cyebrcina.pos.payment.model

/** Which card-payment integration is active — selectable in Settings, persisted in
 * [com.cyebrcina.pos.data.local.TerminalSettingsStore], and resolved at runtime by
 * [com.cyebrcina.pos.payment.SelectedPaymentTerminalService] via the
 * `Map<PaymentProvider, PaymentTerminalService>` bound in [com.cyebrcina.pos.di.PaymentModule]. */
enum class PaymentProvider {
    MOCK,
    STRIPE_TERMINAL,
    SUMUP,
    FLATPAY,
    DOJO,
    TEYA,
}

enum class TerminalStatus {
    DISCONNECTED,
    CONNECTING,
    READY,
    AWAITING_CARD,
    PROCESSING,
    ERROR,
}

data class CardChargeResult(
    val transactionReference: String,
    val cardBrand: String? = null,
    val cardLast4: String? = null,
)

/** Thrown by a charge/connect call the customer can act on (declined, cancelled, card removed too early). */
class CardDeclinedException(message: String) : Exception(message)

/** Thrown by every method of a provider service until its real SDK is integrated (CARD_PAYMENT_SETUP.md). */
class PaymentSdkNotConfiguredException(providerName: String) :
    IllegalStateException("$providerName is not configured yet — see CARD_PAYMENT_SETUP.md.")
