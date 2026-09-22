package com.cyebrcina.pos.feature.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.local.CurrentStaffStore
import com.cyebrcina.pos.data.remote.model.CashRegisterSessionDto
import com.cyebrcina.pos.data.remote.model.ZReport
import com.cyebrcina.pos.data.repository.CashRegisterRepository
import com.cyebrcina.pos.data.repository.ReportChannel
import com.cyebrcina.pos.data.repository.ReportRepository
import com.cyebrcina.pos.printer.PrinterService
import com.cyebrcina.pos.printer.ReceiptBuilder
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
    // A read-only mid-shift checkpoint (today's running totals, incl. cash/card split) — unlike
    // Register Close, pulling this up never touches CashRegisterSession or resets anything.
    val xReport: ZReport? = null,
    val isLoadingXReport: Boolean = false,
    val xReportError: String? = null,
    val xReportPrintWarning: String? = null,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerRepository: CashRegisterRepository,
    private val currentStaffStore: CurrentStaffStore,
    private val reportRepository: ReportRepository,
    private val printerService: PrinterService,
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

    /** Today's running totals (gross/net/cash/card so far), non-destructive — same underlying
     * query as the end-of-day Report tab's Z-Report, just for "right now" rather than a closed
     * day, and surfaced here since Cash Register is where staff already think in shift terms. */
    fun viewXReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingXReport = true, xReportError = null, xReportPrintWarning = null) }
            reportRepository.getZReport(date = null, channel = ReportChannel.TILL)
                .onSuccess { report -> _uiState.update { it.copy(xReport = report, isLoadingXReport = false) } }
                .onFailure { err -> _uiState.update { it.copy(isLoadingXReport = false, xReportError = err.message) } }
        }
    }

    fun dismissXReport() {
        _uiState.update { it.copy(xReport = null, xReportError = null, xReportPrintWarning = null) }
    }

    fun printXReport() {
        val report = _uiState.value.xReport ?: return
        viewModelScope.launch {
            printerService.print(ReceiptBuilder.buildXReport(report))
                .onFailure { err -> _uiState.update { it.copy(xReportPrintWarning = err.message) } }
        }
    }
}
