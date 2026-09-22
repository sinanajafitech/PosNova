package com.cyebrcina.pos.data.repository.firehut

import android.util.Log
import com.cyebrcina.pos.data.local.ConnectivityObserver
import com.cyebrcina.pos.data.local.DeviceSessionStore
import com.cyebrcina.pos.data.local.PendingOrderStore
import com.cyebrcina.pos.data.remote.FireHutDeviceApi
import com.cyebrcina.pos.data.remote.errorMessageOrDefault
import com.cyebrcina.pos.data.remote.model.CardTerminalConfig
import com.cyebrcina.pos.data.remote.model.ChargeOrderRequest
import com.cyebrcina.pos.data.remote.model.CreateOrderRequest
import com.cyebrcina.pos.data.remote.model.CustomerDisplayConfig
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.data.remote.model.PaymentLinkResponse
import com.cyebrcina.pos.data.remote.model.KitchenTicketData
import com.cyebrcina.pos.data.remote.model.ReceiptData
import com.cyebrcina.pos.data.remote.model.ReceiptPrefs
import com.cyebrcina.pos.data.remote.model.RepeatOrderResponse
import com.cyebrcina.pos.data.remote.realtime.FireHutRealtimeManager
import com.cyebrcina.pos.data.repository.CreateOrderResult
import com.cyebrcina.pos.data.repository.OrderRepository
import com.cyebrcina.pos.payment.model.CardChargeResult
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

private const val POLL_INTERVAL_MS = 15_000L
private const val MAX_FLUSH_ATTEMPTS = 10
private const val TAG = "FireHutOrderRepository"

