package com.cyebrcina.pos.feature.order.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.customerdisplay.CustomerDisplayManager
import com.cyebrcina.pos.customerdisplay.CustomerDisplayLineItem
import com.cyebrcina.pos.customerdisplay.CustomerDisplayState
import com.cyebrcina.pos.data.model.DeviceSession
import com.cyebrcina.pos.data.remote.model.CreateOrderItemRequest
import com.cyebrcina.pos.data.remote.model.CreateOrderPayment
import com.cyebrcina.pos.data.remote.model.CreateOrderRequest
import com.cyebrcina.pos.data.remote.model.DeviceOrder
import com.cyebrcina.pos.data.remote.model.DeviceOrderType
import com.cyebrcina.pos.data.remote.model.MenuAddOn
import com.cyebrcina.pos.data.remote.model.MenuCategory
import com.cyebrcina.pos.data.remote.model.MenuProduct
import com.cyebrcina.pos.data.remote.model.MenuProductSize
import com.cyebrcina.pos.data.remote.model.PaymentLinkResponse
import com.cyebrcina.pos.data.repository.AuthRepository
import com.cyebrcina.pos.data.repository.MenuRepository
import com.cyebrcina.pos.data.repository.OrderRepository
import com.cyebrcina.pos.payment.PaymentTerminalService
import com.cyebrcina.pos.payment.model.PaymentProvider
import com.cyebrcina.pos.payment.model.PaymentSdkNotConfiguredException
import com.cyebrcina.pos.payment.model.TerminalStatus
import com.cyebrcina.pos.printer.PrinterService
import com.cyebrcina.pos.printer.ReceiptBuilder
import com.cyebrcina.pos.printer.model.toPrintMode
import com.cyebrcina.pos.printer.model.toPrinterPaperSize
import com.cyebrcina.pos.printer.network.KitchenPrinterDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TillPaymentMethod { CASH, CARD, QR }

/**
 * NOT YET LIVE server-side — see BACKEND_ORDER_CREATE_SPEC.md's "QR payment" section.
 * [order] is created unpaid; [link] is the scan-to-pay QR once fetched. Watched against
 * [OrderRepository.pendingOrders] for [DeviceOrder.paymentStatus] flipping to "PAID".
 */
data class QrPaymentState(
    val isGenerating: Boolean = false,
    val order: DeviceOrder? = null,
    val link: PaymentLinkResponse? = null,
    val error: String? = null,
)

data class NewOrderUiState(
    val categories: List<MenuCategory> = emptyList(),
    val addOns: List<MenuAddOn> = emptyList(),
    val isLoadingMenu: Boolean = true,
    val menuError: String? = null,
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val cart: List<CartItem> = emptyList(),
    val orderType: TillOrderType = TillOrderType.DINE_IN,
    val tableLabel: String? = null,
    val customerName: String = "Walk-in",
    val guestCount: Int = 1,
    val hasStartedOrder: Boolean = false,
    val detailProduct: MenuProduct? = null,
    val showChooseTable: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val paymentMethod: TillPaymentMethod = TillPaymentMethod.CASH,
    val cashTendered: String = "",
    val terminalStatus: TerminalStatus = TerminalStatus.DISCONNECTED,
    val cardChargeError: String? = null,
    // True only when the last charge attempt failed specifically because
    // the selected provider's SDK isn't wired up (never for a real
    // decline), and Admin's Card Terminal settings allow the fallback —
    // see NewOrderViewModel.chargeCard(). Offers "Record as Card
    // (manual)" instead of leaving staff stuck.
    val cardChargeSdkNotConfigured: Boolean = false,
    val completedOrder: DeviceOrder? = null,
    val printWarning: String? = null,
    val qrPayment: QrPaymentState = QrPaymentState(),
) {
    val subtotal: Double get() = cart.subtotal()
    val total: Double get() = subtotal
    val itemCount: Int get() = cart.sumOf { it.quantity }
    val visibleCategories: List<MenuCategory>
        get() = categories
            .let { list -> if (selectedCategoryId == null) list else list.filter { it.id == selectedCategoryId } }
            .map { category ->
                if (searchQuery.isBlank()) category else category.copy(
                    products = category.products.filter { it.name.contains(searchQuery, ignoreCase = true) },
                )
            }
    val canSubmit: Boolean
        get() = cart.isNotEmpty() && (orderType == TillOrderType.COLLECTION || !tableLabel.isNullOrBlank())
    val cashTenderedAmount: Double get() = cashTendered.toDoubleOrNull() ?: 0.0
    val change: Double get() = (cashTenderedAmount - total).coerceAtLeast(0.0)
    val canConfirmCashPayment: Boolean get() = cashTenderedAmount >= total
}

