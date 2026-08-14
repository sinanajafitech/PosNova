package com.cyebrcina.pos.data.repository

import com.cyebrcina.pos.data.remote.model.CardTerminalConfig
import com.cyebrcina.pos.data.remote.model.CreateOrderRequest
import com.cyebrcina.pos.data.remote.model.CustomerDisplayConfig
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.data.remote.model.KitchenTicketData
import com.cyebrcina.pos.data.remote.model.PaymentLinkResponse
import com.cyebrcina.pos.data.remote.model.ReceiptData
import com.cyebrcina.pos.data.remote.model.ReceiptPrefs
import com.cyebrcina.pos.data.remote.model.RepeatOrderResponse
import com.cyebrcina.pos.payment.model.CardChargeResult
import kotlinx.coroutines.flow.StateFlow

/** Outcome of [OrderRepository.createOrder] — either it reached the server ([Submitted], the
 * only outcome possible before offline support existed), or there was no connection and it's
 * been saved locally to retry automatically once one returns ([Queued] — see
 * [com.cyebrcina.pos.data.local.PendingOrderStore]). Never [Queued] for a QR payment, which has
 * no offline story (it needs a server-issued Stripe Checkout link to show the customer). */
sealed interface CreateOrderResult {
    data class Submitted(val order: DeviceOrder) : CreateOrderResult
    data class Queued(val localId: String) : CreateOrderResult
}

/**
 * Orders arrive pre-built/pre-priced from Fire Hut's ordering website — this repository never
 * builds a menu/cart, it only surfaces what the device orders endpoints already resolved server-side.
 * [pendingOrders] is kept fresh by 15s polling (started via [startPolling]) plus an immediate
 * refresh whenever [com.cyebrcina.pos.data.remote.realtime.FireHutRealtimeManager] sees a
 * `new_order`/`order_status_updated`/`order_payment_updated` socket event.
 */
interface OrderRepository {
    val pendingOrders: StateFlow<List<DeviceOrder>>
    val receiptPrefs: StateFlow<ReceiptPrefs?>
    val customerDisplay: StateFlow<CustomerDisplayConfig?>
    val cardTerminal: StateFlow<CardTerminalConfig?>

    /** See [com.cyebrcina.pos.data.local.ConnectivityObserver] — a fast, local "does the device
     * have a network path" signal, not proof the backend itself is reachable. */
    val isOnline: StateFlow<Boolean>

    /** How many orders are saved locally waiting to be submitted — see [CreateOrderResult.Queued]. */
    val pendingOrderCount: StateFlow<Int>

    fun startPolling()
    fun stopPolling()
    suspend fun refreshPending(): Result<Unit>
    suspend fun history(limit: Int = 50): Result<List<DeviceOrder>>
    suspend fun accept(orderId: String): Result<Unit>
    suspend fun reject(orderId: String): Result<Unit>
    suspend fun receiptData(orderId: String): Result<ReceiptData>
    suspend fun ticketData(orderId: String): Result<KitchenTicketData>

    /** NOT YET LIVE server-side — see BACKEND_CARD_PAYMENT_SPEC.md. Will fail (404) until then. */
    suspend fun chargeOrder(orderId: String, provider: String, amount: Double, chargeResult: CardChargeResult): Result<Unit>

    /** Real, live — submits a till-built order. Queues it locally instead of failing if there's
     * no connection (except a QR-payment order — see [CreateOrderResult]). */
    suspend fun createOrder(request: CreateOrderRequest): Result<CreateOrderResult>

    /** Real, live — creates a scan-to-pay QR for an order's outstanding balance. */
    suspend fun requestPaymentLink(orderId: String): Result<PaymentLinkResponse>

    /** Real, live — a past order's line items (with real productId/sizeId/addOnIds), for the
     * Incoming Call popup's "Repeat Last Order" action. */
    suspend fun repeatOrder(orderId: String): Result<RepeatOrderResponse>
}
