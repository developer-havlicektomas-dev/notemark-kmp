package dev.havlicektomas.notemark.auth.presentation.landing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkButton
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkTextButton
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkWordmark
import dev.havlicektomas.notemark.core.presentation.designsystem.theme.NoteMarkTheme

@Composable
fun LandingRoot(
    onGetStarted: () -> Unit,
    onLogIn: () -> Unit,
) {
    LandingScreen(
        onGetStarted = onGetStarted,
        onLogIn = onLogIn,
    )
}

@Composable
fun LandingScreen(
    onGetStarted: () -> Unit,
    onLogIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            NoteMarkWordmark(fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Your notes, everywhere you are.",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Write freely. NoteMark keeps everything on your " +
                    "device and in sync — even offline.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NoteMarkButton(text = "Get started", onClick = onGetStarted)
            NoteMarkTextButton(text = "Log in", onClick = onLogIn)
        }
    }
}

@Preview
@Composable
private fun LandingScreenPreview() {
    NoteMarkTheme {
        LandingScreen(onGetStarted = {}, onLogIn = {})
    }
}
