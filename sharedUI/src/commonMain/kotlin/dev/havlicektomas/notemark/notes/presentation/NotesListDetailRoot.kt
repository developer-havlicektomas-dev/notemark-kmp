package dev.havlicektomas.notemark.notes.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.savedstate.savedState
import dev.havlicektomas.notemark.core.presentation.designsystem.components.NoteMarkIcons
import dev.havlicektomas.notemark.notes.presentation.detail.NoteDetailRoot
import dev.havlicektomas.notemark.notes.presentation.detail.NoteDetailViewModel
import dev.havlicektomas.notemark.notes.presentation.list.NotesListRoot
import kotlin.random.Random
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** The note shown in the detail pane. [instance] gives every opening its own ViewModel. */
private data class NoteSelection(
    val noteId: String,
    val isNew: Boolean,
    val instance: Long,
)

private val NoteSelectionSaver = Saver<NoteSelection?, List<Any>>(
    save = { selection -> selection?.let { listOf(it.noteId, it.isNew, it.instance) } },
    restore = { NoteSelection(it[0] as String, it[1] as Boolean, it[2] as Long) },
)

/**
 * Notes list + note detail as an adaptive list-detail layout.
 *
 * Compact windows (phones, folded foldables) show one pane at a time, like the
 * old list → detail navigation. Expanded windows (tablets, unfolded foldables,
 * desktop-sized windows) show the grid and the open note side by side.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NotesListDetailRoot(
    onOpenReader: (noteId: String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()
    val scope = rememberCoroutineScope()
    var selection by rememberSaveable(stateSaver = NoteSelectionSaver) {
        mutableStateOf<NoteSelection?>(null)
    }

    fun openNote(noteId: String, isNew: Boolean) {
        selection = NoteSelection(noteId, isNew, instance = Random.nextLong())
        if (navigator.currentDestination?.pane != ListDetailPaneScaffoldRole.Detail) {
            scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail) }
        }
    }

    fun closeNote() {
        scope.launch {
            if (navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) {
                // Suspends until the pane transition finishes, so the detail
                // content stays on screen while it animates out.
                navigator.navigateBack(BackNavigationBehavior.PopLatest)
            }
            selection = null
        }
    }

    // System back is handled by the detail pane itself (NoteDetailScreen's
    // BackHandler routes it through the ViewModel for the discard dialog),
    // so the plain, non-navigable scaffold is used here.
    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                NotesListRoot(
                    onOpenNote = { id -> openNote(id, isNew = false) },
                    onCreateNote = { id -> openNote(id, isNew = true) },
                    onOpenSettings = onOpenSettings,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val current = selection
                if (current == null) {
                    NoNoteSelected()
                } else {
                    key(current.instance) {
                        NoteDetailRoot(
                            onBack = ::closeNote,
                            onOpenReader = onOpenReader,
                            viewModel = noteDetailViewModel(current),
                        )
                    }
                }
            }
        },
    )
}

/**
 * Creates the detail ViewModel for [selection], scoped to the enclosing nav
 * entry. The note id is handed over as SavedStateHandle default args, exactly
 * as type-safe navigation did for the former NoteDetailRoute.
 */
@Composable
private fun noteDetailViewModel(selection: NoteSelection): NoteDetailViewModel {
    val owner = checkNotNull(LocalViewModelStoreOwner.current) {
        "NotesListDetailRoot needs a ViewModelStoreOwner"
    }
    val baseExtras = (owner as? HasDefaultViewModelProviderFactory)
        ?.defaultViewModelCreationExtras
        ?: CreationExtras.Empty
    val extras = MutableCreationExtras(baseExtras).apply {
        set(
            DEFAULT_ARGS_KEY,
            savedState {
                putString(NoteDetailViewModel.ARG_NOTE_ID, selection.noteId)
                putBoolean(NoteDetailViewModel.ARG_IS_NEW, selection.isNew)
            },
        )
    }
    return koinViewModel(
        viewModelStoreOwner = owner,
        key = "note-detail-${selection.instance}",
        extras = extras,
    )
}

@Composable
private fun NoNoteSelected() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(18.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = NoteMarkIcons.FileText,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Select a note",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Pick a note from the list to read or edit it here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
