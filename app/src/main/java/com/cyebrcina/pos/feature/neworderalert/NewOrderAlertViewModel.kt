package com.cyebrcina.pos.feature.neworderalert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.core.util.NewOrderAlertPlayer
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Owns the staff-facing "new order" popup + alert sound — until now, a new website/QR order
 * arriving only ever updated the customer-facing secondary display (useless for alerting staff
 * looking at the till itself) and silently bumped a count on the Dashboard tab, with no sound and
 * nothing staff would notice unless they happened to already be looking at that one screen.
 * Mirrors [com.cyebrcina.pos.feature.incomingcall.IncomingCallViewModel]'s queue-not-drop shape
 * so several orders arriving close together are each shown in turn, not dropped.
 */
@HiltViewModel
class NewOrderAlertViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val alertPlayer: NewOrderAlertPlayer,
) : ViewModel() {

    private val queue = MutableStateFlow<List<DeviceOrder>>(emptyList())
    val current: StateFlow<DeviceOrder?> = queue
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Null (not yet initialized) on the very first emission is deliberate — every order already
    // pending when the till starts up is "old news", not a new arrival to alert on; only orders
    // that appear in some later emission actually trigger the popup/sound.
    private var seenOrderIds: Set<String>? = null

    init {
        orderRepository.pendingOrders.onEach { orders ->
            val ids = orders.map { it.id }.toSet()
            val previouslySeen = seenOrderIds
            val newOrders = if (previouslySeen != null) orders.filter { it.id !in previouslySeen } else emptyList()
            seenOrderIds = ids
            if (newOrders.isNotEmpty()) {
                queue.update { it + newOrders }
                alertPlayer.play(orderRepository.notificationSoundUrl.value)
            }
        }.launchIn(viewModelScope)
    }

    fun dismissCurrent() {
        queue.update { if (it.isEmpty()) it else it.drop(1) }
        alertPlayer.stop()
    }
}
