package dev.havlicektomas.notemark.auth.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide session signals. [expired] fires when the backend rejects the refresh
 * token and the session has been cleared, so the UI can send the user to log in
 * instead of leaving them on a screen where every request now fails.
 */
class SessionEvents {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()

    fun notifyExpired() {
        _expired.tryEmit(Unit)
    }
}
