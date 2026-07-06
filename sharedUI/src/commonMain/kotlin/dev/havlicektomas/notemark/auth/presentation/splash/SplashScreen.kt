package dev.havlicektomas.notemark.auth.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkWordmark
import dev.havlicektomas.notemark.core.presentation.designsystem.theme.NoteMarkTheme
import dev.havlicektomas.notemark.core.presentation.util.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SplashRoot(
    onAuthenticated: () -> Unit,
    onUnauthenticated: () -> Unit,
    viewModel: SplashViewModel = koinViewModel(),
) {
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SplashEvent.SessionChecked ->
                if (event.isLoggedIn) onAuthenticated() else onUnauthenticated()
        }
    }
    SplashScreen()
}

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NoteMarkWordmark(fontSize = 34.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Your notes, in sync.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun SplashScreenPreview() {
    NoteMarkTheme {
        SplashScreen()
    }
}
