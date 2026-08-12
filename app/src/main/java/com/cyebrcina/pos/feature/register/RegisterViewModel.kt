package com.cyebrcina.pos.feature.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.local.CurrentStaffStore
import com.cyebrcina.pos.data.remote.model.CashRegisterSessionDto
import com.cyebrcina.pos.data.repository.CashRegisterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val session: CashRegisterSessionDto? = null,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    // The just-closed session's reconciliation summary, shown once until dismissed —
    // kept separate from `session` since closing clears the till's own open session.
    val justClosed: CashRegisterSessionDto? = null,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerRepository: CashRegisterRepository,
    private val currentStaffStore: CurrentStaffStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            registerRepository.current()
                .onSuccess { session -> _uiState.update { it.copy(session = session, isLoading = false) } }
                .onFailure { err -> _uiState.update { it.copy(isLoading = false, error = err.message) } }
        }
    }

    fun openRegister(openingFloat: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            val staffId = currentStaffStore.currentStaff.first()?.id
            registerRepository.open(openingFloat, staffId)
                .onSuccess { session -> _uiState.update { it.copy(session = session, isSubmitting = false) } }
                .onFailure { err -> _uiState.update { it.copy(isSubmitting = false, error = err.message) } }
        }
    }

    fun closeRegister(countedCash: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            val staffId = currentStaffStore.currentStaff.first()?.id
            registerRepository.close(countedCash, staffId)
                .onSuccess { session -> _uiState.update { it.copy(session = null, justClosed = session, isSubmitting = false) } }
                .onFailure { err -> _uiState.update { it.copy(isSubmitting = false, error = err.message) } }
        }
    }

    fun dismissClosedSummary() {
        _uiState.update { it.copy(justClosed = null) }
    }
}
