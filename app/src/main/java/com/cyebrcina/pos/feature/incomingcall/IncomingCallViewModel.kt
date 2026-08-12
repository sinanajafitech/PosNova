package com.cyebrcina.pos.feature.incomingcall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.core.util.IncomingCallAlertPlayer
import com.cyebrcina.pos.data.remote.realtime.FireHutRealtimeManager
import com.cyebrcina.pos.data.remote.realtime.IncomingCallEvent
import com.cyebrcina.pos.data.repository.PhoneContactRepository
import com.cyebrcina.pos.feature.order.create.NewOrderEntryCoordinator
import com.cyebrcina.pos.feature.order.create.NewOrderEntryIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns the queue of pending Incoming Call popups — mirrors
 * [com.cyebrcina.pos.feature.waitercall.WaiterCallViewModel]'s
 * queue-not-drop shape so overlapping calls are each acknowledged
 * individually. */
@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    realtimeManager: FireHutRealtimeManager,
    private val alertPlayer: IncomingCallAlertPlayer,
    private val phoneContactRepository: PhoneContactRepository,
    private val entryCoordinator: NewOrderEntryCoordinator,
) : ViewModel() {

    private val queue = MutableStateFlow<List<IncomingCallEvent>>(emptyList())

    val current: StateFlow<IncomingCallEvent?> = queue
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    init {
        realtimeManager.incomingCallEvents
            .onEach { event ->
                queue.update { it + event }
                alertPlayer.play()
            }
            .launchIn(viewModelScope)
    }

    fun dismissCurrent() {
        queue.update { if (it.isEmpty()) it else it.drop(1) }
    }

    /** "Take Order" — hands the caller's details to the New Order flow via
     * [NewOrderEntryCoordinator] (same one-shot handoff [com.cyebrcina.pos.feature.order.create.TablesViewModel]
     * uses for starting an order at a table). The actual navigation is done by the caller
     * ([IncomingCallOverlay]'s `onTakeOrder`), since this ViewModel has no NavController. */
    fun takeOrder() {
        val call = queue.value.firstOrNull() ?: return
        entryCoordinator.setPending(NewOrderEntryIntent.StartFromCall(call.callerName, call.phone, call.callLogId))
        dismissCurrent()
    }

    fun saveContact(name: String, address: String, notes: String) {
        val phone = queue.value.firstOrNull()?.phone ?: return
        viewModelScope.launch {
            _isSaving.value = true
            phoneContactRepository.saveContact(phone, name, address, notes)
            _isSaving.value = false
            dismissCurrent()
        }
    }
}
