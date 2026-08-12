package com.cyebrcina.pos.feature.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.remote.model.DeviceCall
import com.cyebrcina.pos.data.remote.realtime.FireHutRealtimeManager
import com.cyebrcina.pos.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CallsUiState(
    val calls: List<DeviceCall> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class CallsViewModel @Inject constructor(
    private val callRepository: CallRepository,
    realtimeManager: FireHutRealtimeManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallsUiState())
    val uiState: StateFlow<CallsUiState> = _uiState.asStateFlow()

    init {
        refresh()
        // A new call, or an existing one turning into an order via "Take Order" (which
        // fires new_order, not another incoming_call), should both be reflected here
        // without staff having to remember to tap Refresh.
        merge(realtimeManager.incomingCallEvents.map { }, realtimeManager.orderEvents)
            .onEach { refresh() }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            callRepository.getCalls()
                .onSuccess { calls -> _uiState.update { it.copy(calls = calls, isLoading = false) } }
                .onFailure { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
        }
    }
}