@Singleton
class FireHutOrderRepositoryImpl @Inject constructor(
    private val api: FireHutDeviceApi,
    private val sessionStore: DeviceSessionStore,
    private val realtimeManager: FireHutRealtimeManager,
    private val connectivityObserver: ConnectivityObserver,
    private val pendingOrderStore: PendingOrderStore,
    private val json: Json,
) : OrderRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null
    private val flushMutex = Mutex()

    private val _pendingOrders = MutableStateFlow<List<DeviceOrder>>(emptyList())
    override val pendingOrders: StateFlow<List<DeviceOrder>> = _pendingOrders

    private val _receiptPrefs = MutableStateFlow<ReceiptPrefs?>(null)
    override val receiptPrefs: StateFlow<ReceiptPrefs?> = _receiptPrefs

    private val _customerDisplay = MutableStateFlow<CustomerDisplayConfig?>(null)
    override val customerDisplay: StateFlow<CustomerDisplayConfig?> = _customerDisplay

    private val _cardTerminal = MutableStateFlow<CardTerminalConfig?>(null)
    override val cardTerminal: StateFlow<CardTerminalConfig?> = _cardTerminal

    private val _features = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val features: StateFlow<Map<String, Boolean>> = _features

    private val _notificationSoundUrl = MutableStateFlow<String?>(null)
    override val notificationSoundUrl: StateFlow<String?> = _notificationSoundUrl

    override val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline

    override val pendingOrderCount: StateFlow<Int> = pendingOrderStore.pendingOrders
        .map { it.size }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    init {
        realtimeManager.orderEvents.onEach { refreshPending() }.launchIn(scope)

        // The fast local "network's back" signal — see ConnectivityObserver's doc comment for
        // why this is only a first attempt, not the only one (a real API call succeeding, via
        // the poll loop below, is the actual proof the backend itself is reachable again).
        connectivityObserver.isOnline
            .filter { it }
            .onEach { flushPendingOrders() }
            .launchIn(scope)
    }

    override fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (true) {
                // refreshPending()'s own runCatching only covers its own fetch — flushPendingOrders()
                // (called unconditionally after) and anything else in the cycle is NOT covered. Since
                // this coroutine is the only thing driving polling and nothing else ever calls
                // startPolling() again after the app's first launch, one uncaught exception here used
                // to kill this loop silently and permanently — the till would freeze on stale pending
                // orders indefinitely, with no error shown to staff, until the app was restarted. This
                // outer catch-all guarantees a single bad cycle can never do that; it just retries next
                // cycle instead.
                runCatching { refreshPending() }
                    .onFailure { Log.e(TAG, "Poll cycle failed — will retry in ${POLL_INTERVAL_MS}ms", it) }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    override fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    override suspend fun refreshPending(): Result<Unit> {
        val result = runCatching {
            val response = api.pendingOrders()
            if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
            val body = response.body() ?: throw IllegalStateException("Empty response")
            _pendingOrders.value = body.orders
            body.receiptPrefs?.let { _receiptPrefs.value = it }
            body.customerDisplay?.let { _customerDisplay.value = it }
            body.cardTerminal?.let { _cardTerminal.value = it }
            _features.value = body.features
            _notificationSoundUrl.value = body.notificationSoundUrl
            sessionStore.updateBranding(body.storeName, body.primaryColor, body.logoUrl)
        }.recoverCatching { throw mapNetworkError(it) }
        // A successful poll IS proof the backend is reachable — piggyback the queue flush on it
        // rather than running a second, separate reachability check.
        if (result.isSuccess) flushPendingOrders()
        return result
    }

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

    override suspend fun createOrder(request: CreateOrderRequest): Result<CreateOrderResult> = runCatching {
        try {
            val response = api.createOrder(request)
            if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
            val order = response.body()?.order ?: throw IllegalStateException("Empty response")
            refreshPending()
            CreateOrderResult.Submitted(order)
        } catch (e: IOException) {
            // A real, non-connectivity rejection (sold out, bad table, etc.) throws
            // IllegalStateException above and skips this branch entirely — only a genuine "can't
            // reach the server" failure gets queued. A QR order is never queued (see
            // CreateOrderResult's doc) — there's nothing useful to show the customer without a
            // server-issued Stripe Checkout link, so it's still a real failure to surface.
            if (request.payment.method == "QR") throw e
            val localId = pendingOrderStore.enqueue(request, Instant.now().toString())
            CreateOrderResult.Queued(localId)
        }
    }.recoverCatching { throw mapNetworkError(it) }

    override suspend fun requestPaymentLink(orderId: String): Result<PaymentLinkResponse> = runCatching {
        val response = api.paymentLink(orderId)
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body() ?: throw IllegalStateException("Empty response")
    }.recoverCatching { throw mapNetworkError(it) }

    override suspend fun repeatOrder(orderId: String): Result<RepeatOrderResponse> = runCatching {
        val response = api.repeatOrder(orderId)
        if (!response.isSuccessful) throw IllegalStateException(response.errorMessageOrDefault(json))
        response.body() ?: throw IllegalStateException("Empty response")
    }.recoverCatching { throw mapNetworkError(it) }

    /**
     * Submits every locally-queued order in FIFO order (the order staff actually took them in),
     * stopping at the first failure — if the connection drops again mid-flush, hammering the
     * rest right now gains nothing; the next successful poll or reconnect picks up from there.
     * Guarded by [flushMutex] since this can be triggered by both the connectivity callback and
     * every successful poll, which can easily overlap.
     *
     * Note: a flushed order's `createdAt` is stamped by the server at submission time, not when
     * it was actually taken — Admin's reports will show it later than it really happened. Fixing
     * that would mean the backend accepting and trusting a client-supplied timestamp, out of
     * scope for this client-side queue.
     */
    private suspend fun flushPendingOrders() {
        if (flushMutex.isLocked) return
        flushMutex.withLock {
            for (pending in pendingOrderStore.currentList()) {
                if (pending.attempts >= MAX_FLUSH_ATTEMPTS) continue
                val response = runCatching { api.createOrder(pending.request) }.getOrNull()
                when {
                    response != null && response.isSuccessful && response.body()?.order != null ->
                        pendingOrderStore.remove(pending.localId)
                    response != null -> {
                        // A real HTTP-level rejection, not a connectivity problem (e.g. a menu
                        // item on this order was deleted server-side while it sat queued) —
                        // retrying the identical payload won't succeed, so skip it instead of
                        // blocking every order queued behind it. It stays in the queue, capped by
                        // MAX_FLUSH_ATTEMPTS, so staff can still see it via the pending count.
                        pendingOrderStore.bumpAttempts(pending.localId)
                    }
                    else -> {
                        // No HTTP response at all — a genuine connectivity failure. Every order
                        // still queued behind this one shares the same "can't reach the server"
                        // problem, so stop here; the next successful poll or reconnect resumes.
                        pendingOrderStore.bumpAttempts(pending.localId)
                        break
                    }
                }
            }
        }
    }

    private fun mapNetworkError(cause: Throwable): Throwable =
        if (cause is IOException) IOException("Couldn't reach the server — check your connection", cause) else cause
}
