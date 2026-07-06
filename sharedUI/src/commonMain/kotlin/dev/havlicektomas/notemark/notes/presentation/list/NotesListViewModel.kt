package dev.havlicektomas.notemark.notes.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.havlicektomas.notemark.auth.domain.SessionStorage
import dev.havlicektomas.notemark.core.domain.ConnectivityObserver
import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.onFailure
import dev.havlicektomas.notemark.core.presentation.designsystem.components.initialsOf
import dev.havlicektomas.notemark.core.presentation.util.UiText
import dev.havlicektomas.notemark.core.presentation.util.toUiText
import dev.havlicektomas.notemark.notes.domain.NotesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

data class NotesListState(
    val notes: List<NoteUi> = emptyList(),
    val initials: String = "",
    val isLoading: Boolean = false,
    val notePendingDelete: NoteUi? = null,
    val isOnline: Boolean = true,
)

sealed interface NotesListAction {
    data class OnNoteClick(val id: String) : NotesListAction
    data class OnNoteLongClick(val id: String) : NotesListAction
    data object OnConfirmDelete : NotesListAction
    data object OnDismissDelete : NotesListAction
    data object OnCreateNote : NotesListAction
    data object OnAvatarClick : NotesListAction
}

sealed interface NotesListEvent {
    data class OpenNote(val id: String) : NotesListEvent
    data class CreateNote(val id: String) : NotesListEvent
    data object OpenSettings : NotesListEvent
    data class ShowError(val message: UiText) : NotesListEvent
}

class NotesListViewModel(
    private val notesRepository: NotesRepository,
    private val sessionStorage: SessionStorage,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(NotesListState())
    val state = _state.asStateFlow()

    private val _events = Channel<NotesListEvent>()
    val events = _events.receiveAsFlow()

    init {
        notesRepository.notes
            .onEach { notes ->
                _state.update { it.copy(notes = notes.map { note -> note.toNoteUi() }) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            sessionStorage.get()?.let { info ->
                _state.update { it.copy(initials = initialsOf(info.username)) }
            }
        }

        connectivityObserver.isConnected
            .onEach { connected -> _state.update { it.copy(isOnline = connected) } }
            .launchIn(viewModelScope)

        refresh()
    }

    fun onAction(action: NotesListAction) {
        when (action) {
            is NotesListAction.OnNoteClick -> sendEvent(NotesListEvent.OpenNote(action.id))
            is NotesListAction.OnNoteLongClick -> _state.update { current ->
                current.copy(notePendingDelete = current.notes.find { it.id == action.id })
            }
            NotesListAction.OnDismissDelete -> _state.update { it.copy(notePendingDelete = null) }
            NotesListAction.OnConfirmDelete -> deletePendingNote()
            NotesListAction.OnCreateNote -> sendEvent(NotesListEvent.CreateNote(Uuid.random().toString()))
            NotesListAction.OnAvatarClick -> sendEvent(NotesListEvent.OpenSettings)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            notesRepository.fetchNotes()
                .onFailure { error ->
                    // Offline is expected in M4 (the banner covers it) — local
                    // notes still show and changes queue up; only surface real errors.
                    if (error != DataError.Network.NO_INTERNET) {
                        _events.send(NotesListEvent.ShowError(error.toUiText()))
                    }
                }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun deletePendingNote() {
        val pending = _state.value.notePendingDelete ?: return
        _state.update { it.copy(notePendingDelete = null) }
        viewModelScope.launch {
            notesRepository.deleteNote(pending.id)
                .onFailure { error -> _events.send(NotesListEvent.ShowError(error.toUiText())) }
        }
    }

    private fun sendEvent(event: NotesListEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
