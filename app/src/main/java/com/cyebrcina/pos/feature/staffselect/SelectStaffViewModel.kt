package com.cyebrcina.pos.feature.staffselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.local.ConnectivityObserver
import com.cyebrcina.pos.data.local.CurrentStaff
import com.cyebrcina.pos.data.local.CurrentStaffStore
import com.cyebrcina.pos.data.remote.model.DeviceStaffMember
import com.cyebrcina.pos.data.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_PIN_LENGTH = 8

data class SelectStaffUiState(
    val staff: List<DeviceStaffMember> = emptyList(),
    val isLoadingStaff: Boolean = true,
    val loadError: String? = null,
    val selectedStaffId: String? = null,
    val pin: String = "",
    val isVerifying: Boolean = false,
    val pinError: String? = null,
    // See ConnectivityObserver. When true, PIN verification is skipped entirely — see
    // SelectStaffViewModel's class doc for why that's an acceptable tradeoff here.
    val isOnline: Boolean = true,
) {
    val selectedStaff: DeviceStaffMember? get() = staff.firstOrNull { it.id == selectedStaffId }
    val canConfirm: Boolean
        get() = selectedStaffId != null && !isVerifying && (isOnline.not() || pin.isNotEmpty())
}

/**
 * Backs the "Select Staff" screen shown once per app session, right after login — staff pick
 * their name from a grid then enter their PIN to confirm it's them. Identify-only: writes to
 * [CurrentStaffStore] (which NewOrderViewModel/RegisterViewModel already read for order/register
 * attribution) but never touches shift/clock status — that stays a separate action in Settings.
 *
 * **Offline**: PIN verification needs a live call to `POST /api/device/staff/verify-pin` (the
 * PIN hash never leaves the server) — with no connection, that can't happen at all, and this
 * screen sits between login and every other screen in the app, so hard-blocking it here would
 * make the whole till unusable exactly when the offline order queue (see
 * OrderRepository.CreateOrderResult.Queued) is supposed to keep it working. Since this screen
 * only ever gates order/register *attribution*, never money or permissions (refunds/voids stay
 * behind their own always-online, server-verified manager-PIN gate in Admin — see
 * `requireRefundApprover`), the PIN step is skipped entirely while offline: tapping a name from
 * the cached roster (see StaffCacheStore) is enough to continue. A wrong "who's actually at the
 * till" attribution for a shift is a much smaller problem than a till that can't ring up orders
 * during an outage.
 */
@HiltViewModel
class SelectStaffViewModel @Inject constructor(
    private val staffRepository: StaffRepository,
    private val currentStaffStore: CurrentStaffStore,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val staff = MutableStateFlow<List<DeviceStaffMember>>(emptyList())
    private val isLoadingStaff = MutableStateFlow(true)
    private val loadError = MutableStateFlow<String?>(null)
    private val selectedStaffId = MutableStateFlow<String?>(null)
    private val pin = MutableStateFlow("")
    private val isVerifying = MutableStateFlow(false)
    private val pinError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SelectStaffUiState> = combine(
        combine(staff, isLoadingStaff, loadError, ::Triple),
        combine(selectedStaffId, pin, ::Pair),
        combine(isVerifying, pinError, ::Pair),
        connectivityObserver.isOnline,
    ) { loadState, selection, verify, isOnline ->
        SelectStaffUiState(
            staff = loadState.first,
            isLoadingStaff = loadState.second,
            loadError = loadState.third,
            selectedStaffId = selection.first,
            pin = selection.second,
            isVerifying = verify.first,
            pinError = verify.second,
            isOnline = isOnline,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SelectStaffUiState())

    init {
        loadStaff()
    }

    fun loadStaff() {
        viewModelScope.launch {
            isLoadingStaff.value = true
            staffRepository.list()
                .onSuccess { loadError.value = null; staff.value = it }
                .onFailure { loadError.value = it.message }
            isLoadingStaff.value = false
        }
    }

    fun onStaffSelected(staffId: String) {
        selectedStaffId.value = staffId
        pin.value = ""
        pinError.value = null
    }

    fun onDigit(digit: Char) {
        if (pin.value.length < MAX_PIN_LENGTH) {
            pin.update { it + digit }
            pinError.value = null
        }
    }

    fun onBackspace() {
        pin.update { it.dropLast(1) }
        pinError.value = null
    }

    /** [onConfirmed] is called once identity is confirmed and [CurrentStaffStore] is written —
     * the caller (the screen) does the actual navigation to the dashboard. */
    fun onConfirm(onConfirmed: () -> Unit) {
        val staffId = selectedStaffId.value ?: return
        if (isVerifying.value) return

        // Offline: no PIN check is possible at all (see this class's doc comment) — the cached
        // roster entry is trusted as-is.
        if (!connectivityObserver.isOnline.value) {
            val selected = staff.value.firstOrNull { it.id == staffId } ?: return
            viewModelScope.launch {
                currentStaffStore.setCurrentStaff(CurrentStaff(selected.id, selected.name))
                onConfirmed()
            }
            return
        }

        val currentPin = pin.value
        if (currentPin.isEmpty()) return

        viewModelScope.launch {
            isVerifying.value = true
            pinError.value = null
            staffRepository.verifyPin(staffId, currentPin)
                .onSuccess { response ->
                    val confirmedStaff = response.staff
                    if (response.ok && confirmedStaff != null) {
                        currentStaffStore.setCurrentStaff(CurrentStaff(confirmedStaff.id, confirmedStaff.name))
                        onConfirmed()
                    } else {
                        pinError.value = "Incorrect PIN."
                        pin.value = ""
                    }
                }
                .onFailure { err ->
                    pinError.value = err.message
                    pin.value = ""
                }
            isVerifying.value = false
        }
    }
}
