package dev.havlicektomas.notemark.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.havlicektomas.notemark.auth.domain.AuthRepository
import dev.havlicektomas.notemark.auth.domain.PasswordValidationState
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
import notemark.sharedui.generated.resources.error_account_exists

data class RegisterState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val repeatPassword: String = "",
    val isUsernameValid: Boolean = false,
    val isEmailValid: Boolean = false,
    val passwordValidation: PasswordValidationState = PasswordValidationState(),
    val isRegistering: Boolean = false,
) {
    val passwordsMatch: Boolean
        get() = password.isNotEmpty() && password == repeatPassword

    val canRegister: Boolean
        get() = isUsernameValid && isEmailValid && passwordValidation.isValid &&
            passwordsMatch && !isRegistering
}

sealed interface RegisterAction {
    data class OnUsernameChange(val value: String) : RegisterAction
    data class OnEmailChange(val value: String) : RegisterAction
    data class OnPasswordChange(val value: String) : RegisterAction
    data class OnRepeatPasswordChange(val value: String) : RegisterAction
    data object OnRegisterClick : RegisterAction
    data object OnBackClick : RegisterAction
}

sealed interface RegisterEvent {
    data class RegistrationSuccess(val email: String) : RegisterEvent
    data object NavigateBack : RegisterEvent
    data class ShowError(val message: UiText) : RegisterEvent
}

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val userDataValidator: UserDataValidator,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state = _state.asStateFlow()

    private val _events = Channel<RegisterEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: RegisterAction) {
        when (action) {
            is RegisterAction.OnUsernameChange -> _state.update {
                it.copy(
                    username = action.value,
                    isUsernameValid = userDataValidator.isValidUsername(action.value),
                )
            }
            is RegisterAction.OnEmailChange -> _state.update {
                it.copy(
                    email = action.value,
                    isEmailValid = userDataValidator.isValidEmail(action.value),
                )
            }
            is RegisterAction.OnPasswordChange -> _state.update {
                it.copy(
                    password = action.value,
                    passwordValidation = userDataValidator.validatePassword(action.value),
                )
            }
            is RegisterAction.OnRepeatPasswordChange -> _state.update {
                it.copy(repeatPassword = action.value)
            }
            RegisterAction.OnRegisterClick -> register()
            RegisterAction.OnBackClick -> viewModelScope.launch {
                _events.send(RegisterEvent.NavigateBack)
            }
        }
    }

    private fun register() {
        val current = _state.value
        if (!current.canRegister) return
        viewModelScope.launch {
            _state.update { it.copy(isRegistering = true) }
            authRepository.register(current.username, current.email, current.password)
                .onSuccess {
                    _state.update { it.copy(isRegistering = false) }
                    _events.send(RegisterEvent.RegistrationSuccess(current.email.trim()))
                }
                .onFailure { error ->
                    _state.update { it.copy(isRegistering = false) }
                    _events.send(RegisterEvent.ShowError(error.toRegisterUiText()))
                }
        }
    }
}

private fun DataError.Network.toRegisterUiText(): UiText = when (this) {
    DataError.Network.CONFLICT -> UiText.Resource(Res.string.error_account_exists)
    else -> toUiText()
}
