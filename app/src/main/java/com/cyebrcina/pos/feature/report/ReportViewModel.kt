package com.cyebrcina.pos.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.remote.model.ZReport
import com.cyebrcina.pos.data.repository.ReportRepository
import com.cyebrcina.pos.printer.PrinterService
import com.cyebrcina.pos.printer.ReceiptBuilder
import com.cyebrcina.pos.printer.model.PrintJobState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportUiState(
    val date: LocalDate = LocalDate.now(),
    val report: ZReport? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val printJobState: PrintJobState = PrintJobState.IDLE,
) {
    val isToday: Boolean get() = date == LocalDate.now()
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val printerService: PrinterService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun previousDay() {
        _uiState.update { it.copy(date = it.date.minusDays(1)) }
        load()
    }

    fun nextDay() {
        if (_uiState.value.isToday) return
        _uiState.update { it.copy(date = it.date.plusDays(1)) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            reportRepository.getZReport(_uiState.value.date)
                .onSuccess { report -> _uiState.update { it.copy(report = report, isLoading = false) } }
                .onFailure { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
        }
    }

    fun printReport() {
        val report = _uiState.value.report ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(printJobState = PrintJobState.PRINTING) }
            printerService.print(ReceiptBuilder.buildZReport(report))
                .onSuccess { _uiState.update { it.copy(printJobState = PrintJobState.SUCCESS) } }
                .onFailure { _uiState.update { it.copy(printJobState = PrintJobState.FAILED) } }
        }
    }
}
