package com.cyebrcina.pos.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cyebrcina.pos.data.remote.model.CreateOrderRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One order the till couldn't submit because there was no connection at the time — everything
 * needed to retry the exact same `POST /api/device/orders` call later. Display data (product
 * names etc.) is deliberately NOT kept here: the kitchen ticket/receipt for a queued order print
 * immediately from the live cart when it's queued (see NewOrderViewModel), not from this record
 * — this only exists to get the order to the server once a connection is back. */
@Serializable
data class PendingOrder(
    val localId: String,
    val request: CreateOrderRequest,
    val queuedAt: String,
    val attempts: Int = 0,
)

private val Context.pendingOrdersDataStore by preferencesDataStore(name = "pending_orders")

/**
 * The offline order queue — a single JSON-encoded list in one DataStore key, same tradeoff as
 * [MenuCacheStore]. A till realistically queues at most a handful of orders during an outage, so
 * rewriting the whole blob on every add/remove costs nothing in practice, and it avoids adding
 * Room (a whole new dependency + schema) for what's fundamentally a small list.
 */
@Singleton
class PendingOrderStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private object Keys {
        val ORDERS_JSON = stringPreferencesKey("orders_json")
    }

    val pendingOrders: Flow<List<PendingOrder>> = context.pendingOrdersDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val raw = prefs[Keys.ORDERS_JSON] ?: return@map emptyList()
            runCatching { json.decodeFromString<List<PendingOrder>>(raw) }.getOrDefault(emptyList())
        }

    /** Appends a new queued order and returns its generated [PendingOrder.localId]. */
    suspend fun enqueue(request: CreateOrderRequest, queuedAt: String): String {
        val localId = "offline-" + UUID.randomUUID().toString()
        val entry = PendingOrder(localId = localId, request = request, queuedAt = queuedAt)
        edit { it + entry }
        return localId
    }

    suspend fun remove(localId: String) {
        edit { list -> list.filterNot { it.localId == localId } }
    }

    suspend fun bumpAttempts(localId: String) {
        edit { list -> list.map { if (it.localId == localId) it.copy(attempts = it.attempts + 1) else it } }
    }

    private suspend fun edit(transform: (List<PendingOrder>) -> List<PendingOrder>) {
        context.pendingOrdersDataStore.edit { prefs ->
            val current = prefs[Keys.ORDERS_JSON]?.let { raw ->
                runCatching { json.decodeFromString<List<PendingOrder>>(raw) }.getOrDefault(emptyList())
            } ?: emptyList()
            prefs[Keys.ORDERS_JSON] = json.encodeToString(transform(current))
        }
    }

    suspend fun currentList(): List<PendingOrder> = pendingOrders.first()
}
