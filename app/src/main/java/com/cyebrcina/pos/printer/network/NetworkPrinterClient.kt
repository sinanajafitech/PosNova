package com.cyebrcina.pos.printer.network

import android.util.Log
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** The standard "direct IP printing" (JetDirect-style) raw-socket port virtually every
 * network-capable ESC/POS printer listens on. */
const val DEFAULT_NETWORK_PRINTER_PORT = 9100

/**
 * Opens a raw TCP socket to a network ESC/POS printer, writes the bytes, and closes — one
 * connection per print job rather than a persistent connection like
 * [com.cyebrcina.pos.printer.imin.IminPrinterService] keeps for Bluetooth/USB. This is the
 * normal pattern for network kitchen printers: the till and the printer aren't necessarily on
 * screen/reachable at the same time, and printers on this port accept a fresh connection per
 * job without needing anything kept alive between them.
 */
@Singleton
class NetworkPrinterClient @Inject constructor() {

    suspend fun send(host: String, port: Int, bytes: ByteArray, timeoutMs: Int = 5000): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Bounds the whole operation, not just the initial connect. Socket.setSoTimeout()
                // only governs read() — irrelevant here, this is fire-and-forget with no response
                // read back — so a printer that accepts the TCP handshake but then stalls (jam, out
                // of paper, unresponsive firmware not draining its receive buffer) could otherwise
                // block write()/flush() indefinitely, hanging whatever order/print flow is waiting
                // on this result with no way to recover short of restarting the app.
                withTimeout(timeoutMs.toLong() * 2) {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, port), timeoutMs)
                        socket.getOutputStream().apply {
                            write(bytes)
                            flush()
                        }
                    }
                }
                Unit
            }.onFailure { Log.e(TAG, "Network print to $host:$port failed", it) }
        }

    private companion object {
        const val TAG = "NetworkPrinterClient"
    }
}
