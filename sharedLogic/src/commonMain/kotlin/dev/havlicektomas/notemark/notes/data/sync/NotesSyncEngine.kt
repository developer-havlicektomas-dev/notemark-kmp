package dev.havlicektomas.notemark.notes.data.sync

import dev.havlicektomas.notemark.auth.domain.SessionStorage
import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.EmptyResult
import dev.havlicektomas.notemark.core.domain.util.Result
import dev.havlicektomas.notemark.core.domain.util.asEmptyResult
import dev.havlicektomas.notemark.notes.data.KtorNotesDataSource
import dev.havlicektomas.notemark.notes.data.local.SqlDelightNotesDataSource
import dev.havlicektomas.notemark.notes.data.toNote
import dev.havlicektomas.notemark.notes.data.toRequest
import dev.havlicektomas.notemark.notes.domain.Note
import dev.havlicektomas.notemark.notes.domain.sync.SyncOperation
import dev.havlicektomas.notemark.notes.domain.sync.isPermanentSyncFailure
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Two-way sync per the M4 spec: upload pending local changes, download remote
 * changes, merge into the local DB. Conflicts resolve by last write wins
 * (newest lastEditedAt). A failed step aborts the run — already-uploaded queue
 * records stay removed, the rest survive for the scheduler's retry.
 */
class NotesSyncEngine(
    private val local: SqlDelightNotesDataSource,
    private val queue: SqlDelightSyncQueueDataSource,
    private val remote: KtorNotesDataSource,
    private val sessionStorage: SessionStorage,
    private val metadata: SyncMetadataStore,
) {

    private val mutex = Mutex()

    suspend fun sync(): EmptyResult<DataError.Network> = mutex.withLock {
        val username = sessionStorage.get()?.username
            ?: return@withLock Result.Success(Unit) // logged out: nothing to sync

        try {
            val skipped = mutableListOf<DataError.Network>()
            uploadPendingChanges(username, skipped)?.let { failure -> return@withLock failure }

            // Fresh snapshot after the upload phase, so it includes what we just pushed.
            val remoteNotes = when (val result = remote.getNotes()) {
                is Result.Success -> result.data.notes.map { it.toNote() }
                is Result.Error -> return@withLock result.asEmptyResult()
            }
            mergeRemoteIntoLocal(username, remoteNotes)

            metadata.setLastSyncedAt(username, Clock.System.now())
            // Everything else synced; still report a record the server rejected.
            skipped.firstOrNull()?.let { Result.Error(it) } ?: Result.Success(Unit)
        } catch (e: IllegalArgumentException) {
            // A malformed timestamp from the server (Instant.parse) — report it
            // instead of throwing out of sync (which would crash the iOS app).
            Result.Error(DataError.Network.SERIALIZATION)
        }
    }

    /**
     * Returns null when every pending record uploaded, resolved or was skipped,
     * a failure otherwise. Records the server rejects for good (see
     * [isPermanentSyncFailure]) are skipped and their errors added to [skipped].
     */
    private suspend fun uploadPendingChanges(
        username: String,
        skipped: MutableList<DataError.Network>,
    ): EmptyResult<DataError.Network>? {
        val pending = queue.pendingForUser(username)
        if (pending.isEmpty()) return null

        // Remote edit times, for last-write-wins decisions before overwriting.
        val remoteEditTimes = when (val result = remote.getNotes()) {
            is Result.Success -> result.data.notes.associate {
                it.id to Instant.parse(it.lastEditedAt)
            }
            is Result.Error -> return result.asEmptyResult()
        }

        for (record in pending) {
            val outcome: EmptyResult<DataError.Network> = when (record.operation) {
                SyncOperation.CREATE,
                SyncOperation.UPDATE,
                -> {
                    val note = local.getNote(record.noteId)
                    val remoteEditedAt = remoteEditTimes[record.noteId]
                    when {
                        // Local row is gone (shouldn't happen); drop the record.
                        note == null -> Result.Success(Unit)
                        // Remote is newer: it wins, the merge phase pulls it down.
                        remoteEditedAt != null && remoteEditedAt > note.lastEditedAt ->
                            Result.Success(Unit)
                        else -> remote.upsertNote(note.toRequest()).asEmptyResult()
                    }
                }
                SyncOperation.DELETE -> {
                    val remoteEditedAt = remoteEditTimes[record.noteId]
                    when {
                        // Already gone remotely.
                        remoteEditedAt == null -> Result.Success(Unit)
                        // Edited remotely after we deleted: the edit wins, merge restores it.
                        remoteEditedAt > record.enqueuedAt -> Result.Success(Unit)
                        else -> remote.deleteNote(record.noteId).notFoundAsSuccess()
                    }
                }
            }
            when (outcome) {
                is Result.Success -> queue.removeIfUnchanged(record)
                is Result.Error -> if (outcome.error.isPermanentSyncFailure()) {
                    skipped += outcome.error
                } else {
                    return outcome
                }
            }
        }
        return null
    }

    private suspend fun mergeRemoteIntoLocal(username: String, remoteNotes: List<Note>) {
        // Notes deleted locally while this sync ran: their DELETE is still queued,
        // so don't bring the server copy back.
        val pendingDeletes = queue.pendingForUser(username)
            .filter { it.operation == SyncOperation.DELETE }
            .mapTo(mutableSetOf()) { it.noteId }
        for (remoteNote in remoteNotes) {
            if (remoteNote.id in pendingDeletes) continue
            val localNote = local.getNote(remoteNote.id)
            if (localNote == null || remoteNote.lastEditedAt > localNote.lastEditedAt) {
                local.upsertNote(username, remoteNote)
            }
        }

        // The queue drained in the upload phase, so any local note missing remotely
        // was deleted on the server — unless it was edited mid-sync (pending again).
        val remoteIds = remoteNotes.mapTo(mutableSetOf()) { it.id }
        val stillPending = queue.pendingForUser(username).mapTo(mutableSetOf()) { it.noteId }
        local.notesForUser(username).first()
            .filter { it.id !in remoteIds && it.id !in stillPending }
            .forEach { local.deleteNote(it.id) }
    }

    private fun EmptyResult<DataError.Network>.notFoundAsSuccess(): EmptyResult<DataError.Network> =
        if (this is Result.Error && error == DataError.Network.NOT_FOUND) {
            Result.Success(Unit)
        } else {
            this
        }
}
