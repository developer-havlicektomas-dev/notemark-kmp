package dev.havlicektomas.notemark.notes.data

import dev.havlicektomas.notemark.auth.domain.SessionStorage
import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.EmptyResult
import dev.havlicektomas.notemark.core.domain.util.Result
import dev.havlicektomas.notemark.notes.data.local.SqlDelightNotesDataSource
import dev.havlicektomas.notemark.notes.data.sync.NotesSyncEngine
import dev.havlicektomas.notemark.notes.data.sync.SqlDelightSyncQueueDataSource
import dev.havlicektomas.notemark.notes.data.sync.SyncMetadataStore
import dev.havlicektomas.notemark.notes.domain.Note
import dev.havlicektomas.notemark.notes.domain.NotesRepository
import dev.havlicektomas.notemark.notes.domain.sync.SyncOperation
import dev.havlicektomas.notemark.notes.domain.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock

/**
 * M4 offline-first repository: the local database is the source of truth.
 * Writes land in the DB first, get a sync-queue record, and are pushed to the
 * backend by [NotesSyncEngine] whenever connectivity allows.
 */
class OfflineFirstNotesRepository(
    private val local: SqlDelightNotesDataSource,
    private val queue: SqlDelightSyncQueueDataSource,
    private val sessionStorage: SessionStorage,
    private val syncEngine: NotesSyncEngine,
    private val syncScheduler: SyncScheduler,
    private val syncMetadata: SyncMetadataStore,
) : NotesRepository {

    override val notes: Flow<List<Note>> = flow {
        val username = sessionStorage.get()?.username
        if (username == null) {
            emitAll(flowOf(emptyList()))
        } else {
            emitAll(local.notesForUser(username))
        }
    }

    override suspend fun fetchNotes(): EmptyResult<DataError.Network> = syncEngine.sync()

    // Scoped to the logged-in user: rows of a previous user can linger locally
    // while their pending changes wait for upload (see deleteSyncedForUser).
    override suspend fun getNote(id: String): Note? {
        val username = sessionStorage.get()?.username ?: return null
        return local.getNoteForUser(id, username)
    }

    override suspend fun searchNotes(query: String, limit: Int): List<Note> {
        val username = sessionStorage.get()?.username ?: return emptyList()
        return local.searchNotes(username, query, limit)
    }

    override suspend fun upsertNote(note: Note): EmptyResult<DataError.Network> {
        val username = sessionStorage.get()?.username
            ?: return Result.Error(DataError.Network.UNAUTHORIZED)
        val operation = if (local.getNote(note.id) == null) {
            SyncOperation.CREATE
        } else {
            SyncOperation.UPDATE
        }
        local.upsertNote(username, note)
        queue.enqueue(username, note.id, operation, Clock.System.now())
        syncScheduler.requestSync()
        return Result.Success(Unit)
    }

    override suspend fun deleteNote(id: String): EmptyResult<DataError.Network> {
        val username = sessionStorage.get()?.username
            ?: return Result.Error(DataError.Network.UNAUTHORIZED)
        local.deleteNote(id)
        queue.enqueue(username, id, SyncOperation.DELETE, Clock.System.now())
        syncScheduler.requestSync()
        return Result.Success(Unit)
    }

    override suspend fun clearSyncedLocalData() {
        val username = sessionStorage.get()?.username ?: return
        local.deleteSyncedForUser(username)
    }

    override suspend fun clearAllLocalData() {
        val username = sessionStorage.get()?.username ?: return
        local.deleteAllForUser(username)
        queue.clearForUser(username)
        syncMetadata.clearLastSyncedAt(username)
    }
}
