package dev.havlicektomas.notemark.notes.data.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import dev.havlicektomas.notemark.db.NoteMarkDatabase
import dev.havlicektomas.notemark.db.SyncQueueEntity
import dev.havlicektomas.notemark.notes.domain.sync.SyncOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

data class PendingSyncRecord(
    val queueId: Long,
    val noteId: String,
    val operation: SyncOperation,
    val enqueuedAt: Instant,
)

class SqlDelightSyncQueueDataSource(database: NoteMarkDatabase) {

    private val queries = database.syncQueueQueries

    suspend fun pendingForUser(username: String): List<PendingSyncRecord> =
        withContext(Dispatchers.IO) {
            queries.pendingForUser(username).executeAsList().map { it.toRecord() }
        }

    fun pendingCountForUser(username: String): Flow<Long> =
        queries.pendingCountForUser(username)
            .asFlow()
            .mapToOne(Dispatchers.IO)

    /**
     * Enqueues an operation, coalescing with what's already pending for the note
     * so the queue holds at most one record per note:
     * - CREATE then UPDATE stays CREATE (payload is read from the DB at sync time)
     * - CREATE then DELETE cancels out (the server never saw the note)
     * - UPDATE then UPDATE stays a single UPDATE
     * - UPDATE then DELETE becomes DELETE
     */
    suspend fun enqueue(
        username: String,
        noteId: String,
        operation: SyncOperation,
        enqueuedAt: Instant,
    ) = withContext(Dispatchers.IO) {
        val existing = queries.operationsForNote(username, noteId).executeAsList()
            .map { it.toRecord() }
        val pendingOp = existing.lastOrNull()?.operation

        when {
            pendingOp == null -> insert(username, noteId, operation, enqueuedAt)

            operation == SyncOperation.DELETE -> {
                queries.deleteForNote(username, noteId)
                if (pendingOp != SyncOperation.CREATE) {
                    insert(username, noteId, SyncOperation.DELETE, enqueuedAt)
                }
            }

            // CREATE or UPDATE over an existing CREATE/UPDATE: the pending record
            // already covers it — content is read fresh from the DB when syncing.
            else -> Unit
        }
    }

    suspend fun remove(queueId: Long) = withContext(Dispatchers.IO) {
        queries.deleteByQueueId(queueId)
    }

    suspend fun clearForUser(username: String) = withContext(Dispatchers.IO) {
        queries.clearForUser(username)
    }

    private fun insert(
        username: String,
        noteId: String,
        operation: SyncOperation,
        enqueuedAt: Instant,
    ) {
        queries.enqueue(
            username = username,
            noteId = noteId,
            operation = operation.name,
            enqueuedAtEpochMs = enqueuedAt.toEpochMilliseconds(),
        )
    }
}

private fun SyncQueueEntity.toRecord(): PendingSyncRecord = PendingSyncRecord(
    queueId = queueId,
    noteId = noteId,
    operation = SyncOperation.valueOf(operation),
    enqueuedAt = Instant.fromEpochMilliseconds(enqueuedAtEpochMs),
)
