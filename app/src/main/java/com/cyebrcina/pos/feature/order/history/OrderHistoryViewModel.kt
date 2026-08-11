package com.cyebrcina.pos.feature.order.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.core.util.toInstantOrNull
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val PAGE_SIZE_OPTIONS = listOf(10, 25, 50)

data class OrderHistoryUiState(
    val allOrders: List<DeviceOrder> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val dateFilter: LocalDate? = null,
    val pageSize: Int = PAGE_SIZE_OPTIONS[0],
    val page: Int = 0,
) {
    val filtered: List<DeviceOrder>
        get() = allOrders
            .filter { order ->
                if (dateFilter == null) return@filter true
                val orderDate = order.createdAt.toInstantOrNull()?.atZone(ZoneId.systemDefault())?.toLocalDate()
                orderDate == dateFilter
            }
            .filter { order ->
                if (searchQuery.isBlank()) return@filter true
                order.number.contains(searchQuery, ignoreCase = true) || order.customerName.contains(searchQuery, ignoreCase = true)
            }

    val pageCount: Int get() = ((filtered.size - 1) / pageSize + 1).coerceAtLeast(1)
    val pagedOrders: List<DeviceOrder> get() = filtered.drop(page * pageSize).take(pageSize)
}

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderHistoryUiState())
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            orderRepository.history(limit = 100)
                .onSuccess { orders -> _uiState.update { it.copy(allOrders = orders, isLoading = false, page = 0) } }
                .onFailure { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
        }
    }

    fun onSearchQueryChange(query: String) = _uiState.update { it.copy(searchQuery = query, page = 0) }

    fun onDateFilterChange(date: LocalDate?) = _uiState.update { it.copy(dateFilter = date, page = 0) }

    fun onPageSizeChange(size: Int) = _uiState.update { it.copy(pageSize = size, page = 0) }

    fun previousPage() = _uiState.update { it.copy(page = (it.page - 1).coerceAtLeast(0)) }

    fun nextPage() = _uiState.update { it.copy(page = (it.page + 1).coerceAtMost(it.pageCount - 1)) }
}
