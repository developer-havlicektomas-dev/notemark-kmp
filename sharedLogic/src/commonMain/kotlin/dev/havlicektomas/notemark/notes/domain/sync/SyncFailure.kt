package dev.havlicektomas.notemark.notes.domain.sync

import dev.havlicektomas.notemark.core.domain.util.DataError

/**
 * Errors that retrying the same upload won't fix (the server rejected the
 * payload itself). A queue record failing this way is skipped so it doesn't
 * block the rest of the queue or the download phase; it stays queued (keeping
 * the local note) and is retried on later syncs or when the note is edited.
 */
fun DataError.Network.isPermanentSyncFailure(): Boolean = when (this) {
    DataError.Network.BAD_REQUEST,
    DataError.Network.FORBIDDEN,
    DataError.Network.NOT_FOUND,
    DataError.Network.CONFLICT,
    DataError.Network.PAYLOAD_TOO_LARGE,
    -> true
    else -> false
}
