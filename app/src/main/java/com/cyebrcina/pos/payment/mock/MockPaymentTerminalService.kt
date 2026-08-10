package com.cyebrcina.pos.payment.mock

import com.cyebrcina.pos.payment.PaymentTerminalService
import com.cyebrcina.pos.payment.model.CardChargeResult
import com.cyebrcina.pos.payment.model.CardDeclinedException
import com.cyebrcina.pos.payment.model.PaymentProvider
import com.cyebrcina.pos.payment.model.TerminalStatus
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * No-hardware terminal used by default: simulates a realistic present-card → processing →
 * approved timeline (~2.5s) so the Payment screen's states can be built and tested without a
 * real reader or merchant account. Declines roughly 1 in 12 charges so the decline/retry path
 * is exercisable too.
 */
@Singleton
class MockPaymentTerminalService @Inject constructor() : PaymentTerminalService {

    override val provider: PaymentProvider = PaymentProvider.MOCK

    private val _status = MutableStateFlow(TerminalStatus.DISCONNECTED)
    override val status: StateFlow<TerminalStatus> = _status

    override suspend fun connect(): Result<Unit> {
        _status.value = TerminalStatus.CONNECTING
        delay(300)
        _status.value = TerminalStatus.READY
        return Result.success(Unit)
    }

    override suspend fun chargeCard(amount: Double, currencyCode: String, reference: String): Result<CardChargeResult> {
        if (_status.value != TerminalStatus.READY) connect()
        return try {
            _status.value = TerminalStatus.AWAITING_CARD
            delay(1400)
            _status.value = TerminalStatus.PROCESSING
            delay(1100)

            if ((1..12).random() == 1) {
                _status.value = TerminalStatus.READY
                return Result.failure(CardDeclinedException("Card declined — please try another card"))
            }

            _status.value = TerminalStatus.READY
            Result.success(
                CardChargeResult(
                    transactionReference = "MOCK-${UUID.randomUUID().toString().take(8).uppercase()}",
                    cardBrand = listOf("Visa", "Mastercard", "Amex").random(),
                    cardLast4 = (1000..9999).random().toString(),
                ),
            )
        } catch (e: CancellationException) {
            _status.value = TerminalStatus.READY
            throw e
        }
    }

    override suspend fun cancelCharge() {
        _status.value = TerminalStatus.READY
    }
}
