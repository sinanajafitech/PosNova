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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One day's gross sales, for the "last 7 days" bar chart. Null [report] means that day's fetch failed. */
data class DayPoint(val date: LocalDate, val report: ZReport?)

data class ReportUiState(
    val date: LocalDate = LocalDate.now(),
    val report: ZReport? = null,
    val previousDayReport: ZReport? = null,
    val last7Days: List<DayPoint> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val printJobState: PrintJobState = PrintJobState.IDLE,
) {
    val isToday: Boolean get() = date == LocalDate.now()

    /** Percent change vs the previous day — null if we don't have both figures (e.g. previous day's fetch failed). */
    fun trend(current: Double, previous: (ZReport) -> Double): Double? {
        val prev = previousDayReport?.let(previous) ?: return null
        if (prev == 0.0) return null
        return ((current - prev) / prev) * 100
    }
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
        val date = _uiState.value.date
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            reportRepository.getZReport(date)
                .onSuccess { report -> _uiState.update { it.copy(report = report, isLoading = false) } }
                .onFailure { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }

            // Trend chip + 7-day chart both need neighbouring days — fetched in parallel rather
            // than blocking the main report on them.
            launch {
                reportRepository.getZReport(date.minusDays(1))
                    .onSuccess { prev -> _uiState.update { it.copy(previousDayReport = prev) } }
            }
            launch {
                val days = (6 downTo 0).map { offset -> date.minusDays(offset.toLong()) }
                val points = days.map { day ->
                    async { DayPoint(day, reportRepository.getZReport(day).getOrNull()) }
                }.awaitAll()
                _uiState.update { it.copy(last7Days = points) }
            }
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
