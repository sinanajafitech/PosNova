package com.cyebrcina.pos.feature.order.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.data.remote.model.DeviceOrderType
import com.cyebrcina.pos.data.remote.model.PaymentLinkResponse
import com.cyebrcina.pos.data.repository.OrderRepository
import com.cyebrcina.pos.payment.PaymentTerminalService
import com.cyebrcina.pos.payment.model.CardChargeResult
import com.cyebrcina.pos.payment.model.PaymentProvider
import com.cyebrcina.pos.payment.model.TerminalStatus
import com.cyebrcina.pos.printer.PrinterService
import com.cyebrcina.pos.printer.ReceiptBuilder
import com.cyebrcina.pos.printer.model.PrintJobState
import com.cyebrcina.pos.printer.model.toPrintMode
import com.cyebrcina.pos.printer.model.toPrinterPaperSize
import com.cyebrcina.pos.printer.network.KitchenPrinterDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderDetailUiState(
    val order: DeviceOrder? = null,
    val isPending: Boolean = false,
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val showRejectConfirm: Boolean = false,
    val printJobState: PrintJobState = PrintJobState.IDLE,
    val printWarning: String? = null,
    val terminalStatus: TerminalStatus = TerminalStatus.DISCONNECTED,
    val paymentError: String? = null,
    val paymentQr: PaymentQrState = PaymentQrState(),
) {
    /** DINE_IN is the one order type this till might still need to collect payment for. */
    val canTakePayment: Boolean get() = order?.type == DeviceOrderType.DINE_IN
    val isChargingCard: Boolean
        get() = terminalStatus == TerminalStatus.CONNECTING || terminalStatus == TerminalStatus.AWAITING_CARD || terminalStatus == TerminalStatus.PROCESSING
}

data class PaymentQrState(
    val isLoading: Boolean = false,
    val link: PaymentLinkResponse? = null,
    val error: String? = null,
)

