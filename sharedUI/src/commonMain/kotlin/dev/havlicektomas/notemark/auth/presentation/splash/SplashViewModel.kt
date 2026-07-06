package dev.havlicektomas.notemark.auth.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.havlicektomas.notemark.auth.domain.SessionStorage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface SplashEvent {
    data class SessionChecked(val isLoggedIn: Boolean) : SplashEvent
}

class SplashViewModel(
    private val sessionStorage: SessionStorage,
) : ViewModel() {

    private val _events = Channel<SplashEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val isLoggedIn = sessionStorage.get() != null
            _events.send(SplashEvent.SessionChecked(isLoggedIn))
        }
    }
}
