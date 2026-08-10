package com.cyebrcina.pos.payment

import com.cyebrcina.pos.payment.model.CardChargeResult
import com.cyebrcina.pos.payment.model.PaymentProvider
import com.cyebrcina.pos.payment.model.TerminalStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over a physical card-payment terminal (Stripe Terminal reader, SumUp Air/Solo,
 * Flatpay terminal, ...) so the Payment screen never touches a provider SDK directly. Exactly
 * one implementation is bound at a time via [com.cyebrcina.pos.di.PaymentModule] —
 * [com.cyebrcina.pos.payment.mock.MockPaymentTerminalService] by default, so the whole payment
 * flow is testable without any real hardware or merchant account. See CARD_PAYMENT_SETUP.md for
 * swapping in a real provider.
 */
interface PaymentTerminalService {
    val provider: PaymentProvider
    val status: StateFlow<TerminalStatus>

    /** Connects to / wakes the configured reader. Safe to call repeatedly. */
    suspend fun connect(): Result<Unit>

    /**
     * Starts a card charge for [amount] (major currency units, e.g. dollars) in [currencyCode]
     * (ISO 4217, e.g. "USD"). [reference] is the order number, passed through to the provider so
     * it shows up on their dashboard/receipt. Suspends until the customer's card is
     * presented/declined/approved — observe [status] for UI state (AWAITING_CARD, PROCESSING) in
     * the meantime.
     */
    suspend fun chargeCard(amount: Double, currencyCode: String, reference: String): Result<CardChargeResult>

    /** Cancels an in-flight [chargeCard] call, e.g. from a "Cancel" button while awaiting a card. */
    suspend fun cancelCharge()
}
