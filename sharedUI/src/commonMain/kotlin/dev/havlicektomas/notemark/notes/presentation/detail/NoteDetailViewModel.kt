package dev.havlicektomas.notemark.notes.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.havlicektomas.notemark.core.domain.ConnectivityObserver
import dev.havlicektomas.notemark.core.domain.util.onFailure
import dev.havlicektomas.notemark.core.domain.util.onSuccess
import dev.havlicektomas.notemark.core.presentation.util.UiText
import dev.havlicektomas.notemark.core.presentation.util.toUiText
import dev.havlicektomas.notemark.notes.domain.Note
import dev.havlicektomas.notemark.notes.domain.NotesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

enum class NoteMode { VIEW, EDIT }

data class NoteDetailState(
    val mode: NoteMode = NoteMode.EDIT,
    val title: String = "",
    val content: String = "",
    val createdAt: Instant? = null,
    val lastEditedAt: Instant? = null,
    val canSave: Boolean = false,
    val hasChanges: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isOnline: Boolean = true,
    /** When the note content on screen was last persisted locally (M4 autosave). */
    val lastSavedAt: Instant? = null,
)

sealed interface NoteDetailAction {
    data class OnTitleChange(val value: String) : NoteDetailAction
    data class OnContentChange(val value: String) : NoteDetailAction
    data object OnSaveClick : NoteDetailAction
    data object OnBackClick : NoteDetailAction
    data object OnConfirmDiscard : NoteDetailAction
    data object OnDismissDiscard : NoteDetailAction
    data object OnEditClick : NoteDetailAction
    data object OnReaderClick : NoteDetailAction
    data object OnDeleteClick : NoteDetailAction
    data object OnConfirmDelete : NoteDetailAction
    data object OnDismissDelete : NoteDetailAction
}

sealed interface NoteDetailEvent {
    data object NavigateBack : NoteDetailEvent
    data class OpenReader(val noteId: String) : NoteDetailEvent
    data class ShowError(val message: UiText) : NoteDetailEvent
}

