package com.cyebrcina.pos.feature.waitercall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.core.util.WaiterCallAlertPlayer
import com.cyebrcina.pos.data.remote.realtime.FireHutRealtimeManager
import com.cyebrcina.pos.data.remote.realtime.WaiterCallEvent
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

/**
 * Owns the queue of pending "Call Waiter" popups — a call is queued (not
 * dropped) if another popup is already showing, so two calls arriving
 * close together both still get acknowledged individually rather than the
 * second silently overwriting the first. Lives for as long as
 * [com.cyebrcina.pos.core.navigation.MainGraphHost] does, so it keeps
 * queuing even while staff are on a different tab than the popup's host.
 */
@HiltViewModel
class WaiterCallViewModel @Inject constructor(
    realtimeManager: FireHutRealtimeManager,
    private val alertPlayer: WaiterCallAlertPlayer,
) : ViewModel() {

    private val queue = MutableStateFlow<List<WaiterCallEvent>>(emptyList())

    val current: StateFlow<WaiterCallEvent?> = queue
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        realtimeManager.waiterCallEvents
            .onEach { event ->
                queue.update { it + event }
                alertPlayer.play()
            }
            .launchIn(viewModelScope)
    }

    fun acknowledgeCurrent() {
        queue.update { if (it.isEmpty()) it else it.drop(1) }
    }
}
