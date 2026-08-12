package com.cyebrcina.pos.data.remote.realtime

import android.util.Log
import com.cyebrcina.pos.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

/** A dine-in diner tapped "Call Waiter" on the menu — see Admin's
 * POST /api/orders/waiter-call, which emits this. Unlike the order events
 * below, there's no REST endpoint to re-poll for "current waiter calls",
 * so the payload itself has to carry what the popup needs rather than
 * just being a refresh signal. */
data class WaiterCallEvent(val tableId: String, val tableNumber: String, val calledAt: String)

/** A call came in on the restaurant's phone line — see Admin's
 * POST /api/integrations/phone/incoming-call (the bOnline webhook
 * receiver), which resolves the caller against Customer/PhoneContact and
 * emits this. Like [WaiterCallEvent], the payload carries what the popup
 * needs directly since there's no REST endpoint to re-poll. */
data class IncomingCallEvent(
    val phone: String,
    val callerName: String?,
    val address: String?,
    val isKnown: Boolean,
    val receivedAt: String,
)

/**
 * Socket.IO connection to Fire Hut's backend (see openapi.yaml's `x-realtime` section). This is
 * the "instant pop-up" channel — [FireHutOrderRepository][com.cyebrcina.pos.data.repository.firehut.FireHutOrderRepositoryImpl]
 * still owns polling as the source of truth; this just triggers an immediate re-poll on
 * `new_order`/`order_status_updated`/`order_payment_updated` rather than trying to reconcile the
 * broadcast payload's smaller shape against the full `DeviceOrder` shape.
 */
@Singleton
class FireHutRealtimeManager @Inject constructor() {

    private var socket: Socket? = null

    private val _orderEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val orderEvents: SharedFlow<Unit> = _orderEvents.asSharedFlow()

    private val _storeStatusEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val storeStatusEvents: SharedFlow<Unit> = _storeStatusEvents.asSharedFlow()

    private val _waiterCallEvents = MutableSharedFlow<WaiterCallEvent>(extraBufferCapacity = 8)
    val waiterCallEvents: SharedFlow<WaiterCallEvent> = _waiterCallEvents.asSharedFlow()

    private val _incomingCallEvents = MutableSharedFlow<IncomingCallEvent>(extraBufferCapacity = 8)
    val incomingCallEvents: SharedFlow<IncomingCallEvent> = _incomingCallEvents.asSharedFlow()

    fun connect(token: String) {
        disconnect()
        runCatching {
            val options = IO.Options().apply {
                extraHeaders = mapOf("Authorization" to listOf("Bearer $token"))
                reconnection = true
            }
            val newSocket = IO.socket(BuildConfig.DEVICE_API_BASE_URL, options)
            newSocket.on("new_order") { _orderEvents.tryEmit(Unit) }
            newSocket.on("order_status_updated") { _orderEvents.tryEmit(Unit) }
            newSocket.on("order_payment_updated") { _orderEvents.tryEmit(Unit) }
            newSocket.on("store_status_updated") { _storeStatusEvents.tryEmit(Unit) }
            newSocket.on("waiter_called") { args ->
                val payload = args.firstOrNull() as? JSONObject
                val tableId = payload?.optString("tableId")?.takeIf { it.isNotBlank() }
                val tableNumber = payload?.optString("tableNumber")?.takeIf { it.isNotBlank() }
                if (tableId != null && tableNumber != null) {
                    _waiterCallEvents.tryEmit(WaiterCallEvent(tableId, tableNumber, payload.optString("calledAt")))
                }
            }
            newSocket.on("incoming_call") { args ->
                val payload = args.firstOrNull() as? JSONObject
                val phone = payload?.optString("phone")?.takeIf { it.isNotBlank() }
                if (phone != null) {
                    _incomingCallEvents.tryEmit(
                        IncomingCallEvent(
                            phone = phone,
                            callerName = payload.optString("callerName").takeIf { it.isNotBlank() },
                            address = payload.optString("address").takeIf { it.isNotBlank() },
                            isKnown = payload.optBoolean("isKnown", false),
                            receivedAt = payload.optString("receivedAt"),
                        ),
                    )
                }
            }
            newSocket.on(Socket.EVENT_CONNECT_ERROR) { args -> Log.w(TAG, "Socket connect error: ${args.firstOrNull()}") }
            newSocket.connect()
            socket = newSocket
        }.onFailure { Log.e(TAG, "Couldn't start Socket.IO connection", it) }
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }

    private companion object {
        const val TAG = "FireHutRealtime"
    }
}
