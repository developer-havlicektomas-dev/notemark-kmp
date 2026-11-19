package dev.havlicektomas.notemark.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.havlicektomas.notemark.auth.domain.AuthRepository
import dev.havlicektomas.notemark.auth.domain.UserDataValidator
import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.onFailure
import dev.havlicektomas.notemark.core.domain.util.onSuccess
import dev.havlicektomas.notemark.core.presentation.util.UiText
import dev.havlicektomas.notemark.core.presentation.util.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import notemark.sharedui.generated.resources.Res
import notemark.sharedui.generated.resources.error_invalid_credentials
import notemark.sharedui.generated.resources.error_secure_storage
import notemark.sharedui.generated.resources.error_unknown

data class LoginState(
    val email: String = "",
    val password: String = "",
    val canLogin: Boolean = false,
    val isLoggingIn: Boolean = false,
)

sealed interface LoginAction {
    data class OnEmailChange(val value: String) : LoginAction
    data class OnPasswordChange(val value: String) : LoginAction
    data object OnLoginClick : LoginAction
    data object OnRegisterClick : LoginAction
    data object OnBackClick : LoginAction
}

sealed interface LoginEvent {
    data object LoginSuccess : LoginEvent
    data object NavigateToRegister : LoginEvent
    data object NavigateBack : LoginEvent
    data class ShowError(val message: UiText) : LoginEvent
}

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val userDataValidator: UserDataValidator,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val _events = Channel<LoginEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnEmailChange -> _state.update {
                it.copy(email = action.value).withCanLogin()
            }
            is LoginAction.OnPasswordChange -> _state.update {
                it.copy(password = action.value).withCanLogin()
            }
            LoginAction.OnLoginClick -> login()
            LoginAction.OnRegisterClick -> sendEvent(LoginEvent.NavigateToRegister)
            LoginAction.OnBackClick -> sendEvent(LoginEvent.NavigateBack)
        }
    }

    private fun login() {
        val current = _state.value
        if (!current.canLogin || current.isLoggingIn) return
        viewModelScope.launch {
            _state.update { it.copy(isLoggingIn = true) }
            authRepository.login(current.email, current.password)
                .onSuccess {
                    _state.update { it.copy(isLoggingIn = false) }
                    _events.send(LoginEvent.LoginSuccess)
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoggingIn = false) }
                    _events.send(LoginEvent.ShowError(error.toLoginUiText()))
                }
        }
    }

    private fun sendEvent(event: LoginEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    private fun LoginState.withCanLogin(): LoginState = copy(
        canLogin = userDataValidator.isValidEmail(email) && password.isNotBlank(),
    )
}

private fun DataError.toLoginUiText(): UiText = when (this) {
    DataError.Network.UNAUTHORIZED -> UiText.Resource(Res.string.error_invalid_credentials)
    is DataError.Network -> toUiText()
    DataError.Local.SECURE_STORAGE -> UiText.Resource(Res.string.error_secure_storage)
    is DataError.Local -> UiText.Resource(Res.string.error_unknown)
}
