package dev.havlicektomas.notemark.auth.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
fun LoginRoot(
    onLoggedIn: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var error by remember { mutableStateOf<UiText?>(null) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            LoginEvent.LoginSuccess -> onLoggedIn()
            LoginEvent.NavigateToRegister -> onNavigateToRegister()
            LoginEvent.NavigateBack -> onNavigateBack()
            is LoginEvent.ShowError -> error = event.message
        }
    }

    error?.let { uiText ->
        val message = uiText.asString()
        LaunchedEffect(uiText) {
            snackbarHostState.showSnackbar(message)
            error = null
        }
    }

    LoginScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
    )
}

@Composable
fun LoginScreen(
    state: LoginState,
    snackbarHostState: SnackbarHostState,
    onAction: (LoginAction) -> Unit,
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
                title = "Log in",
                onNavigateBack = { onAction(LoginAction.OnBackClick) },
            )
            Column(
                modifier = Modifier.padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Log in to pick up where you left off.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NoteMarkTextField(
                    value = state.email,
                    onValueChange = { onAction(LoginAction.OnEmailChange(it)) },
                    label = "Email",
                    keyboardType = KeyboardType.Email,
                )
                NoteMarkPasswordTextField(
                    value = state.password,
                    onValueChange = { onAction(LoginAction.OnPasswordChange(it)) },
                    label = "Password",
                    imeAction = ImeAction.Done,
                )
                Spacer(Modifier.height(4.dp))
                NoteMarkButton(
                    text = "Log in",
                    onClick = { onAction(LoginAction.OnLoginClick) },
                    enabled = state.canLogin,
                    isLoading = state.isLoggingIn,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "No account yet? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Create one",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onAction(LoginAction.OnRegisterClick) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    NoteMarkTheme {
        LoginScreen(
            state = LoginState(email = "sam.rivera@email.com", password = "secret", canLogin = true),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}
