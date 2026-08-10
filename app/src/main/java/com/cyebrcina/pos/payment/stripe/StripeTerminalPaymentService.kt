package com.cyebrcina.pos.payment.stripe

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
 * Card payments via Stripe's Terminal SDK (physical readers: WisePOS E, Stripe Reader S700, or
 * the D4's own NFC/contactless hardware once Stripe supports it as a "tap to pay" device).
 *
 * UNLIKE the Imin printer SDK, Stripe's Terminal SDK is public — `com.stripe:stripeterminal-core`
 * on Maven Central, no developer-portal gate. It was left out of `app/build.gradle.kts` (see the
 * commented dependency line) because using it correctly needs two things this environment
 * doesn't have: a Stripe account and a backend endpoint. Rather than half-wire a networking
 * client against a backend that doesn't exist yet, this class documents the real integration
 * shape and fails clearly until both pieces exist. Full steps in CARD_PAYMENT_SETUP.md.
 *
 * The real flow, once wired:
 * ```
 * // 1. ConnectionTokenProvider — Terminal SDK asks your backend for a short-lived token.
 * //    Your backend calls Stripe's server-side API (POST /v1/terminal/connection_tokens)
 * //    with your SECRET key; the secret key must never ship in this app.
 * class BackendConnectionTokenProvider(private val api: YourBackendApi) : ConnectionTokenProvider {
 *     override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
 *         // api.createStripeConnectionToken() -> callback.onSuccess(token) / onFailure(...)
 *     }
 * }
 *
 * // 2. One-time init (Application or first use):
 * Terminal.initTerminal(context, LogLevel.NONE, tokenProvider, terminalListener)
 *
 * // 3. Discover + connect a reader:
 * val config = DiscoveryConfiguration.BluetoothDiscoveryConfiguration(isSimulated = false)
 * Terminal.getInstance().discoverReaders(config, discoveryListener, discoveryCallback)
 * // ...then Terminal.getInstance().connectBluetoothReader(reader, connectionConfig, callback)
 *
 * // 4. Charge — the PaymentIntent itself must be created server-side (needs your secret key):
 * //    your backend calls Stripe's API to create a PaymentIntent for `amount`, returns the
 * //    client secret, then:
 * Terminal.getInstance().retrievePaymentIntent(clientSecret, retrieveCallback)
 * Terminal.getInstance().collectPaymentMethod(paymentIntent, collectCallback)
 * Terminal.getInstance().confirmPaymentIntent(paymentIntent, confirmCallback)
 * ```
 */
@Singleton
class StripeTerminalPaymentService @Inject constructor(
    @ApplicationContext private val context: Context,
) : PaymentTerminalService {

    override val provider: PaymentProvider = PaymentProvider.STRIPE_TERMINAL

    private val _status = MutableStateFlow(TerminalStatus.DISCONNECTED)
    override val status: StateFlow<TerminalStatus> = _status

    override suspend fun connect(): Result<Unit> {
        // TODO: Terminal.initTerminal(...) + discoverReaders(...) + connectBluetoothReader(...)
        // once app/build.gradle.kts has `com.stripe:stripeterminal-core` and a backend
        // ConnectionTokenProvider endpoint exists (see class doc + CARD_PAYMENT_SETUP.md).
        _status.value = TerminalStatus.ERROR
        return Result.failure(PaymentSdkNotConfiguredException("Stripe"))
    }

    override suspend fun chargeCard(amount: Double, currencyCode: String, reference: String): Result<CardChargeResult> {
        if (_status.value != TerminalStatus.READY) {
            val connected = connect()
            if (connected.isFailure) return Result.failure(connected.exceptionOrNull() ?: PaymentSdkNotConfiguredException("Stripe"))
        }
        // TODO: create a PaymentIntent server-side for `amount`/`currencyCode`, then
        // retrievePaymentIntent -> collectPaymentMethod -> confirmPaymentIntent as documented
        // in the class doc above.
        return Result.failure(PaymentSdkNotConfiguredException("Stripe"))
    }

    override suspend fun cancelCharge() {
        // TODO: Terminal.getInstance().cancelCollectPaymentMethod(cancelCallback) if a collect
        // is in flight.
    }
}
