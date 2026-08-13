package com.cyebrcina.pos.feature.staffselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) {
    val selectedStaff: DeviceStaffMember? get() = staff.firstOrNull { it.id == selectedStaffId }
    val canConfirm: Boolean get() = selectedStaffId != null && pin.isNotEmpty() && !isVerifying
}

/** Backs the "Select Staff" screen shown once per app session, right after login — staff pick
 * their name from a grid then enter their PIN to confirm it's them. Identify-only: writes to
 * [CurrentStaffStore] (which NewOrderViewModel/RegisterViewModel already read for order/register
 * attribution) but never touches shift/clock status — that stays a separate action in Settings. */
@HiltViewModel
class SelectStaffViewModel @Inject constructor(
    private val staffRepository: StaffRepository,
    private val currentStaffStore: CurrentStaffStore,
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
    ) { loadState, selection, verify ->
        SelectStaffUiState(
            staff = loadState.first,
            isLoadingStaff = loadState.second,
            loadError = loadState.third,
            selectedStaffId = selection.first,
            pin = selection.second,
            isVerifying = verify.first,
            pinError = verify.second,
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
        val currentPin = pin.value
        if (currentPin.isEmpty() || isVerifying.value) return

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
