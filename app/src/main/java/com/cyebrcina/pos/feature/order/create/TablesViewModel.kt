package com.cyebrcina.pos.feature.order.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.remote.model.RestaurantTableDto
import com.cyebrcina.pos.data.remote.realtime.FireHutRealtimeManager
import com.cyebrcina.pos.data.repository.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class TablesViewModel @Inject constructor(
    private val heldOrdersStore: HeldOrdersStore,
    private val entryCoordinator: NewOrderEntryCoordinator,
    private val tableRepository: TableRepository,
    private val realtimeManager: FireHutRealtimeManager,
) : ViewModel() {

    val heldOrders: StateFlow<List<HeldOrder>> = heldOrdersStore.heldOrders

    private val _tables = MutableStateFlow<List<RestaurantTableDto>>(emptyList())
    val tables: StateFlow<List<RestaurantTableDto>> = _tables.asStateFlow()

    init {
        refresh()
        // A table's real status is derived from its open orders (see
        // Admin's GET /api/device/tables) — any order event can flip it, so
        // the same signal OrderRepository already re-polls on is reused
        // here instead of a dedicated table-status socket event.
        realtimeManager.orderEvents.onEach { refresh() }.launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            tableRepository.getTables().onSuccess { _tables.value = it }
        }
    }

    /** Tap on an available table — seeds the New Order flow to start there, skipping the "Create New Order" popup. */
    fun startAtTable(table: String) {
        entryCoordinator.setPending(NewOrderEntryIntent.StartAtTable(table))
    }

    /** Tap on a held order (table-bound or a Collection order surfaced in the held list) — resumes its cart. */
    fun resumeHeldOrder(heldOrderId: String) {
        entryCoordinator.setPending(NewOrderEntryIntent.ResumeHeldOrder(heldOrderId))
    }
}
