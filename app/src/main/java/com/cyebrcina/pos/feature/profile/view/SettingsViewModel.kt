package com.cyebrcina.pos.feature.profile.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.model.DeviceSession
import com.cyebrcina.pos.data.repository.AuthRepository
import com.cyebrcina.pos.data.repository.StoreStatusRepository
import com.cyebrcina.pos.printer.PrinterService
import com.cyebrcina.pos.printer.ReceiptBuilder
import com.cyebrcina.pos.printer.model.DiscoveredPrinter
import com.cyebrcina.pos.printer.model.PrinterStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val session: DeviceSession? = null,
    val printerStatus: PrinterStatus = PrinterStatus.UNKNOWN,
    val discoveredPrinters: List<DiscoveredPrinter> = emptyList(),
    val isTestPrinting: Boolean = false,
    val testPrintError: String? = null,
    val acceptingOrders: Boolean? = null,
    val isTogglingStatus: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storeStatusRepository: StoreStatusRepository,
    private val printerService: PrinterService,
) : ViewModel() {

    private val isTestPrinting = MutableStateFlow(false)
    private val testPrintError = MutableStateFlow<String?>(null)
    private val isTogglingStatus = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        authRepository.session,
        printerService.status,
        printerService.discoveredPrinters,
        storeStatusRepository.acceptingOrders,
        combine(isTestPrinting, testPrintError, isTogglingStatus, ::Triple),
    ) { session, printerStatus, printers, accepting, testFlags ->
        SettingsUiState(
            session = session,
            printerStatus = printerStatus,
            discoveredPrinters = printers,
            isTestPrinting = testFlags.first,
            testPrintError = testFlags.second,
            acceptingOrders = accepting,
            isTogglingStatus = testFlags.third,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun startPrinterDiscovery() {
        viewModelScope.launch { printerService.startDiscovery() }
    }

    fun selectPrinter(printer: DiscoveredPrinter) {
        viewModelScope.launch { printerService.selectPrinter(printer) }
    }

    fun testPrint() {
        viewModelScope.launch {
            isTestPrinting.value = true
            testPrintError.value = null
            val storeName = uiState.value.session?.storeName?.ifBlank { null } ?: "Fire Hut Pizza & Wraps"
            printerService.print(ReceiptBuilder.buildTestPrint(storeName))
                .onSuccess { isTestPrinting.value = false }
                .onFailure { err ->
                    isTestPrinting.value = false
                    testPrintError.value = err.message
                }
        }
    }

    fun openCashDrawer() {
        viewModelScope.launch { printerService.openCashDrawer() }
    }

    fun toggleAcceptingOrders() {
        val current = uiState.value.acceptingOrders ?: return
        viewModelScope.launch {
            isTogglingStatus.value = true
            storeStatusRepository.setAcceptingOrders(!current)
            isTogglingStatus.value = false
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
