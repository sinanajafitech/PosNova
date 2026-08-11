package com.cyebrcina.pos.feature.profile.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.local.KitchenPrinterSettings
import com.cyebrcina.pos.data.local.PrinterSettingsStore
import com.cyebrcina.pos.data.model.DeviceSession
import com.cyebrcina.pos.data.remote.model.ReceiptPrefs
import com.cyebrcina.pos.data.repository.AuthRepository
import com.cyebrcina.pos.data.repository.OrderRepository
import com.cyebrcina.pos.data.repository.StoreStatusRepository
import com.cyebrcina.pos.printer.PrinterService
import com.cyebrcina.pos.printer.ReceiptBuilder
import com.cyebrcina.pos.printer.model.DiscoveredPrinter
import com.cyebrcina.pos.printer.model.PrinterPaperSize
import com.cyebrcina.pos.printer.model.PrinterStatus
import com.cyebrcina.pos.printer.network.KitchenPrinterDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val session: DeviceSession? = null,
    val printerStatus: PrinterStatus = PrinterStatus.UNKNOWN,
    val discoveredPrinters: List<DiscoveredPrinter> = emptyList(),
    val isTestPrinting: Boolean = false,
    val testPrintError: String? = null,
    val acceptingOrders: Boolean? = null,
    val isTogglingStatus: Boolean = false,
    val kitchenPrinter: KitchenPrinterSettings = KitchenPrinterSettings(),
    val isTestPrintingKitchen: Boolean = false,
    val kitchenTestResult: String? = null,
    val receiptPrefs: ReceiptPrefs? = null,
)

private data class ExternalState(
    val session: DeviceSession?,
    val printerStatus: PrinterStatus,
    val discoveredPrinters: List<DiscoveredPrinter>,
    val acceptingOrders: Boolean?,
    val kitchenPrinter: KitchenPrinterSettings,
)

private data class LocalFlags(
    val isTestPrinting: Boolean = false,
    val testPrintError: String? = null,
    val isTogglingStatus: Boolean = false,
    val isTestPrintingKitchen: Boolean = false,
    val kitchenTestResult: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storeStatusRepository: StoreStatusRepository,
    private val orderRepository: OrderRepository,
    private val printerService: PrinterService,
    private val printerSettingsStore: PrinterSettingsStore,
    private val kitchenPrinterDispatcher: KitchenPrinterDispatcher,
) : ViewModel() {

    private val localFlags = MutableStateFlow(LocalFlags())

    init {
        // The main printer selection is normally made once (by whoever set the till up) and
        // should just keep working across app restarts/reboots — previously nothing here was
        // persisted at all, so every relaunch meant reselecting/reconnecting the printer by hand.
        viewModelScope.launch {
            printerSettingsStore.mainPrinter.first()?.let { saved ->
                printerService.selectPrinter(saved)
            }
        }
    }

    private val externalState = combine(
        authRepository.session,
        printerService.status,
        printerService.discoveredPrinters,
        storeStatusRepository.acceptingOrders,
        printerSettingsStore.kitchenPrinter,
    ) { session, printerStatus, printers, accepting, kitchenPrinter ->
        ExternalState(session, printerStatus, printers, accepting, kitchenPrinter)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        externalState,
        orderRepository.receiptPrefs,
        localFlags,
    ) { external, receiptPrefs, local ->
        SettingsUiState(
            session = external.session,
            printerStatus = external.printerStatus,
            discoveredPrinters = external.discoveredPrinters,
            isTestPrinting = local.isTestPrinting,
            testPrintError = local.testPrintError,
            acceptingOrders = external.acceptingOrders,
            isTogglingStatus = local.isTogglingStatus,
            kitchenPrinter = external.kitchenPrinter,
            isTestPrintingKitchen = local.isTestPrintingKitchen,
            kitchenTestResult = local.kitchenTestResult,
            receiptPrefs = receiptPrefs,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun startPrinterDiscovery() {
        viewModelScope.launch { printerService.startDiscovery() }
    }

    fun selectPrinter(printer: DiscoveredPrinter) {
        viewModelScope.launch {
            printerService.selectPrinter(printer)
            printerSettingsStore.saveMainPrinter(printer)
        }
    }

    fun testPrint() {
        viewModelScope.launch {
            localFlags.update { it.copy(isTestPrinting = true, testPrintError = null) }
            val storeName = uiState.value.session?.storeName?.ifBlank { null } ?: "Fire Hut Pizza & Wraps"
            printerService.print(ReceiptBuilder.buildTestPrint(storeName))
                .onSuccess { localFlags.update { it.copy(isTestPrinting = false) } }
                .onFailure { err -> localFlags.update { it.copy(isTestPrinting = false, testPrintError = err.message) } }
        }
    }

    fun openCashDrawer() {
        viewModelScope.launch { printerService.openCashDrawer() }
    }

    /** Persists the kitchen printer form as the cashier edits it. */
    fun setKitchenPrinter(settings: KitchenPrinterSettings) {
        viewModelScope.launch { printerSettingsStore.saveKitchenPrinter(settings) }
    }

    fun testKitchenPrint() {
        viewModelScope.launch {
            localFlags.update { it.copy(isTestPrintingKitchen = true, kitchenTestResult = null) }
            val storeName = uiState.value.session?.storeName?.ifBlank { null } ?: "Fire Hut Pizza & Wraps"
            val result = kitchenPrinterDispatcher.printIfConfigured(
                ReceiptBuilder.buildTestPrint(storeName),
                PrinterPaperSize.MM_58,
            )
            val message = when (result) {
                null -> "Enter a host/IP and turn the kitchen printer on first."
                true -> "Sent — check the kitchen printer."
                false -> "Couldn't reach the kitchen printer. Check the IP/port and that it's on the same network."
            }
            localFlags.update { it.copy(isTestPrintingKitchen = false, kitchenTestResult = message) }
        }
    }

    fun toggleAcceptingOrders() {
        val current = uiState.value.acceptingOrders ?: return
        viewModelScope.launch {
            localFlags.update { it.copy(isTogglingStatus = true) }
            storeStatusRepository.setAcceptingOrders(!current)
            localFlags.update { it.copy(isTogglingStatus = false) }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
