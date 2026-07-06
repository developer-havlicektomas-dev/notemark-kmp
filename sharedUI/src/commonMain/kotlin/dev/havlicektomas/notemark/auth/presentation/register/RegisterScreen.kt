package dev.havlicektomas.notemark.auth.presentation.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkButton
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkPasswordTextField
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkTextField
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkTopAppBar
import dev.havlicektomas.notemark.core.presentation.designsystem.theme.NoteMarkTheme
import dev.havlicektomas.notemark.core.presentation.util.ObserveAsEvents
import dev.havlicektomas.notemark.core.presentation.util.UiText
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterRoot(
    onRegistered: (email: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var error by remember { mutableStateOf<UiText?>(null) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is RegisterEvent.RegistrationSuccess -> onRegistered(event.email)
            RegisterEvent.NavigateBack -> onNavigateBack()
            is RegisterEvent.ShowError -> error = event.message
        }
    }

    error?.let { uiText ->
        val message = uiText.asString()
        LaunchedEffect(uiText) {
            snackbarHostState.showSnackbar(message)
            error = null
        }
    }

    RegisterScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
    )
}

@Composable
fun RegisterScreen(
    state: RegisterState,
    snackbarHostState: SnackbarHostState,
    onAction: (RegisterAction) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            NoteMarkTopAppBar(
                title = "Create account",
                onNavigateBack = { onAction(RegisterAction.OnBackClick) },
            )
            Column(
                modifier = Modifier.padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                NoteMarkTextField(
                    value = state.username,
                    onValueChange = { onAction(RegisterAction.OnUsernameChange(it)) },
                    label = "Username",
                    helperText = "3 to 20 characters.",
                    errorText = "Username must be 3 to 20 characters."
                        .takeIf { state.username.isNotEmpty() && !state.isUsernameValid },
                )
                NoteMarkTextField(
                    value = state.email,
                    onValueChange = { onAction(RegisterAction.OnEmailChange(it)) },
                    label = "Email",
                    keyboardType = KeyboardType.Email,
                    errorText = "That doesn't look like a valid email."
                        .takeIf { state.email.isNotEmpty() && !state.isEmailValid },
                )
                NoteMarkPasswordTextField(
                    value = state.password,
                    onValueChange = { onAction(RegisterAction.OnPasswordChange(it)) },
                    label = "Password",
                    helperText = "At least 8 characters, with a number or symbol.",
                    errorText = "At least 8 characters, with a number or symbol."
                        .takeIf { state.password.isNotEmpty() && !state.passwordValidation.isValid },
                )
                NoteMarkPasswordTextField(
                    value = state.repeatPassword,
                    onValueChange = { onAction(RegisterAction.OnRepeatPasswordChange(it)) },
                    label = "Repeat password",
                    imeAction = ImeAction.Done,
                    errorText = "Passwords don't match."
                        .takeIf { state.repeatPassword.isNotEmpty() && !state.passwordsMatch },
                )
                Spacer(Modifier.height(4.dp))
                NoteMarkButton(
                    text = "Create account",
                    onClick = { onAction(RegisterAction.OnRegisterClick) },
                    enabled = state.canRegister,
                    isLoading = state.isRegistering,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Preview
@Composable
private fun RegisterScreenPreview() {
    NoteMarkTheme {
        RegisterScreen(
            state = RegisterState(username = "sam.rivera", email = "sam.rivera@email.com"),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}
