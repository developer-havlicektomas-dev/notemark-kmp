package dev.havlicektomas.notemark.notes.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteCard
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkIcons
import dev.havlicektomas.notemark.core.presentation.designsystem.components.OfflineBanner
import dev.havlicektomas.notemark.core.presentation.designsystem.components.UserAvatar
import dev.havlicektomas.notemark.core.presentation.designsystem.theme.NoteMarkTheme
import dev.havlicektomas.notemark.core.presentation.util.ObserveAsEvents
import dev.havlicektomas.notemark.core.presentation.util.UiText
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotesListRoot(
    onOpenNote: (id: String) -> Unit,
    onCreateNote: (id: String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: NotesListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var error by remember { mutableStateOf<UiText?>(null) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is NotesListEvent.OpenNote -> onOpenNote(event.id)
            is NotesListEvent.CreateNote -> onCreateNote(event.id)
            NotesListEvent.OpenSettings -> onOpenSettings()
            is NotesListEvent.ShowError -> error = event.message
        }
    }

    error?.let { uiText ->
        val message = uiText.asString()
        LaunchedEffect(uiText) {
            snackbarHostState.showSnackbar(message)
            error = null
        }
    }

    NotesListScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
    )
}

@Composable
fun NotesListScreen(
    state: NotesListState,
    snackbarHostState: SnackbarHostState,
    onAction: (NotesListAction) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.notes.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onAction(NotesListAction.OnCreateNote) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(NoteMarkIcons.Plus, contentDescription = "New note")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NotesTopBar(
                initials = state.initials,
                onAvatarClick = { onAction(NotesListAction.OnAvatarClick) },
            )

            if (!state.isOnline) {
                OfflineBanner(
                    text = "Offline — changes are saved on this device",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                )
            }

            if (state.notes.isEmpty()) {
                EmptyNotes(
                    onCreateNote = { onAction(NotesListAction.OnCreateNote) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.notes, key = { it.id }) { note ->
                        NoteCard(
                            date = note.formattedDate,
                            title = note.title,
                            preview = note.preview,
                            onClick = { onAction(NotesListAction.OnNoteClick(note.id)) },
                            onLongClick = { onAction(NotesListAction.OnNoteLongClick(note.id)) },
                        )
                    }
                }
            }
        }
    }

    state.notePendingDelete?.let {
        DeleteNoteDialog(
            onConfirm = { onAction(NotesListAction.OnConfirmDelete) },
            onDismiss = { onAction(NotesListAction.OnDismissDelete) },
        )
    }
}

@Composable
private fun NotesTopBar(
    initials: String,
    onAvatarClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "NoteMark",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        UserAvatar(initials = initials, onClick = onAvatarClick)
    }
}

@Composable
private fun EmptyNotes(
    onCreateNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = NoteMarkIcons.FileText,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "No notes yet",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your notes will show up here. Create your first one to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        ExtendedFloatingActionButton(
            onClick = onCreateNote,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(NoteMarkIcons.Plus, contentDescription = null) },
            text = { Text("New note") },
        )
    }
}

@Composable
private fun DeleteNoteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this note?") },
        text = { Text("This can't be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Preview
@Composable
private fun NotesListScreenPreview() {
    NoteMarkTheme {
        NotesListScreen(
            state = NotesListState(
                initials = "SR",
                notes = listOf(
                    NoteUi("1", "Weekly planning", "Block two hours Monday for deep work and review.", "12 Jun"),
                    NoteUi("2", "Groceries", "Milk, eggs, bread, coffee, and something nice.", "10 Jun"),
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun NotesListEmptyPreview() {
    NoteMarkTheme {
        NotesListScreen(
            state = NotesListState(initials = "SR"),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}
