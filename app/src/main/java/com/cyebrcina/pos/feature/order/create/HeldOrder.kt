package com.cyebrcina.pos.feature.order.create

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** A cart parked mid-build so staff can serve another table/customer, then come back to it. */
data class HeldOrder(
    val id: String = UUID.randomUUID().toString(),
    val orderType: TillOrderType,
    val tableLabel: String?,
    val customerName: String,
    val customerPhone: String? = null,
    val phoneCallLogId: String? = null,
    val guestCount: Int,
    val cart: List<CartItem>,
    val heldAt: Instant = Instant.now(),
) {
    val itemCount: Int get() = cart.sumOf { it.quantity }
    val total: Double get() = cart.subtotal()
}

/**
 * In-memory only — held orders are a till-side convenience (park a cart, come back to it later
 * in the same shift), not an API concept (`openapi.yaml` has no draft/hold endpoint), so there's
 * nothing to persist them against server-side. They don't survive a process kill; that's an
 * accepted trade-off rather than an oversight — a real hold queue is short-lived by nature.
 */
@Singleton
class HeldOrdersStore @Inject constructor() {
    private val _heldOrders = MutableStateFlow<List<HeldOrder>>(emptyList())
    val heldOrders: StateFlow<List<HeldOrder>> = _heldOrders.asStateFlow()

    fun hold(order: HeldOrder) {
        _heldOrders.update { it + order }
    }

    fun remove(id: String): HeldOrder? {
        val existing = _heldOrders.value.firstOrNull { it.id == id } ?: return null
        _heldOrders.update { list -> list.filter { it.id != id } }
        return existing
    }

    fun heldForTable(table: String): HeldOrder? = _heldOrders.value.firstOrNull { it.tableLabel == table }
}
