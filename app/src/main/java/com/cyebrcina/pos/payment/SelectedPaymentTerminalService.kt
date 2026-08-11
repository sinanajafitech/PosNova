package com.cyebrcina.pos.payment

import com.cyebrcina.pos.data.repository.OrderRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The [PaymentTerminalService] every screen actually injects — delegates to whichever real
 * provider (SumUp/Flatpay/Dojo/Teya/Stripe Terminal/Mock) Admin has selected in
 * Settings -> Card Terminal ([OrderRepository.cardTerminal], refreshed on the same 15s poll as
 * everything else), instead of a till-local choice. Deliberately **not** something the till can
 * change on its own — an earlier on-device picker meant browsing Settings could silently switch
 * the active provider to an unconfigured one mid-shift; Admin is now the only source of truth.
 * [status] re-subscribes to the newly-selected provider's own status flow whenever Admin's
 * choice changes; [connect]/[chargeCard]/[cancelCharge] always resolve the current selection
 * fresh.
 */
@Singleton
class SelectedPaymentTerminalService @Inject constructor(
    private val services: Map<PaymentProvider, @JvmSuppressWildcards PaymentTerminalService>,
    private val orderRepository: OrderRepository,
) : PaymentTerminalService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val selectedProvider: StateFlow<PaymentProvider> = orderRepository.cardTerminal
        .map { config -> PaymentProvider.entries.find { it.name == config?.provider } ?: PaymentProvider.MOCK }
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
