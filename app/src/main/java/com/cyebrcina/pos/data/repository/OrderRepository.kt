package com.cyebrcina.pos.data.repository

import com.cyebrcina.pos.data.remote.model.CardTerminalConfig
import com.cyebrcina.pos.data.remote.model.CreateOrderRequest
import com.cyebrcina.pos.data.remote.model.CustomerDisplayConfig
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.data.remote.model.KitchenTicketData
import com.cyebrcina.pos.data.remote.model.PaymentLinkResponse
import com.cyebrcina.pos.data.remote.model.ReceiptData
import com.cyebrcina.pos.data.remote.model.ReceiptPrefs
import com.cyebrcina.pos.payment.model.CardChargeResult
import kotlinx.coroutines.flow.StateFlow

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

    /** Real, live — submits a till-built order. */
    suspend fun createOrder(request: CreateOrderRequest): Result<DeviceOrder>

    /** Real, live — creates a scan-to-pay QR for an order's outstanding balance. */
    suspend fun requestPaymentLink(orderId: String): Result<PaymentLinkResponse>
}
