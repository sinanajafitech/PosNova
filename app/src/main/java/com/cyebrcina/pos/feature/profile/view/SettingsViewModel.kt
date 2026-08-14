package com.cyebrcina.pos.feature.profile.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.local.CurrentStaff
import com.cyebrcina.pos.data.local.CurrentStaffStore
import com.cyebrcina.pos.data.local.KitchenPrinterConnectionType
import com.cyebrcina.pos.data.local.KitchenPrinterSettings
import com.cyebrcina.pos.data.local.OfflineSyncManager
import com.cyebrcina.pos.data.local.OfflineSyncStatus
import com.cyebrcina.pos.data.local.PrinterSettingsStore
import com.cyebrcina.pos.data.model.DeviceSession
import com.cyebrcina.pos.data.remote.model.ReceiptPrefs
import com.cyebrcina.pos.data.repository.AuthRepository
import com.cyebrcina.pos.data.repository.OrderRepository
import com.cyebrcina.pos.data.repository.StaffRepository
import com.cyebrcina.pos.data.repository.StoreStatusRepository
import com.cyebrcina.pos.payment.PaymentTerminalService
import com.cyebrcina.pos.payment.model.PaymentProvider
import com.cyebrcina.pos.payment.model.TerminalStatus
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
    val selectedMainPrinter: DiscoveredPrinter? = null,
    val isTestPrinting: Boolean = false,
    val testPrintError: String? = null,
    val acceptingOrders: Boolean? = null,
    val isTogglingStatus: Boolean = false,
    val kitchenPrinter: KitchenPrinterSettings = KitchenPrinterSettings(),
    val isTestPrintingKitchen: Boolean = false,
    val kitchenTestResult: String? = null,
    val receiptPrefs: ReceiptPrefs? = null,
    /** Set by Admin -> Settings -> Card Terminal — not changeable from the till itself. */
    val terminalProvider: PaymentProvider = PaymentProvider.MOCK,
    val terminalStatus: TerminalStatus = TerminalStatus.DISCONNECTED,
    val isConnectingTerminal: Boolean = false,
    val terminalError: String? = null,
    val currentStaff: CurrentStaff? = null,
    val isClockingStaff: Boolean = false,
    val staffClockError: String? = null,
    val offlineSync: OfflineSyncStatus = OfflineSyncStatus(),
)

private data class ExternalState(
    val session: DeviceSession?,
    val printerStatus: PrinterStatus,
    val discoveredPrinters: List<DiscoveredPrinter>,
    val acceptingOrders: Boolean?,
    val kitchenPrinter: KitchenPrinterSettings,
)

private data class TerminalState(
    val provider: PaymentProvider,
    val status: TerminalStatus,
    val selectedMainPrinter: DiscoveredPrinter?,
)

