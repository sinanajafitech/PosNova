package com.cyebrcina.pos.payment.teya

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
 * Card payments via Teya (formerly SaltPay; UK/EU terminal provider).
 *
 * **I don't have reliable knowledge of Teya's Android SDK** — same situation as
 * [com.cyebrcina.pos.payment.flatpay.FlatpayPaymentService] and
 * [com.cyebrcina.pos.payment.dojo.DojoPaymentService]: no verified reference for its package
 * names, class shapes, or auth flow. Rather than invent plausible-looking method calls that
 * would be actively misleading, this only implements the [PaymentTerminalService] contract with
 * a clear "not configured" failure, matching the same pattern as the other providers so it's a
 * drop-in once you have real integration details.
 *
 * Before implementing this for real:
 * 1. Get Teya's actual Android SDK/API docs from their merchant developer portal — confirm
 *    whether they offer a native Android SDK or a REST-API-based integration; confirm the
 *    current shape directly with them before assuming either model.
 * 2. If REST-API-based: this becomes closer to `data/repository` — a network client, not an SDK
 *    wrapper — and `chargeCard` would call your backend, which calls Teya's API.
 * 3. If a native SDK exists: follow the same shape as
 *    [com.cyebrcina.pos.payment.stripe.StripeTerminalPaymentService] (connect to reader, then
 *    charge) or [com.cyebrcina.pos.payment.sumup.SumUpPaymentService] (launch a checkout
 *    Activity/Intent) depending on how their SDK is structured.
 */
@Singleton
class TeyaPaymentService @Inject constructor(
    @ApplicationContext private val context: Context,
) : PaymentTerminalService {

    override val provider: PaymentProvider = PaymentProvider.TEYA

    private val _status = MutableStateFlow(TerminalStatus.DISCONNECTED)
    override val status: StateFlow<TerminalStatus> = _status

    override suspend fun connect(): Result<Unit> {
        _status.value = TerminalStatus.ERROR
        return Result.failure(PaymentSdkNotConfiguredException("Teya"))
    }

    override suspend fun chargeCard(amount: Double, currencyCode: String, reference: String): Result<CardChargeResult> {
        return Result.failure(PaymentSdkNotConfiguredException("Teya"))
    }

    override suspend fun cancelCharge() = Unit
}