sealed interface OrderDetailEvent {
    data object Accepted : OrderDetailEvent
    data object Rejected : OrderDetailEvent
}

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
    private val printerService: PrinterService,
    private val paymentTerminalService: PaymentTerminalService,
    private val kitchenPrinterDispatcher: KitchenPrinterDispatcher,
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val loadedOrder = MutableStateFlow<DeviceOrder?>(null)
    private val isLoading = MutableStateFlow(true)
    private val isProcessing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val showRejectConfirm = MutableStateFlow(false)
    private val printJobState = MutableStateFlow(PrintJobState.IDLE)
    private val printWarning = MutableStateFlow<String?>(null)
    private val paymentError = MutableStateFlow<String?>(null)
    private val paymentQr = MutableStateFlow(PaymentQrState())

    private val _events = MutableSharedFlow<OrderDetailEvent>()
    val events: SharedFlow<OrderDetailEvent> = _events.asSharedFlow()

    private data class FlagsCore(
        val isLoading: Boolean,
        val isProcessing: Boolean,
        val errorMessage: String?,
        val showRejectConfirm: Boolean,
        val printJobState: PrintJobState,
    )

    private data class Flags(
        val isLoading: Boolean,
        val isProcessing: Boolean,
        val errorMessage: String?,
        val showRejectConfirm: Boolean,
        val printJobState: PrintJobState,
        val printWarning: String?,
        val paymentError: String?,
    )

    private val isPending = MutableStateFlow(false)

    private val flags = combine(
        combine(isLoading, isProcessing, errorMessage, showRejectConfirm, printJobState, ::FlagsCore),
        printWarning,
        paymentError,
    ) { core, warning, payError ->
        Flags(core.isLoading, core.isProcessing, core.errorMessage, core.showRejectConfirm, core.printJobState, warning, payError)
    }

    val uiState: StateFlow<OrderDetailUiState> = combine(
        loadedOrder,
        isPending,
        flags,
        paymentTerminalService.status,
        paymentQr,
    ) { order, pending, flags, terminalStatus, qr ->
        OrderDetailUiState(
            order = order,
            isPending = pending,
            isLoading = flags.isLoading,
            isProcessing = flags.isProcessing,
            errorMessage = flags.errorMessage,
            showRejectConfirm = flags.showRejectConfirm,
            printJobState = flags.printJobState,
            printWarning = flags.printWarning,
            terminalStatus = terminalStatus,
            paymentError = flags.paymentError,
            paymentQr = qr,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OrderDetailUiState())

    init {
        orderRepository.pendingOrders.onEach { list ->
            val match = list.firstOrNull { it.id == orderId }
            isPending.value = match != null
            if (match != null) loadedOrder.value = match
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            val fromPending = orderRepository.pendingOrders.value.firstOrNull { it.id == orderId }
            if (fromPending != null) {
                loadedOrder.value = fromPending
            } else {
                orderRepository.history(100).onSuccess { list ->
                    loadedOrder.value = list.firstOrNull { it.id == orderId }
                }
            }
            isLoading.value = false
        }
    }

    fun accept() {
        viewModelScope.launch {
            isProcessing.value = true
            errorMessage.value = null
            orderRepository.accept(orderId)
                .onSuccess { printReceiptAndTicket() }
                .onFailure { err -> errorMessage.value = err.message }
            isProcessing.value = false
            if (errorMessage.value == null) _events.emit(OrderDetailEvent.Accepted)
        }
    }

    fun onRejectRequested() = showRejectConfirm.update { true }
    fun onRejectDismissed() = showRejectConfirm.update { false }

    fun confirmReject() {
        viewModelScope.launch {
            isProcessing.value = true
            orderRepository.reject(orderId)
                .onSuccess {
                    showRejectConfirm.value = false
                    _events.emit(OrderDetailEvent.Rejected)
                }
                .onFailure { err -> errorMessage.value = err.message }
            isProcessing.value = false
        }
    }

    fun reprint() {
        viewModelScope.launch { printReceiptAndTicket() }
    }

    private suspend fun printReceiptAndTicket() {
        printJobState.value = PrintJobState.PRINTING
        val warnings = mutableListOf<String>()

        orderRepository.receiptData(orderId)
            .onSuccess { data ->
                printerService.setPaperSize(data.prefs?.paperSize.toPrinterPaperSize())
                printerService.setPrintMode(data.prefs?.printMode.toPrintMode())
                printerService.print(ReceiptBuilder.buildCustomerReceipt(data)).onFailure { warnings += "Receipt: ${it.message}" }
            }
            .onFailure { warnings += "Couldn't fetch receipt data: ${it.message}" }

        orderRepository.ticketData(orderId)
            .onSuccess { data ->
                val paperSize = data.prefs?.paperSize.toPrinterPaperSize()
                val printMode = data.prefs?.printMode.toPrintMode()
                val ticket = ReceiptBuilder.buildKitchenTicket(data)
                // A dedicated kitchen printer (configured in Settings) takes priority over the
                // main receipt printer — a kitchen physically apart from the till shouldn't need
                // someone to carry a paper ticket over from the counter.
                when (kitchenPrinterDispatcher.printIfConfigured(ticket, paperSize, printMode)) {
                    null -> {
                        printerService.setPaperSize(paperSize)
                        printerService.setPrintMode(printMode)
                        printerService.print(ticket).onFailure { warnings += "Ticket: ${it.message}" }
                    }
                    false -> warnings += "Ticket: couldn't reach the kitchen printer"
                    true -> Unit
                }
            }
            .onFailure { warnings += "Couldn't fetch ticket data: ${it.message}" }

        printJobState.value = if (warnings.isEmpty()) PrintJobState.SUCCESS else PrintJobState.FAILED
        printWarning.value = warnings.joinToString("; ").ifBlank { null }
    }

    fun chargeCard() {
        val order = loadedOrder.value ?: return
        viewModelScope.launch {
            paymentError.value = null
            paymentTerminalService.chargeCard(order.total, "GBP", order.number)
                .onSuccess { result -> recordCharge(order.id, order.total, result) }
                .onFailure { err -> paymentError.value = err.message ?: "Card payment failed" }
        }
    }

    fun cancelCharge() {
        viewModelScope.launch { paymentTerminalService.cancelCharge() }
    }

    fun requestPaymentQr() {
        viewModelScope.launch {
            paymentQr.value = PaymentQrState(isLoading = true)
            orderRepository.requestPaymentLink(orderId)
                .onSuccess { link -> paymentQr.value = PaymentQrState(link = link) }
                .onFailure { err -> paymentQr.value = PaymentQrState(error = err.message ?: "Couldn't create a payment QR") }
        }
    }

    fun dismissPaymentQr() {
        paymentQr.value = PaymentQrState()
    }

    private suspend fun recordCharge(orderId: String, amount: Double, result: CardChargeResult) {
        val provider = when (paymentTerminalService.provider) {
            PaymentProvider.MOCK -> "MOCK"
            PaymentProvider.STRIPE_TERMINAL -> "STRIPE"
            PaymentProvider.SUMUP -> "SUMUP"
            PaymentProvider.FLATPAY -> "FLATPAY"
            PaymentProvider.DOJO -> "DOJO"
            PaymentProvider.TEYA -> "TEYA"
        }
        orderRepository.chargeOrder(orderId, provider, amount, result)
            .onFailure { err ->
                // Expected until the backend implements POST /api/device/orders/{id}/charge —
                // see BACKEND_CARD_PAYMENT_SPEC.md. The terminal charge itself still succeeded.
                paymentError.value = "Card charged, but couldn't record it on the server yet: ${err.message}"
            }
    }
}
