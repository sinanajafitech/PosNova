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
