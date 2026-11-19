package dev.havlicektomas.notemark.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.havlicektomas.notemark.auth.domain.SessionStorage
import dev.havlicektomas.notemark.core.domain.ConnectivityObserver
import dev.havlicektomas.notemark.core.domain.util.Result
import dev.havlicektomas.notemark.core.presentation.designsystem.components.initialsOf
import dev.havlicektomas.notemark.core.presentation.util.UiText
import dev.havlicektomas.notemark.core.presentation.util.toUiText
import dev.havlicektomas.notemark.notes.domain.NotesRepository
import dev.havlicektomas.notemark.notes.domain.sync.SyncInterval
import dev.havlicektomas.notemark.notes.domain.sync.SyncManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

data class SettingsState(
    val username: String = "",
    val initials: String = "",
    val isOnline: Boolean = true,
    /** Null means never synced. */
    val lastSyncedAt: Instant? = null,
    val isSyncing: Boolean = false,
    val syncInterval: SyncInterval = SyncInterval.MANUAL_ONLY,
    val showAutosyncSheet: Boolean = false,
    val pendingChanges: Long = 0,
    val showLogoutDialog: Boolean = false,
    val isLoggingOut: Boolean = false,
    val showClearDataDialog: Boolean = false,
)

sealed interface SettingsAction {
    data object OnBackClick : SettingsAction
    data object OnSyncNowClick : SettingsAction
    data object OnAutosyncClick : SettingsAction
    data object OnDismissAutosync : SettingsAction
    data class OnSyncIntervalSelected(val interval: SyncInterval) : SettingsAction
    data object OnLogoutClick : SettingsAction
    data object OnSyncAndLogout : SettingsAction
    data object OnLogoutWithoutSync : SettingsAction
    data object OnDismissLogout : SettingsAction
    data object OnClearDataClick : SettingsAction
    data object OnConfirmClearData : SettingsAction
    data object OnDismissClearData : SettingsAction
}

sealed interface SettingsEvent {
    data object NavigateBack : SettingsEvent
    data object NavigateToLanding : SettingsEvent
    data class ShowMessage(val message: UiText) : SettingsEvent
}

class SettingsViewModel(
    private val sessionStorage: SessionStorage,
    private val notesRepository: NotesRepository,
    private val syncManager: SyncManager,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionStorage.get()?.let { info ->
                _state.update {
                    it.copy(username = info.username, initials = initialsOf(info.username))
                }
            }
        }
        connectivityObserver.isConnected
            .onEach { connected -> _state.update { it.copy(isOnline = connected) } }
            .launchIn(viewModelScope)
        syncManager.lastSyncedAt
            .onEach { syncedAt -> _state.update { it.copy(lastSyncedAt = syncedAt) } }
            .launchIn(viewModelScope)
        syncManager.syncInterval
            .onEach { interval -> _state.update { it.copy(syncInterval = interval) } }
            .launchIn(viewModelScope)
        syncManager.pendingChangesCount
            .onEach { count -> _state.update { it.copy(pendingChanges = count) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.OnBackClick -> sendEvent(SettingsEvent.NavigateBack)
            SettingsAction.OnSyncNowClick -> syncNow()
            SettingsAction.OnAutosyncClick -> _state.update { it.copy(showAutosyncSheet = true) }
            SettingsAction.OnDismissAutosync -> _state.update { it.copy(showAutosyncSheet = false) }
            is SettingsAction.OnSyncIntervalSelected -> selectInterval(action.interval)
            SettingsAction.OnLogoutClick -> onLogoutClick()
            SettingsAction.OnDismissLogout -> _state.update { it.copy(showLogoutDialog = false) }
            SettingsAction.OnSyncAndLogout -> syncAndLogout()
            SettingsAction.OnLogoutWithoutSync -> logoutWithoutSync()
            SettingsAction.OnClearDataClick -> _state.update { it.copy(showClearDataDialog = true) }
            SettingsAction.OnDismissClearData -> _state.update { it.copy(showClearDataDialog = false) }
            SettingsAction.OnConfirmClearData -> clearLocalData()
        }
    }

    private fun syncNow() {
        if (_state.value.isSyncing) return
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            when (val result = syncManager.syncNow()) {
                is Result.Success -> Unit // lastSyncedAt flow updates the row
                is Result.Error ->
                    _events.send(SettingsEvent.ShowMessage(result.error.toUiText()))
            }
            _state.update { it.copy(isSyncing = false) }
        }
    }

    private fun selectInterval(interval: SyncInterval) {
        viewModelScope.launch {
            syncManager.setSyncInterval(interval)
            _state.update { it.copy(showAutosyncSheet = false) }
        }
    }

    private fun onLogoutClick() {
        // M4: logging out needs a connection — the queue check and any final
        // sync both talk to the backend.
        if (!_state.value.isOnline) {
            sendEvent(
                SettingsEvent.ShowMessage(
                    UiText.Dynamic("You are offline. Connect to sync or log out."),
                ),
            )
            return
        }
        _state.update { it.copy(showLogoutDialog = true) }
    }

    /** Full sync, then wipe everything: notes, queue, sync metadata, session. */
    private fun syncAndLogout() {
        if (_state.value.isLoggingOut) return
        _state.update { it.copy(showLogoutDialog = false, isLoggingOut = true) }
        viewModelScope.launch {
            when (val result = syncManager.syncNow()) {
                is Result.Success -> {
                    notesRepository.clearAllLocalData()
                    sessionStorage.clear()
                    _events.send(SettingsEvent.NavigateToLanding)
                }
                is Result.Error -> {
                    // Sync failed — keep the user logged in so nothing is lost.
                    _state.update { it.copy(isLoggingOut = false) }
                    _events.send(SettingsEvent.ShowMessage(result.error.toUiText()))
                }
            }
        }
    }

    /** Clears session + synced notes but preserves the queue for post-login recovery. */
    private fun logoutWithoutSync() {
        if (_state.value.isLoggingOut) return
        _state.update { it.copy(showLogoutDialog = false, isLoggingOut = true) }
        viewModelScope.launch {
            notesRepository.clearSyncedLocalData()
            sessionStorage.clear()
            _events.send(SettingsEvent.NavigateToLanding)
        }
    }

    private fun clearLocalData() {
        _state.update { it.copy(showClearDataDialog = false) }
        viewModelScope.launch {
            // Unsynced rows survive (they're still referenced by the queue);
            // the notes list updates reactively through the repository flow.
            notesRepository.clearSyncedLocalData()
        }
    }

    private fun sendEvent(event: SettingsEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
