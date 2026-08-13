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
import org.json.JSONArray
import org.json.JSONObject

/** A dine-in diner tapped "Call Waiter" on the menu — see Admin's
 * POST /api/orders/waiter-call, which emits this. Unlike the order events
 * below, there's no REST endpoint to re-poll for "current waiter calls",
 * so the payload itself has to carry what the popup needs rather than
 * just being a refresh signal. */
data class WaiterCallEvent(val tableId: String, val tableNumber: String, val calledAt: String)

/** A saved address on a caller's profile (see Admin's CustomerAddress /
 * lib/customer-profile.ts) — the multi-address book behind "Change/Add
 * Address" in [IncomingCallOverlay][com.cyebrcina.pos.feature.incomingcall.IncomingCallOverlay]. */
data class IncomingCallAddress(
    val id: String,
    val label: String,
    val line1: String,
    val line2: String?,
    val postcode: String,
    val area: String?,
    val lastUsedAt: String,
)

/** One line of a past or "usual" order — same shape whether it's a real
 * order's line item or the computed usual-order pattern. */
data class IncomingCallOrderItem(
    val productName: String,
    val sizeLabel: String?,
    val quantity: Int,
    val addOnNames: List<String>,
)

data class IncomingCallOrderSummary(
    val id: String,
    val number: String,
    val createdAt: String,
    val type: String,
    val total: Double,
    val items: List<IncomingCallOrderItem>,
)

/** Fee/ETA/minimum for whichever admin-configured DeliveryZone matches the
 * caller's most-recently-used address — see matchDeliveryZone() in Admin's
 * lib/customer-profile.ts. Null when no address is on file, or none match. */
data class IncomingCallDeliveryZone(
    val id: String,
    val name: String,
    val fee: Double,
    val minOrder: Double,
    val etaMinutes: Int?,
)

/** A call came in on the restaurant's phone line — see Admin's
 * POST /api/integrations/phone/incoming-call (the bOnline webhook
 * receiver), which resolves the caller against Customer/PhoneContact and
 * emits this. Like [WaiterCallEvent], the payload carries what the popup
 * needs directly since there's no REST endpoint to re-poll.
 *
 * `customerId`/`contactId` and everything below `receivedAt` are the
 * Customer Intelligence enrichment (Admin's lib/customer-profile.ts) —
 * additive on top of the original fields, so this always parses even if a
 * future payload change dropped one of them. */
data class IncomingCallEvent(
    val phone: String,
    val callerName: String?,
    val address: String?,
    // The PhoneCallLog row Admin recorded this call as — passed through to
    // CreateOrderRequest.phoneCallLogId if staff tap "Take Order".
    val callLogId: String?,
    val isKnown: Boolean,
    val receivedAt: String,
    val customerId: String?,
    val contactId: String?,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val addresses: List<IncomingCallAddress> = emptyList(),
    val orderCount: Int = 0,
    val totalSpent: Double = 0.0,
    val lastOrder: IncomingCallOrderSummary? = null,
    val usualOrder: List<IncomingCallOrderItem>? = null,
    val deliveryZone: IncomingCallDeliveryZone? = null,
)

private fun parseOrderItem(json: JSONObject): IncomingCallOrderItem {
    val addOnNames = mutableListOf<String>()
    val addOnArray = json.optJSONArray("addOnNames")
    if (addOnArray != null) {
        for (i in 0 until addOnArray.length()) addOnNames.add(addOnArray.optString(i))
    }
    return IncomingCallOrderItem(
        productName = json.optString("productName"),
        sizeLabel = json.optString("sizeLabel").takeIf { it.isNotBlank() },
        quantity = json.optInt("quantity", 1),
        addOnNames = addOnNames,
    )
}

private fun parseOrderItemList(array: JSONArray?): List<IncomingCallOrderItem> {
    if (array == null) return emptyList()
    return (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let(::parseOrderItem) }
}

private fun parseOrderSummary(json: JSONObject?): IncomingCallOrderSummary? {
    if (json == null) return null
    return IncomingCallOrderSummary(
        id = json.optString("id"),
        number = json.optString("number"),
        createdAt = json.optString("createdAt"),
        type = json.optString("type"),
        total = json.optDouble("total", 0.0),
        items = parseOrderItemList(json.optJSONArray("items")),
    )
}

private fun parseAddresses(array: JSONArray?): List<IncomingCallAddress> {
    if (array == null) return emptyList()
    return (0 until array.length()).mapNotNull { i ->
        val obj = array.optJSONObject(i) ?: return@mapNotNull null
        IncomingCallAddress(
            id = obj.optString("id"),
            label = obj.optString("label"),
            line1 = obj.optString("line1"),
            line2 = obj.optString("line2").takeIf { it.isNotBlank() },
            postcode = obj.optString("postcode"),
            area = obj.optString("area").takeIf { it.isNotBlank() },
            lastUsedAt = obj.optString("lastUsedAt"),
        )
    }
}

private fun parseTags(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return (0 until array.length()).map { i -> array.optString(i) }
}

private fun parseDeliveryZone(json: JSONObject?): IncomingCallDeliveryZone? {
    if (json == null) return null
    return IncomingCallDeliveryZone(
        id = json.optString("id"),
        name = json.optString("name"),
        fee = json.optDouble("fee", 0.0),
        minOrder = json.optDouble("minOrder", 0.0),
        etaMinutes = if (json.isNull("etaMinutes")) null else json.optInt("etaMinutes"),
    )
}

private fun parseIncomingCallEvent(phone: String, payload: JSONObject): IncomingCallEvent = IncomingCallEvent(
    phone = phone,
    callerName = payload.optString("callerName").takeIf { it.isNotBlank() },
    address = payload.optString("address").takeIf { it.isNotBlank() },
    callLogId = payload.optString("callLogId").takeIf { it.isNotBlank() },
    isKnown = payload.optBoolean("isKnown", false),
    receivedAt = payload.optString("receivedAt"),
    customerId = payload.optString("customerId").takeIf { it.isNotBlank() },
    contactId = payload.optString("contactId").takeIf { it.isNotBlank() },
    tags = parseTags(payload.optJSONArray("tags")),
    notes = payload.optString("notes").takeIf { it.isNotBlank() },
    addresses = parseAddresses(payload.optJSONArray("addresses")),
    orderCount = payload.optInt("orderCount", 0),
    totalSpent = payload.optDouble("totalSpent", 0.0),
    lastOrder = parseOrderSummary(payload.optJSONObject("lastOrder")),
    usualOrder = payload.optJSONArray("usualOrder")?.let(::parseOrderItemList),
    deliveryZone = parseDeliveryZone(payload.optJSONObject("deliveryZone")),
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

    /** A product was 86'd/brought back from this till, another till's Tools/86 Board, or
     * Admin's own Menu page — signal-only, same pattern as [orderEvents]. */
    private val _menuEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val menuEvents: SharedFlow<Unit> = _menuEvents.asSharedFlow()

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
                    _incomingCallEvents.tryEmit(parseIncomingCallEvent(phone, payload))
                }
            }
            newSocket.on("menu_updated") { _menuEvents.tryEmit(Unit) }
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
