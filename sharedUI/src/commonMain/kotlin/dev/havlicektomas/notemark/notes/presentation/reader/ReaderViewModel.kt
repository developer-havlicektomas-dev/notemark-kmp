package dev.havlicektomas.notemark.notes.presentation.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.havlicektomas.notemark.notes.domain.NotesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderState(
    val title: String = "",
    val content: String = "",
    val controlsVisible: Boolean = true,
)

sealed interface ReaderAction {
    data object OnTap : ReaderAction
    data object OnHideControls : ReaderAction
    data object OnBackClick : ReaderAction
}

sealed interface ReaderEvent {
    data object NavigateBack : ReaderEvent
}

class ReaderViewModel(
    private val notesRepository: NotesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val noteId: String = savedStateHandle.get<String>("noteId").orEmpty()

    private val _state = MutableStateFlow(ReaderState())
    val state = _state.asStateFlow()

    private val _events = Channel<ReaderEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val note = notesRepository.getNote(noteId)
            if (note != null) {
                _state.update { it.copy(title = note.title, content = note.content) }
            } else {
                // Not stored locally (e.g. after process death) — nothing to read.
                _events.send(ReaderEvent.NavigateBack)
            }
        }
    }

    fun onAction(action: ReaderAction) {
        when (action) {
            ReaderAction.OnTap -> _state.update { it.copy(controlsVisible = !it.controlsVisible) }
            ReaderAction.OnHideControls -> _state.update { it.copy(controlsVisible = false) }
            ReaderAction.OnBackClick -> viewModelScope.launch { _events.send(ReaderEvent.NavigateBack) }
        }
    }
}
