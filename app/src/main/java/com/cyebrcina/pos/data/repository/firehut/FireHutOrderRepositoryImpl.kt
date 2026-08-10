package com.cyebrcina.pos.data.repository.firehut

import com.cyebrcina.pos.data.local.DeviceSessionStore
import com.cyebrcina.pos.data.remote.FireHutDeviceApi
import com.cyebrcina.pos.data.remote.errorMessageOrDefault
import com.cyebrcina.pos.data.remote.model.ChargeOrderRequest
import com.cyebrcina.pos.data.remote.model.CreateOrderRequest
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.data.remote.model.PaymentLinkResponse
import com.cyebrcina.pos.data.remote.model.KitchenTicketData
import com.cyebrcina.pos.data.remote.model.ReceiptData
import com.cyebrcina.pos.data.remote.model.ReceiptPrefs
import com.cyebrcina.pos.data.remote.realtime.FireHutRealtimeManager
import com.cyebrcina.pos.data.repository.OrderRepository
import com.cyebrcina.pos.payment.model.CardChargeResult
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val POLL_INTERVAL_MS = 15_000L

@Singleton
class FireHutOrderRepositoryImpl @Inject constructor(
    private val api: FireHutDeviceApi,
    private val sessionStore: DeviceSessionStore,
    private val realtimeManager: FireHutRealtimeManager,
    private val json: Json,
) : OrderRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    private val _pendingOrders = MutableStateFlow<List<DeviceOrder>>(emptyList())
    override val pendingOrders: StateFlow<List<DeviceOrder>> = _pendingOrders

    private val _receiptPrefs = MutableStateFlow<ReceiptPrefs?>(null)
    override val receiptPrefs: StateFlow<ReceiptPrefs?> = _receiptPrefs

    init {
        realtimeManager.orderEvents.onEach { refreshPending() }.launchIn(scope)
    }

    override fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (true) {
                refreshPending()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    override fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    override suspend fun refreshPending(): Result<Unit> = runCatching {
        val response = api.pendingOrders()
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        val body = response.body() ?: throw IllegalStateException("Empty response")
        _pendingOrders.value = body.orders
        body.receiptPrefs?.let { _receiptPrefs.value = it }
        sessionStore.updateBranding(body.storeName, body.primaryColor, body.logoUrl)
    }.recoverCatching { throw mapNetworkError(it) }

    override suspend fun history(limit: Int): Result<List<DeviceOrder>> = runCatching {
        val response = api.orderHistory(limit)
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body()?.orders ?: emptyList()
    }.recoverCatching { throw mapNetworkError(it) }

    override suspend fun accept(orderId: String): Result<Unit> = runCatching {
        val response = api.acceptOrder(orderId)
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        refreshPending()
        Unit
    }.recoverCatching { throw mapNetworkError(it) }

    override suspend fun reject(orderId: String): Result<Unit> = runCatching {
        val response = api.rejectOrder(orderId)
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        refreshPending()
        Unit
    }.recoverCatching { throw mapNetworkError(it) }

    override suspend fun receiptData(orderId: String): Result<ReceiptData> = runCatching {
        val response = api.receiptData(orderId)
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body() ?: throw IllegalStateException("Empty response")
    }.recoverCatching { throw mapNetworkError(it) }

    override suspend fun ticketData(orderId: String): Result<KitchenTicketData> = runCatching {
        val response = api.ticketData(orderId)
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body() ?: throw IllegalStateException("Empty response")
    }.recoverCatching { throw mapNetworkError(it) }

    override suspend fun chargeOrder(orderId: String, provider: String, amount: Double, chargeResult: CardChargeResult): Result<Unit> = runCatching {
        val response = api.chargeOrder(
            orderId,
            ChargeOrderRequest(
                provider = provider,
                amount = amount,
                cardBrand = chargeResult.cardBrand,
                cardLast4 = chargeResult.cardLast4,
                terminalReference = chargeResult.transactionReference,
            ),
        )
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        Unit
    }.recoverCatching { throw mapNetworkError(it) }

    override suspend fun createOrder(request: CreateOrderRequest): Result<DeviceOrder> = runCatching {
        val response = api.createOrder(request)
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        val order = response.body()?.order ?: throw IllegalStateException("Empty response")
        refreshPending()
        order
    }.recoverCatching { throw mapNetworkError(it) }

    override suspend fun requestPaymentLink(orderId: String): Result<PaymentLinkResponse> = runCatching {
        val response = api.paymentLink(orderId)
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body() ?: throw IllegalStateException("Empty response")
    }.recoverCatching { throw mapNetworkError(it) }

    private fun mapNetworkError(cause: Throwable): Throwable =
        if (cause is IOException) IOException("Couldn't reach the server — check your connection", cause) else cause
}
