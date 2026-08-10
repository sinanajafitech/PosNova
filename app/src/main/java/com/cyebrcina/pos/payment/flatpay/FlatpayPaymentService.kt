package com.cyebrcina.pos.payment.flatpay

import android.content.Context
import com.cyebrcina.pos.payment.PaymentTerminalService
import com.cyebrcina.pos.payment.model.CardChargeResult
import com.cyebrcina.pos.payment.model.PaymentProvider
import com.cyebrcina.pos.payment.model.PaymentSdkNotConfiguredException
import com.cyebrcina.pos.payment.model.TerminalStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Card payments via Flatpay.
 *
 * **I don't have reliable knowledge of Flatpay's Android SDK** — it's a smaller Nordic/EU
 * provider and I don't have a verified reference for its package names, class shapes, or auth
 * flow the way I do for Stripe Terminal (public, well-documented) or SumUp (documented, if
 * version-inconsistent). Rather than invent plausible-looking method calls that would be actively
 * misleading, this class only implements the [PaymentTerminalService] contract with a clear
 * "not configured" failure, matching the same pattern as the other providers so it's a drop-in
 * once you have real integration details.
 *
 * Before implementing this for real:
 * 1. Get Flatpay's actual Android SDK/API docs from their merchant developer portal — confirm
 *    whether they offer a native Android SDK at all, or whether integration is REST-API-based
 *    (create a payment via their API, then poll/webhook for a terminal-side result) — POS card
 *    terminal providers do both depending on the region and reader hardware.
 * 2. If REST-API-based: this becomes closer to `data/repository` — a network client, not an SDK
 *    wrapper — and `chargeCard` would call your backend, which calls Flatpay's API.
 * 3. If a native SDK exists: follow the same shape as [com.cyebrcina.pos.payment.stripe.StripeTerminalPaymentService]
 *    (connect to reader, then charge) or [com.cyebrcina.pos.payment.sumup.SumUpPaymentService]
 *    (launch a checkout Activity/Intent) depending on how their SDK is structured.
 */
@Singleton
class FlatpayPaymentService @Inject constructor(
    @ApplicationContext private val context: Context,
) : PaymentTerminalService {

    override val provider: PaymentProvider = PaymentProvider.FLATPAY

    private val _status = MutableStateFlow(TerminalStatus.DISCONNECTED)
    override val status: StateFlow<TerminalStatus> = _status

    override suspend fun connect(): Result<Unit> {
        _status.value = TerminalStatus.ERROR
        return Result.failure(PaymentSdkNotConfiguredException("Flatpay"))
    }

    override suspend fun chargeCard(amount: Double, currencyCode: String, reference: String): Result<CardChargeResult> {
        return Result.failure(PaymentSdkNotConfiguredException("Flatpay"))
    }

    override suspend fun cancelCharge() = Unit
}
