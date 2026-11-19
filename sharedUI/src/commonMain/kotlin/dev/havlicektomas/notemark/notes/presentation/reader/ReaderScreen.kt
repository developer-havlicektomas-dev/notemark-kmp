package dev.havlicektomas.notemark.notes.presentation.reader

import dev.havlicektomas.notemark.core.presentation.designsystem.layout.centeredMaxWidth
import dev.havlicektomas.notemark.core.presentation.designsystem.layout.ReadingMaxWidth
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkIcons
import dev.havlicektomas.notemark.core.presentation.designsystem.theme.NoteMarkTheme
import dev.havlicektomas.notemark.core.presentation.designsystem.theme.jetBrainsMonoFontFamily
import dev.havlicektomas.notemark.core.presentation.util.ObserveAsEvents
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private const val CONTROLS_AUTO_HIDE_MILLIS = 3_000L

@Composable
fun ReaderRoot(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ReaderEvent.NavigateBack -> onBack()
        }
    }

    ReaderScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ReaderScreen(
    state: ReaderState,
    onAction: (ReaderAction) -> Unit,
) {
    BackHandler(enabled = true) { onAction(ReaderAction.OnBackClick) }

    // Auto-hide the controls a few seconds after they appear.
    LaunchedEffect(state.controlsVisible) {
        if (state.controlsVisible) {
            delay(CONTROLS_AUTO_HIDE_MILLIS)
            onAction(ReaderAction.OnHideControls)
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Tapping anywhere toggles the controls; no ripple for a clean reading surface.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onAction(ReaderAction.OnTap) },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .centeredMaxWidth(ReadingMaxWidth)
                    .padding(horizontal = 30.dp)
                    .padding(top = 78.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = state.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.018).em,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.content.isNotBlank()) {
                    Text(
                        text = state.content,
                        fontSize = 18.sp,
                        lineHeight = 31.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            AnimatedVisibility(
                visible = state.controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                ReaderControlsBar(onBack = { onAction(ReaderAction.OnBackClick) })
            }

            AnimatedVisibility(
                visible = !state.controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp),
            ) {
                Text(
                    text = "Tap anywhere to show controls",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ReaderControlsBar(
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(48.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = NoteMarkIcons.ArrowLeft,
                contentDescription = "Exit reader mode",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "READER",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.04.em,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        // Typography control is visual only in M3.
        Text(
            text = "Aa",
            fontFamily = jetBrainsMonoFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ReaderScreenPreview() {
    NoteMarkTheme {
        ReaderScreen(
            state = ReaderState(
                title = "Weekly planning",
                content = "Block two hours on Monday morning for deep work — no meetings before 11.",
                controlsVisible = true,
            ),
            onAction = {},
        )
    }
}