private data class LocalFlags(
    val isTestPrinting: Boolean = false,
    val testPrintError: String? = null,
    val isTogglingStatus: Boolean = false,
    val isTestPrintingKitchen: Boolean = false,
    val kitchenTestResult: String? = null,
    val isConnectingTerminal: Boolean = false,
    val terminalError: String? = null,
    val isClockingStaff: Boolean = false,
    val staffClockError: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storeStatusRepository: StoreStatusRepository,
    private val orderRepository: OrderRepository,
    private val printerService: PrinterService,
    private val printerSettingsStore: PrinterSettingsStore,
    private val kitchenPrinterDispatcher: KitchenPrinterDispatcher,
    private val paymentTerminalService: PaymentTerminalService,
    private val staffRepository: StaffRepository,
    private val currentStaffStore: CurrentStaffStore,
    private val offlineSyncManager: OfflineSyncManager,
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
        // So "Last synced" has something to show immediately, even before staff ever tap
        // "Download for Offline Use" this session (e.g. it happened on a previous day).
        viewModelScope.launch { offlineSyncManager.refreshLastSyncedAt() }
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

    private val terminalState = combine(
        orderRepository.cardTerminal,
        paymentTerminalService.status,
        printerSettingsStore.mainPrinter,
    ) { config, status, mainPrinter ->
        val provider = PaymentProvider.entries.find { it.name == config?.provider } ?: PaymentProvider.MOCK
        TerminalState(provider, status, mainPrinter)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        externalState,
        terminalState,
        orderRepository.receiptPrefs,
        combine(localFlags, currentStaffStore.currentStaff, offlineSyncManager.status, ::Triple),
    ) { external, terminal, receiptPrefs, localAndStaff ->
        val (local, currentStaff, offlineSync) = localAndStaff
        SettingsUiState(
            session = external.session,
            printerStatus = external.printerStatus,
            discoveredPrinters = external.discoveredPrinters,
            selectedMainPrinter = terminal.selectedMainPrinter,
            isTestPrinting = local.isTestPrinting,
            testPrintError = local.testPrintError,
            acceptingOrders = external.acceptingOrders,
            isTogglingStatus = local.isTogglingStatus,
            kitchenPrinter = external.kitchenPrinter,
            isTestPrintingKitchen = local.isTestPrintingKitchen,
            kitchenTestResult = local.kitchenTestResult,
            receiptPrefs = receiptPrefs,
            terminalProvider = terminal.provider,
            terminalStatus = terminal.status,
            isConnectingTerminal = local.isConnectingTerminal,
            terminalError = local.terminalError,
            currentStaff = currentStaff,
            isClockingStaff = local.isClockingStaff,
            staffClockError = local.staffClockError,
            offlineSync = offlineSync,
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
            val isBluetooth = printerSettingsStore.kitchenPrinter.first().connectionType == KitchenPrinterConnectionType.BLUETOOTH
            val result = kitchenPrinterDispatcher.printIfConfigured(
                ReceiptBuilder.buildTestPrint(storeName),
                PrinterPaperSize.MM_58,
            )
            val message = when (result) {
                null -> if (isBluetooth) {
                    "Choose a paired Bluetooth printer and turn the kitchen printer on first."
                } else {
                    "Enter a host/IP and turn the kitchen printer on first."
                }
                true -> "Sent — check the kitchen printer."
                false -> if (isBluetooth) {
                    "Couldn't reach the Bluetooth printer. Check it's paired, powered on, and in range."
                } else {
                    "Couldn't reach the kitchen printer. Check the IP/port and that it's on the same network."
                }
            }
            localFlags.update { it.copy(isTestPrintingKitchen = false, kitchenTestResult = message) }
        }
    }

    /** Retries connecting to whichever provider Admin currently has selected — the till can't
     * switch providers itself, only Admin -> Settings -> Card Terminal can. */
    fun connectTerminal() {
        viewModelScope.launch {
            localFlags.update { it.copy(isConnectingTerminal = true, terminalError = null) }
            paymentTerminalService.connect()
                .onFailure { err -> localFlags.update { it.copy(terminalError = err.message) } }
            localFlags.update { it.copy(isConnectingTerminal = false) }
        }
    }

    /** Identifies the staff member by PIN alone (no name/username step here either, same as
     * Admin's kiosk) and toggles their shift — "in" updates the locally-remembered current
     * staff so new orders from this till get attributed to them, "out" clears it. */
    fun clockStaff(pin: String) {
        viewModelScope.launch {
            localFlags.update { it.copy(isClockingStaff = true, staffClockError = null) }
            staffRepository.clock(pin)
                .onSuccess { response ->
                    val staff = response.staff
                    if (response.action == "out" || staff == null) {
                        currentStaffStore.setCurrentStaff(null)
                    } else {
                        currentStaffStore.setCurrentStaff(CurrentStaff(staff.id, staff.name))
                    }
                }
                .onFailure { err -> localFlags.update { it.copy(staffClockError = err.message) } }
            localFlags.update { it.copy(isClockingStaff = false) }
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

    /** Explicit "Download for Offline Use" action — see OfflineSyncManager's doc comment for why
     * this is more than just re-running the menu's normal background refresh. */
    fun syncForOffline() {
        viewModelScope.launch { offlineSyncManager.syncForOffline() }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