class NoteDetailViewModel(
    private val notesRepository: NotesRepository,
    connectivityObserver: ConnectivityObserver,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Type-safe nav stores route properties under their declared names.
    private val noteId: String = savedStateHandle.get<String>("noteId").orEmpty()
    private val isNew: Boolean = savedStateHandle.get<Boolean>("isNew") ?: false

    private var createdAt: Instant = Clock.System.now()

    // What the current edit session started from — the discard target. Autosave
    // persists as the user types, so "discard" reverts to (and re-saves) these.
    private var sessionTitle: String = ""
    private var sessionContent: String = ""
    private var savedDuringSession = false

    private var autosaveJob: Job? = null

    private val _state = MutableStateFlow(NoteDetailState())
    val state = _state.asStateFlow()

    private val _events = Channel<NoteDetailEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isConnected.collect { connected ->
                _state.update { it.copy(isOnline = connected) }
            }
        }
        if (!isNew) {
            viewModelScope.launch {
                val note = notesRepository.getNote(noteId)
                if (note != null) {
                    createdAt = note.createdAt
                    sessionTitle = note.title
                    sessionContent = note.content
                    // Existing notes open read-only; new notes open straight into the editor.
                    _state.update {
                        it.copy(
                            mode = NoteMode.VIEW,
                            title = note.title,
                            content = note.content,
                            createdAt = note.createdAt,
                            lastEditedAt = note.lastEditedAt,
                            lastSavedAt = note.lastEditedAt,
                        ).recomputed()
                    }
                } else {
                    // The note isn't stored locally (e.g. opened after process death). Bail out.
                    _events.send(NoteDetailEvent.NavigateBack)
                }
            }
        } else {
            _state.update { it.copy(createdAt = createdAt, lastEditedAt = createdAt) }
        }
    }

    fun onAction(action: NoteDetailAction) {
        when (action) {
            is NoteDetailAction.OnTitleChange -> {
                _state.update { it.copy(title = action.value).recomputed() }
                scheduleAutosave()
            }
            is NoteDetailAction.OnContentChange -> {
                _state.update { it.copy(content = action.value).recomputed() }
                scheduleAutosave()
            }
            NoteDetailAction.OnSaveClick -> saveAndExitEditing()
            NoteDetailAction.OnBackClick -> onBack()
            NoteDetailAction.OnDismissDiscard -> _state.update { it.copy(showDiscardDialog = false) }
            NoteDetailAction.OnConfirmDiscard -> confirmDiscard()
            NoteDetailAction.OnEditClick -> startEditSession()
            NoteDetailAction.OnReaderClick -> sendEvent(NoteDetailEvent.OpenReader(noteId))
            NoteDetailAction.OnDeleteClick -> _state.update { it.copy(showDeleteDialog = true) }
            NoteDetailAction.OnDismissDelete -> _state.update { it.copy(showDeleteDialog = false) }
            NoteDetailAction.OnConfirmDelete -> deleteNote()
        }
    }

    private fun startEditSession() {
        val current = _state.value
        sessionTitle = current.title
        sessionContent = current.content
        savedDuringSession = false
        _state.update { it.copy(mode = NoteMode.EDIT).recomputed() }
    }

    /** M4 automatic saving: persist locally 800ms after the user stops typing. */
    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        val current = _state.value
        if (current.title.isBlank()) return // a note needs a title before it can exist
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DEBOUNCE_MS)
            persist()
        }
    }

    /** Writes the current editor content to the repository (local DB + sync queue). */
    private suspend fun persist(): Boolean {
        val current = _state.value
        if (current.title.isBlank()) return false
        val note = Note(
            id = noteId,
            title = current.title.trim(),
            content = current.content,
            createdAt = createdAt,
            lastEditedAt = Clock.System.now(),
        )
        var saved = false
        notesRepository.upsertNote(note)
            .onSuccess {
                savedDuringSession = true
                saved = true
                _state.update {
                    it.copy(
                        lastEditedAt = note.lastEditedAt,
                        lastSavedAt = note.lastEditedAt,
                    )
                }
            }
            .onFailure { error ->
                _events.send(NoteDetailEvent.ShowError(error.toUiText()))
            }
        return saved
    }

    private fun saveAndExitEditing() {
        val current = _state.value
        if (!current.canSave || current.isSaving) return
        autosaveJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val saved = persist()
            _state.update { it.copy(isSaving = false) }
            if (!saved) return@launch
            if (isNew) {
                _events.send(NoteDetailEvent.NavigateBack)
            } else {
                // Keep the user on the note, now read-only with refreshed metadata.
                sessionTitle = _state.value.title
                sessionContent = _state.value.content
                _state.update {
                    it.copy(mode = NoteMode.VIEW, title = it.title.trim()).recomputed()
                }
            }
        }
    }

    private fun onBack() {
        val current = _state.value
        when {
            // Reading an existing note → leave the screen.
            current.mode == NoteMode.VIEW -> sendEvent(NoteDetailEvent.NavigateBack)
            // Edits this session (possibly already autosaved) → confirm before leaving.
            current.hasChanges || (isNew && savedDuringSession) ->
                _state.update { it.copy(showDiscardDialog = true) }
            // No edits: a new note leaves; an existing note returns to read-only.
            isNew -> sendEvent(NoteDetailEvent.NavigateBack)
            else -> _state.update { it.copy(mode = NoteMode.VIEW) }
        }
    }

    private fun confirmDiscard() {
        autosaveJob?.cancel()
        _state.update { it.copy(showDiscardDialog = false) }
        viewModelScope.launch {
            if (isNew) {
                // Autosave may have already stored the draft — remove it again.
                if (savedDuringSession) notesRepository.deleteNote(noteId)
                _events.send(NoteDetailEvent.NavigateBack)
                return@launch
            }
            // Revert the editor to the session snapshot; if autosave already wrote
            // newer content, persist the revert so the stored note matches again.
            _state.update {
                it.copy(mode = NoteMode.VIEW, title = sessionTitle, content = sessionContent)
                    .recomputed()
            }
            if (savedDuringSession) persist()
        }
    }

    private fun deleteNote() {
        autosaveJob?.cancel()
        _state.update { it.copy(showDeleteDialog = false) }
        viewModelScope.launch {
            notesRepository.deleteNote(noteId)
                .onSuccess { _events.send(NoteDetailEvent.NavigateBack) }
                .onFailure { error -> _events.send(NoteDetailEvent.ShowError(error.toUiText())) }
        }
    }

    private fun sendEvent(event: NoteDetailEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    private fun NoteDetailState.recomputed(): NoteDetailState {
        val changed = title != sessionTitle || content != sessionContent
        return copy(
            hasChanges = changed,
            canSave = title.isNotBlank() && (changed || savedDuringSession),
        )
    }

    private companion object {
        /** Within the 500–1000ms window the M4 spec asks for. */
        const val AUTOSAVE_DEBOUNCE_MS = 800L
    }
}