@HiltViewModel
class NewOrderViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository,
    private val printerService: PrinterService,
    val paymentTerminalService: PaymentTerminalService,
    private val authRepository: AuthRepository,
    private val customerDisplayManager: CustomerDisplayManager,
    private val heldOrdersStore: HeldOrdersStore,
    private val entryCoordinator: NewOrderEntryCoordinator,
    private val kitchenPrinterDispatcher: KitchenPrinterDispatcher,
) : ViewModel() {

    private val session = MutableStateFlow<DeviceSession?>(null)
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val cart = MutableStateFlow<List<CartItem>>(emptyList())
    private val orderType = MutableStateFlow(TillOrderType.DINE_IN)
    private val tableLabel = MutableStateFlow<String?>(null)
    private val customerName = MutableStateFlow("Walk-in")
    private val guestCount = MutableStateFlow(1)
    private val hasStartedOrder = MutableStateFlow(false)
    private val detailProduct = MutableStateFlow<MenuProduct?>(null)
    private val showChooseTable = MutableStateFlow(false)
    private val isLoadingMenu = MutableStateFlow(true)
    private val menuError = MutableStateFlow<String?>(null)
    private val isSubmitting = MutableStateFlow(false)
    private val submitError = MutableStateFlow<String?>(null)
    private val paymentMethod = MutableStateFlow(TillPaymentMethod.CASH)
    private val cashTendered = MutableStateFlow("")
    private val cardChargeError = MutableStateFlow<String?>(null)
    private val cardChargeSdkNotConfigured = MutableStateFlow(false)
    private val completedOrder = MutableStateFlow<DeviceOrder?>(null)
    private val printWarning = MutableStateFlow<String?>(null)
    private val qrPayment = MutableStateFlow(QrPaymentState())

    private data class ConfigFlags(
        val selectedCategoryId: String?,
        val searchQuery: String,
        val cart: List<CartItem>,
        val orderType: TillOrderType,
        val tableLabel: String?,
        val customerName: String,
        val guestCount: Int,
    )

    private data class ConfigCore(
        val selectedCategoryId: String?,
        val searchQuery: String,
        val cart: List<CartItem>,
        val orderType: TillOrderType,
        val tableLabel: String?,
    )

    private data class UiCore(
        val hasStartedOrder: Boolean,
        val detailProduct: MenuProduct?,
        val showChooseTable: Boolean,
        val isLoadingMenu: Boolean,
        val menuError: String?,
    )

    private data class UiFlags(
        val hasStartedOrder: Boolean,
        val detailProduct: MenuProduct?,
        val showChooseTable: Boolean,
        val isLoadingMenu: Boolean,
        val menuError: String?,
        val isSubmitting: Boolean,
        val submitError: String?,
    )

    private data class CheckoutCore(
        val paymentMethod: TillPaymentMethod,
        val cashTendered: String,
        val cardChargeError: String?,
        val completedOrder: DeviceOrder?,
        val printWarning: String?,
    )

    private data class CheckoutFlags(
        val paymentMethod: TillPaymentMethod,
        val cashTendered: String,
        val cardChargeError: String?,
        val completedOrder: DeviceOrder?,
        val printWarning: String?,
        val qrPayment: QrPaymentState,
    )

    // combine()'s typed overloads max out at 5 flows, so >5-flow groups are nested.
    private val configFlags = combine(
        combine(selectedCategoryId, searchQuery, cart, orderType, tableLabel, ::ConfigCore),
        customerName,
        guestCount,
    ) { core, name, guests -> ConfigFlags(core.selectedCategoryId, core.searchQuery, core.cart, core.orderType, core.tableLabel, name, guests) }
    private val uiFlags = combine(
        combine(hasStartedOrder, detailProduct, showChooseTable, isLoadingMenu, menuError, ::UiCore),
        isSubmitting,
        submitError,
    ) { core, submitting, error -> UiFlags(core.hasStartedOrder, core.detailProduct, core.showChooseTable, core.isLoadingMenu, core.menuError, submitting, error) }
    private val checkoutFlags = combine(
        combine(paymentMethod, cashTendered, cardChargeError, completedOrder, printWarning, ::CheckoutCore),
        qrPayment,
    ) { core, qr -> CheckoutFlags(core.paymentMethod, core.cashTendered, core.cardChargeError, core.completedOrder, core.printWarning, qr) }

    val uiState: StateFlow<NewOrderUiState> = combine(
        menuRepository.categories,
        menuRepository.addOns,
        combine(configFlags, uiFlags, checkoutFlags, paymentTerminalService.status) { config, ui, checkout, terminal ->
            NewOrderUiState(
                categories = emptyList(), // filled in below from menuRepository.categories directly
                addOns = emptyList(),
                isLoadingMenu = ui.isLoadingMenu,
                menuError = ui.menuError,
                selectedCategoryId = config.selectedCategoryId,
                searchQuery = config.searchQuery,
                cart = config.cart,
                orderType = config.orderType,
                tableLabel = config.tableLabel,
                customerName = config.customerName,
                guestCount = config.guestCount,
                hasStartedOrder = ui.hasStartedOrder,
                detailProduct = ui.detailProduct,
                showChooseTable = ui.showChooseTable,
                isSubmitting = ui.isSubmitting,
                submitError = ui.submitError,
                paymentMethod = checkout.paymentMethod,
                cashTendered = checkout.cashTendered,
                terminalStatus = terminal,
                cardChargeError = checkout.cardChargeError,
                completedOrder = checkout.completedOrder,
                printWarning = checkout.printWarning,
                qrPayment = checkout.qrPayment,
            )
        },
        cardChargeSdkNotConfigured,
    ) { categories, addOns, partial, sdkNotConfigured ->
        partial.copy(categories = categories, addOns = addOns, cardChargeSdkNotConfigured = sdkNotConfigured)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NewOrderUiState())

    init {
        viewModelScope.launch {
            isLoadingMenu.value = true
            menuRepository.refresh().onFailure { menuError.value = it.message }
            isLoadingMenu.value = false
        }

        authRepository.session.onEach { session.value = it }.launchIn(viewModelScope)

        // Mirrors the cart on the customer-facing display while staff are actively building an
        // order; other screens (e.g. Order Queue) own the display outside this flow.
        combine(cart, hasStartedOrder) { cartItems, started -> cartItems to started }
            .onEach { (cartItems, started) ->
                if (started) {
                    customerDisplayManager.update(
                        CustomerDisplayState.BuildingOrder(
                            items = cartItems.map { CustomerDisplayLineItem(it.product.name, it.quantity, it.lineTotal) },
                            total = cartItems.subtotal(),
                        ),
                    )
                }
            }.launchIn(viewModelScope)

        when (val intent = entryCoordinator.consumePending()) {
            is NewOrderEntryIntent.StartAtTable -> startOrder(TillOrderType.DINE_IN, "Walk-in", 1, intent.table)
            is NewOrderEntryIntent.ResumeHeldOrder -> resumeHeldOrder(intent.heldOrderId)
            null -> Unit
        }

        // Watches for a QR-created order's paymentStatus flipping to "PAID" — kept fresh by the
        // repository's existing 15s poll + order_payment_updated socket trigger, no new realtime
        // handling needed here. See BACKEND_ORDER_CREATE_SPEC.md's "QR payment" section — this is
        // inert until the server actually sends paymentStatus.
        orderRepository.pendingOrders.onEach { orders ->
            val pendingQrOrder = qrPayment.value.order ?: return@onEach
            val updated = orders.firstOrNull { it.id == pendingQrOrder.id } ?: return@onEach
            if (updated.paymentStatus == "PAID") {
                qrPayment.value = QrPaymentState()
                completedOrder.value = updated
                customerDisplayManager.update(
                    CustomerDisplayState.NewOrderReceived(updated.number),
                )
                printReceiptAndTicket(updated.id)
            }
        }.launchIn(viewModelScope)
    }

    fun refreshMenu() {
        viewModelScope.launch {
            isLoadingMenu.value = true
            menuRepository.refresh().onFailure { menuError.value = it.message }
            isLoadingMenu.value = false
        }
    }

    fun onCategorySelected(categoryId: String?) = selectedCategoryId.update { categoryId }
    fun onSearchQueryChange(query: String) = searchQuery.update { query }

    fun startOrder(type: TillOrderType, name: String, guests: Int, table: String?) {
        orderType.value = type
        customerName.value = name.ifBlank { "Walk-in" }
        guestCount.value = guests.coerceAtLeast(1)
        tableLabel.value = table
        hasStartedOrder.value = true
    }

    fun onChooseTableRequested() = showChooseTable.update { true }
    fun onChooseTableDismissed() = showChooseTable.update { false }
    fun onTableSelected(table: String) {
        tableLabel.value = table
        showChooseTable.value = false
    }

    fun onOrderTypeChanged(type: TillOrderType) {
        orderType.value = type
        if (type == TillOrderType.COLLECTION) tableLabel.value = null
    }

    fun onProductTapped(product: MenuProduct) = detailProduct.update { product }
    fun onDetailDismissed() = detailProduct.update { null }

    fun addToCart(product: MenuProduct, size: MenuProductSize?, addOns: List<MenuAddOn>, quantity: Int, notes: String?) {
        cart.update { it + CartItem(product = product, size = size, addOns = addOns, quantity = quantity, notes = notes) }
        detailProduct.value = null
    }

    fun incrementLine(lineId: String) = cart.update { list ->
        list.map { if (it.lineId == lineId) it.copy(quantity = it.quantity + 1) else it }
    }

    fun decrementLine(lineId: String) = cart.update { list ->
        list.mapNotNull { item ->
            if (item.lineId != lineId) return@mapNotNull item
            if (item.quantity <= 1) null else item.copy(quantity = item.quantity - 1)
        }
    }

    fun removeLine(lineId: String) = cart.update { list -> list.filter { it.lineId != lineId } }

    fun onPaymentMethodChanged(method: TillPaymentMethod) {
        paymentMethod.value = method
        cardChargeError.value = null
        cardChargeSdkNotConfigured.value = false
    }

    fun onCashTenderedChange(value: String) = cashTendered.update { value }

    fun chargeCard() {
        val state = uiState.value
        viewModelScope.launch {
            cardChargeError.value = null
            cardChargeSdkNotConfigured.value = false
            paymentTerminalService.chargeCard(state.total, "GBP", "TILL-${System.currentTimeMillis()}")
                .onSuccess { result ->
                    submitOrder(
                        CreateOrderPayment(
                            method = "CARD",
                            amount = state.total,
                            provider = paymentTerminalService.provider.toApiName(),
                            cardBrand = result.cardBrand,
                            cardLast4 = result.cardLast4,
                            terminalReference = result.transactionReference,
                        ),
                    )
                }
                .onFailure { err ->
                    cardChargeError.value = err.message ?: "Card payment failed"
                    // Only offer the manual fallback for "not configured yet" — never for a
                    // real decline/timeout from a provider that's actually wired up, and only
                    // when Admin's Card Terminal settings allow it.
                    if (err is PaymentSdkNotConfiguredException && orderRepository.cardTerminal.value?.manualFallbackAllowed != false) {
                        cardChargeSdkNotConfigured.value = true
                    }
                }
        }
    }

    fun cancelCardCharge() {
        viewModelScope.launch { paymentTerminalService.cancelCharge() }
    }

    /** No real terminal call — records a plain Card payment with no provider/brand/last4/
     * reference, same shape as how an unconfigured PSP already works server-side. Only ever
     * reachable when [NewOrderUiState.cardChargeSdkNotConfigured] is true, i.e. the last attempt
     * failed specifically because nothing's configured, not from a real decline. */
    fun submitManualCardPayment() {
        val state = uiState.value
        viewModelScope.launch {
            cardChargeError.value = null
            cardChargeSdkNotConfigured.value = false
            submitOrder(CreateOrderPayment(method = "CARD", amount = state.total))
        }
    }

    fun confirmCashPayment() {
        val state = uiState.value
        viewModelScope.launch {
            submitOrder(CreateOrderPayment(method = "CASH", amount = state.total, cashTendered = state.cashTenderedAmount))
        }
    }

    /**
     * NOT YET LIVE server-side — see BACKEND_ORDER_CREATE_SPEC.md's "QR payment" section. Creates
     * the order unpaid (`payment.method: "QR"`), then requests its scan-to-pay QR. Both calls will
     * genuinely fail until the backend implements create-order and its QR extension.
     */
    fun generateQrPayment() {
        val state = uiState.value
        viewModelScope.launch {
            qrPayment.value = QrPaymentState(isGenerating = true)

            val request = CreateOrderRequest(
                type = if (state.orderType == TillOrderType.DINE_IN) DeviceOrderType.DINE_IN else DeviceOrderType.COLLECTION,
                tableLabel = state.tableLabel,
                customerName = state.customerName,
                items = state.cart.map { item ->
                    CreateOrderItemRequest(
                        productId = item.product.id,
                        sizeId = item.size?.id,
                        quantity = item.quantity,
                        addOnIds = item.addOns.map { it.id },
                        notes = item.notes,
                    )
                },
                payment = CreateOrderPayment(method = "QR", amount = state.total),
            )

            orderRepository.createOrder(request)
                .onSuccess { order -> fetchPaymentLink(order) }
                .onFailure { err -> qrPayment.value = QrPaymentState(error = err.message ?: "Couldn't create the order") }
        }
    }

    /** Retries just the QR fetch (e.g. Stripe wasn't configured a moment ago) without re-creating the order. */
    fun retryPaymentLink() {
        val order = qrPayment.value.order ?: return
        viewModelScope.launch { fetchPaymentLink(order) }
    }

    private suspend fun fetchPaymentLink(order: DeviceOrder) {
        qrPayment.value = QrPaymentState(isGenerating = true, order = order)
        orderRepository.requestPaymentLink(order.id)
            .onSuccess { link -> qrPayment.value = QrPaymentState(order = order, link = link) }
            .onFailure { err -> qrPayment.value = QrPaymentState(order = order, error = err.message ?: "Couldn't create a payment QR") }
    }

    fun cancelQrPayment() {
        qrPayment.value = QrPaymentState()
    }

    private suspend fun submitOrder(payment: CreateOrderPayment) {
        val state = uiState.value
        isSubmitting.value = true
        submitError.value = null

        val request = CreateOrderRequest(
            type = if (state.orderType == TillOrderType.DINE_IN) DeviceOrderType.DINE_IN else DeviceOrderType.COLLECTION,
            tableLabel = state.tableLabel,
            customerName = state.customerName,
            items = state.cart.map { item ->
                CreateOrderItemRequest(
                    productId = item.product.id,
                    sizeId = item.size?.id,
                    quantity = item.quantity,
                    addOnIds = item.addOns.map { it.id },
                    notes = item.notes,
                )
            },
            payment = payment,
        )

        orderRepository.createOrder(request)
            .onSuccess { order ->
                completedOrder.value = order
                customerDisplayManager.update(
                    CustomerDisplayState.NewOrderReceived(order.number),
                )
                printReceiptAndTicket(order.id)
            }
            .onFailure { err -> submitError.value = err.message ?: "Couldn't submit the order" }

        isSubmitting.value = false
    }

    private suspend fun printReceiptAndTicket(orderId: String) {
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
        printWarning.value = warnings.joinToString("; ").ifBlank { null }
    }

    fun reprintCompletedOrder() {
        val order = completedOrder.value ?: return
        viewModelScope.launch { printReceiptAndTicket(order.id) }
    }

    /** Parks the current cart so staff can serve someone else, then clears the draft for reuse. */
    fun holdCurrentOrder() {
        val state = uiState.value
        if (state.cart.isEmpty()) return
        heldOrdersStore.hold(
            HeldOrder(
                orderType = state.orderType,
                tableLabel = state.tableLabel,
                customerName = state.customerName,
                guestCount = state.guestCount,
                cart = state.cart,
            ),
        )
        startNewOrder()
    }

    private fun resumeHeldOrder(id: String) {
        val held = heldOrdersStore.remove(id) ?: return
        cart.value = held.cart
        orderType.value = held.orderType
        tableLabel.value = held.tableLabel
        customerName.value = held.customerName
        guestCount.value = held.guestCount
        hasStartedOrder.value = true
    }

    /** Resets the whole flow so the shared ViewModel can be reused for the next order without re-navigating. */
    fun startNewOrder() {
        cart.value = emptyList()
        hasStartedOrder.value = false
        tableLabel.value = null
        customerName.value = "Walk-in"
        guestCount.value = 1
        orderType.value = TillOrderType.DINE_IN
        paymentMethod.value = TillPaymentMethod.CASH
        cashTendered.value = ""
        cardChargeError.value = null
        completedOrder.value = null
        printWarning.value = null
        submitError.value = null
        qrPayment.value = QrPaymentState()
        selectedCategoryId.value = null
        searchQuery.value = ""
        customerDisplayManager.update(CustomerDisplayState.Idle)
    }

    private fun PaymentProvider.toApiName(): String = when (this) {
        PaymentProvider.MOCK -> "MOCK"
        PaymentProvider.STRIPE_TERMINAL -> "STRIPE"
        PaymentProvider.SUMUP -> "SUMUP"
        PaymentProvider.FLATPAY -> "FLATPAY"
        PaymentProvider.DOJO -> "DOJO"
        PaymentProvider.TEYA -> "TEYA"
    }
}
