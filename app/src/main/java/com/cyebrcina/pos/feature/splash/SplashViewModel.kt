package com.cyebrcina.pos.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyebrcina.pos.data.remote.realtime.FireHutRealtimeManager
import com.cyebrcina.pos.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SplashEvent {
    data object NavigateToQueue : SplashEvent
    data object NavigateToLogin : SplashEvent
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val realtimeManager: FireHutRealtimeManager,
) : ViewModel() {

    private val _events = MutableSharedFlow<SplashEvent>()
    val events: SharedFlow<SplashEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            delay(500)
            val session = authRepository.session.first()
            val event = if (session != null) {
                realtimeManager.connect(session.token)
                SplashEvent.NavigateToQueue
            } else {
                SplashEvent.NavigateToLogin
            }
            _events.emit(event)
        }
    }
}
