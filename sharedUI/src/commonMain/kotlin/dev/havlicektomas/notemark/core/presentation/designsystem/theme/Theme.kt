package dev.havlicektomas.notemark.core.presentation.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NoteMarkLightColors = lightColorScheme(
    primary = Green500,
    onPrimary = White,
    primaryContainer = Green50,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Green600,
    onSecondary = White,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
)

@Composable
fun NoteMarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NoteMarkLightColors,
        typography = noteMarkTypography(),
        content = content,
    )
}
