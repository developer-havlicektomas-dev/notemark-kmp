package dev.havlicektomas.notemark.notes.presentation.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkIcons
import dev.havlicektomas.notemark.core.presentation.designsystem.components.OfflineBanner
import dev.havlicektomas.notemark.core.presentation.designsystem.theme.NoteMarkTheme
import dev.havlicektomas.notemark.core.presentation.designsystem.theme.OnWarningContainer
import dev.havlicektomas.notemark.core.presentation.designsystem.theme.jetBrainsMonoFontFamily
import dev.havlicektomas.notemark.core.presentation.util.ObserveAsEvents
import dev.havlicektomas.notemark.core.presentation.util.UiText
import dev.havlicektomas.notemark.core.presentation.util.toRelativeTime
import dev.havlicektomas.notemark.core.presentation.util.toShortDate
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NoteDetailRoot(
    onBack: () -> Unit,
    onOpenReader: (noteId: String) -> Unit,
    viewModel: NoteDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var error by remember { mutableStateOf<UiText?>(null) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            NoteDetailEvent.NavigateBack -> onBack()
            is NoteDetailEvent.OpenReader -> onOpenReader(event.noteId)
            is NoteDetailEvent.ShowError -> error = event.message
        }
    }

    error?.let { uiText ->
        val message = uiText.asString()
        LaunchedEffect(uiText) {
            snackbarHostState.showSnackbar(message)
            error = null
        }
    }

    NoteDetailScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NoteDetailScreen(
    state: NoteDetailState,
    snackbarHostState: SnackbarHostState,
    onAction: (NoteDetailAction) -> Unit,
) {
    BackHandler(enabled = true) { onAction(NoteDetailAction.OnBackClick) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            when (state.mode) {
                NoteMode.EDIT -> EditModeContent(state = state, onAction = onAction)
                NoteMode.VIEW -> ViewModeContent(state = state, onAction = onAction)
            }
        }
    }

    if (state.showDiscardDialog) {
        DiscardChangesDialog(
            onConfirm = { onAction(NoteDetailAction.OnConfirmDiscard) },
            onDismiss = { onAction(NoteDetailAction.OnDismissDiscard) },
        )
    }

    if (state.showDeleteDialog) {
        DeleteNoteDialog(
            onConfirm = { onAction(NoteDetailAction.OnConfirmDelete) },
            onDismiss = { onAction(NoteDetailAction.OnDismissDelete) },
        )
    }
}

@Composable
private fun EditModeContent(
    state: NoteDetailState,
    onAction: (NoteDetailAction) -> Unit,
) {
    EditTopBar(
        canSave = state.canSave,
        isSaving = state.isSaving,
        onClose = { onAction(NoteDetailAction.OnBackClick) },
        onSave = { onAction(NoteDetailAction.OnSaveClick) },
    )

    SaveStatusLine(
        isOnline = state.isOnline,
        lastSavedAt = state.lastSavedAt,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 8.dp),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        EditorField(
            value = state.title,
            onValueChange = { onAction(NoteDetailAction.OnTitleChange(it)) },
            placeholder = "Note title",
            textStyle = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                letterSpacing = (-0.018).em,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            singleLine = true,
            imeAction = ImeAction.Next,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        EditorField(
            value = state.content,
            onValueChange = { onAction(NoteDetailAction.OnContentChange(it)) },
            placeholder = "Start typing…",
            textStyle = TextStyle(
                fontSize = 16.sp,
                lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            singleLine = false,
            imeAction = ImeAction.Default,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun ViewModeContent(
    state: NoteDetailState,
    onAction: (NoteDetailAction) -> Unit,
) {
    ViewTopBar(
        onBack = { onAction(NoteDetailAction.OnBackClick) },
        onReader = { onAction(NoteDetailAction.OnReaderClick) },
        onEdit = { onAction(NoteDetailAction.OnEditClick) },
        onDelete = { onAction(NoteDetailAction.OnDeleteClick) },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 6.dp, bottom = 40.dp),
    ) {
        Text(
            text = state.title,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.018).em,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val created = state.createdAt?.toShortDate()
        val edited = state.lastEditedAt?.toRelativeTime()
        if (created != null && edited != null) {
            Text(
                text = "Created $created · Edited $edited",
                fontFamily = jetBrainsMonoFontFamily(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (!state.isOnline) {
            OfflineBanner(
                text = "Saved on this device",
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        if (state.content.isNotBlank()) {
            Text(
                text = state.content,
                fontSize = 16.sp,
                lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}

@Composable
private fun EditTopBar(
    canSave: Boolean,
    isSaving: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = NoteMarkIcons.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = "Edit note",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = onSave,
            enabled = canSave && !isSaving,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            modifier = Modifier.height(40.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(text = "Save", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ViewTopBar(
    onBack: () -> Unit,
    onReader: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = NoteMarkIcons.ArrowLeft,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onReader) {
            Icon(
                imageVector = NoteMarkIcons.BookReader,
                contentDescription = "Reader mode",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = NoteMarkIcons.Pencil,
                contentDescription = "Edit note",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = NoteMarkIcons.MoreVertical,
                contentDescription = "Delete note",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * M4 autosave status under the edit top bar: a green tick with the last local
 * save while online, the amber offline notice otherwise.
 */
@Composable
private fun SaveStatusLine(
    isOnline: Boolean,
    lastSavedAt: Instant?,
    modifier: Modifier = Modifier,
) {
    if (lastSavedAt == null) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
    ) {
        if (isOnline) {
            Icon(
                imageVector = NoteMarkIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "Saved ${lastSavedAt.toRelativeTime().lowercaseFirst()}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Icon(
                imageVector = NoteMarkIcons.WifiOff,
                contentDescription = null,
                tint = OnWarningContainer,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "Offline — saved on this device",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = OnWarningContainer,
            )
        }
    }
}

private fun String.lowercaseFirst(): String = replaceFirstChar { it.lowercaseChar() }

/** Borderless editor field with a placeholder, used for both title and body. */
@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    singleLine: Boolean,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            singleLine = singleLine,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = imeAction),
            modifier = if (singleLine) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
        )
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
private fun DiscardChangesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard changes?") },
        text = { Text("Your edits won't be saved.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Discard", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep editing", color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Composable
private fun DeleteNoteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete note?") },
        text = { Text("This note will be permanently deleted.") },
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
private fun NoteDetailEditPreview() {
    NoteMarkTheme {
        NoteDetailScreen(
            state = NoteDetailState(
                mode = NoteMode.EDIT,
                title = "Weekly planning",
                content = "Block two hours Monday for deep work and review.",
                canSave = true,
                hasChanges = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun NoteDetailViewPreview() {
    NoteMarkTheme {
        NoteDetailScreen(
            state = NoteDetailState(
                mode = NoteMode.VIEW,
                title = "Weekly planning",
                content = "Block two hours Monday for deep work and review.",
                createdAt = Clock.System.now(),
                lastEditedAt = Clock.System.now(),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}
