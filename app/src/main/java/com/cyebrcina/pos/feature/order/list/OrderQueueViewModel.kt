package com.cyebrcina.pos.feature.order.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.customerdisplay.CustomerDisplayManager
import com.cyebrcina.pos.customerdisplay.CustomerDisplayState
import com.cyebrcina.pos.data.model.DeviceSession
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.data.repository.AuthRepository
import com.cyebrcina.pos.data.repository.OrderRepository
import com.cyebrcina.pos.data.repository.StoreStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrderQueueUiState(
    val session: DeviceSession? = null,
    val pendingOrders: List<DeviceOrder> = emptyList(),
    val acceptingOrders: Boolean? = null,
)

@HiltViewModel
class OrderQueueViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val storeStatusRepository: StoreStatusRepository,
    private val authRepository: AuthRepository,
    private val customerDisplayManager: CustomerDisplayManager,
) : ViewModel() {

    val uiState: StateFlow<OrderQueueUiState> = combine(
        authRepository.session,
        orderRepository.pendingOrders,
        storeStatusRepository.acceptingOrders,
    ) { session, orders, accepting ->
        OrderQueueUiState(
            session = session,
            pendingOrders = orders.sortedBy { it.createdAt },
            acceptingOrders = accepting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OrderQueueUiState())

    private var seenOrderIds: Set<String>? = null

    init {
        orderRepository.startPolling()
        viewModelScope.launch { storeStatusRepository.refresh() }

        authRepository.session.onEach { session ->
            if (session != null) {
                customerDisplayManager.update(CustomerDisplayState.Idle)
            }
        }.launchIn(viewModelScope)

        orderRepository.pendingOrders.onEach { orders ->
            val ids = orders.map { it.id }.toSet()
            val previouslySeen = seenOrderIds
            val newOrder = if (previouslySeen != null) orders.firstOrNull { it.id !in previouslySeen } else null
            seenOrderIds = ids
            if (newOrder != null) {
                pulseNewOrder(newOrder.number)
            }
        }.launchIn(viewModelScope)
    }

    private fun pulseNewOrder(orderNumber: String) {
        viewModelScope.launch {
            customerDisplayManager.update(CustomerDisplayState.NewOrderReceived(orderNumber))
            delay(6000)
            customerDisplayManager.update(CustomerDisplayState.Idle)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            orderRepository.refreshPending()
            storeStatusRepository.refresh()
        }
    }
}
