package com.cyebrcina.pos.payment.sumup

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
 * Card payments via a SumUp Air/Solo card reader paired to the D4 over Bluetooth, using SumUp's
 * checkout SDK.
 *
 * **Lower confidence than the Stripe integration doc**: SumUp has changed their Android SDK's
 * public API across major versions (the older static `SumUpAPI`/`SumUpState` classes vs newer
 * builder-based login/checkout flows), and I don't have a verified-current reference for it —
 * confirm exact class/method names against SumUp's current developer docs for the SDK version
 * you actually pull in, rather than trusting the sketch below verbatim. What IS solid: the
 * overall shape (merchant login once via SumUp's own UI, then launch their checkout Activity per
 * charge, read the result back via [SumUpResultBridge]) — that part matches how SumUp's SDK has
 * worked for years.
 *
 * ```
 * // 1. One-time SDK init + merchant login (SumUp handles their own UI for this):
 * SumUpState.init(context)  // needs your SumUp App ID from their developer dashboard
 * val login = SumUpLogin.builder(affiliateKey).build()
 * SumUpAPI.openLoginActivity(activity, login, LOGIN_REQUEST_CODE)
 *
 * // 2. Per-charge checkout — build a payment request and launch SumUp's checkout Activity:
 * val payment = SumUpPayment.builder()
 *     .total(BigDecimal.valueOf(amount))
 *     .currency(SumUpPayment.Currency.valueOf(currencyCode))
 *     .title(reference)
 *     .build()
 * val intent = SumUpAPI.checkoutIntent(activity, payment)  // exact factory method name may differ
 * val (resultCode, data) = sumUpResultBridge.launchForResult(intent)
 * // 3. Parse `data`'s extras for the transaction code / status — field names vary by SDK version.
 * ```
 */
@Singleton
class SumUpPaymentService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resultBridge: SumUpResultBridge,
) : PaymentTerminalService {

    override val provider: PaymentProvider = PaymentProvider.SUMUP

    private val _status = MutableStateFlow(TerminalStatus.DISCONNECTED)
    override val status: StateFlow<TerminalStatus> = _status

    override suspend fun connect(): Result<Unit> {
        // TODO: SumUpState.init(context) + confirm merchant is logged in (SumUpAPI.isLoggedIn()
        // or equivalent for your SDK version); if not, launch the login Activity via
        // resultBridge.launchForResult(...) before returning success.
        _status.value = TerminalStatus.ERROR
        return Result.failure(PaymentSdkNotConfiguredException("SumUp"))
    }

    override suspend fun chargeCard(amount: Double, currencyCode: String, reference: String): Result<CardChargeResult> {
        if (_status.value != TerminalStatus.READY) {
            val connected = connect()
            if (connected.isFailure) return Result.failure(connected.exceptionOrNull() ?: PaymentSdkNotConfiguredException("SumUp"))
        }
        // TODO: build the SumUpPayment request and launch it via resultBridge.launchForResult(...)
        // as documented in the class doc above, then parse the result Intent's extras.
        return Result.failure(PaymentSdkNotConfiguredException("SumUp"))
    }

    override suspend fun cancelCharge() {
        // SumUp's checkout Activity handles its own cancel UI once launched; nothing to cancel
        // programmatically before that point.
    }
}
