package com.cyebrcina.pos.payment

import com.cyebrcina.pos.data.local.TerminalSettingsStore
import com.cyebrcina.pos.payment.model.CardChargeResult
import com.cyebrcina.pos.payment.model.PaymentProvider
import com.cyebrcina.pos.payment.model.TerminalStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * The [PaymentTerminalService] every screen actually injects — delegates to whichever real
 * provider (SumUp/Flatpay/Dojo/Teya/Stripe Terminal/Mock) is currently selected in Settings
 * (persisted via [TerminalSettingsStore]), instead of one hardcoded implementation baked into
 * [com.cyebrcina.pos.di.PaymentModule] at build time. Switching providers takes effect live —
 * [status] re-subscribes to the newly-selected provider's own status flow immediately, and
 * [connect]/[chargeCard]/[cancelCharge] always resolve the current selection fresh.
 */
@Singleton
class SelectedPaymentTerminalService @Inject constructor(
    private val services: Map<PaymentProvider, @JvmSuppressWildcards PaymentTerminalService>,
    private val terminalSettingsStore: TerminalSettingsStore,
) : PaymentTerminalService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val selectedProvider: StateFlow<PaymentProvider> = terminalSettingsStore.selectedProvider
        .stateIn(scope, SharingStarted.Eagerly, PaymentProvider.MOCK)

    private fun serviceFor(provider: PaymentProvider): PaymentTerminalService =
        services[provider] ?: services.getValue(PaymentProvider.MOCK)

    private fun current(): PaymentTerminalService = serviceFor(selectedProvider.value)

    override val provider: PaymentProvider
        get() = current().provider

    override val status: StateFlow<TerminalStatus> = selectedProvider
        .flatMapLatest { serviceFor(it).status }
        .stateIn(scope, SharingStarted.Eagerly, TerminalStatus.DISCONNECTED)

    override suspend fun connect(): Result<Unit> = current().connect()

    override suspend fun chargeCard(amount: Double, currencyCode: String, reference: String): Result<CardChargeResult> =
        current().chargeCard(amount, currencyCode, reference)

    override suspend fun cancelCharge() = current().cancelCharge()
}
