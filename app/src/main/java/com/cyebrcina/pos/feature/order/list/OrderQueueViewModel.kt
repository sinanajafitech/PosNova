package com.cyebrcina.pos.feature.order.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.customerdisplay.CustomerDisplayManager
import com.cyebrcina.pos.customerdisplay.CustomerDisplayState
import com.cyebrcina.pos.data.model.DeviceSession
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.data.remote.model.ZReport
import com.cyebrcina.pos.data.repository.AuthRepository
import com.cyebrcina.pos.data.repository.OrderRepository
import com.cyebrcina.pos.data.repository.ReportRepository
import com.cyebrcina.pos.data.repository.StoreStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Aggregated from recent order history (`OrderRepository.history`) — how many times each product was ordered. */
data class TopProduct(val name: String, val timesOrdered: Int)

data class DashboardStats(
    val today: ZReport? = null,
    val yesterday: ZReport? = null,
    val topProducts: List<TopProduct> = emptyList(),
)

data class OrderQueueUiState(
    val session: DeviceSession? = null,
    val pendingOrders: List<DeviceOrder> = emptyList(),
    val acceptingOrders: Boolean? = null,
    val stats: DashboardStats = DashboardStats(),
) {
    /** Percent change vs yesterday — null if yesterday's Z-report isn't available. */
    fun trend(current: Double, metric: (ZReport) -> Double): Double? {
        val prev = stats.yesterday?.let(metric) ?: return null
        if (prev == 0.0) return null
        return ((current - prev) / prev) * 100
    }
}

@HiltViewModel
class OrderQueueViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val storeStatusRepository: StoreStatusRepository,
    private val authRepository: AuthRepository,
    private val customerDisplayManager: CustomerDisplayManager,
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val dashboardStats = MutableStateFlow(DashboardStats())

    val uiState: StateFlow<OrderQueueUiState> = combine(
        authRepository.session,
        orderRepository.pendingOrders,
        storeStatusRepository.acceptingOrders,
        dashboardStats,
    ) { session, orders, accepting, stats ->
        OrderQueueUiState(
            session = session,
            pendingOrders = orders.sortedBy { it.createdAt },
            acceptingOrders = accepting,
            stats = stats,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OrderQueueUiState())

    private var seenOrderIds: Set<String>? = null

    init {
        orderRepository.startPolling()
        viewModelScope.launch { storeStatusRepository.refresh() }
        viewModelScope.launch { loadDashboardStats() }

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

    private suspend fun loadDashboardStats() {
        val today = LocalDate.now()
        val todayDeferred = viewModelScope.async { reportRepository.getZReport(today).getOrNull() }
        val yesterdayDeferred = viewModelScope.async { reportRepository.getZReport(today.minusDays(1)).getOrNull() }
        val historyDeferred = viewModelScope.async { orderRepository.history(100).getOrNull().orEmpty() }

        val topProducts = historyDeferred.await()
            .flatMap { it.items }
            .groupBy { it.productName }
            .map { (name, items) -> TopProduct(name, items.sumOf { item -> item.quantity }) }
            .sortedByDescending { it.timesOrdered }
            .take(5)

        dashboardStats.value = DashboardStats(
            today = todayDeferred.await(),
            yesterday = yesterdayDeferred.await(),
            topProducts = topProducts,
        )
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
            loadDashboardStats()
        }
    }
}
