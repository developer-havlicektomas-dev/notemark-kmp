package dev.havlicektomas.notemark

import androidx.compose.runtime.Composable
import dev.havlicektomas.notemark.core.presentation.designsystem.theme.NoteMarkTheme
import dev.havlicektomas.notemark.navigation.NavigationRoot

@Composable
fun App() {
    NoteMarkTheme {
        NavigationRoot()
    }
}
