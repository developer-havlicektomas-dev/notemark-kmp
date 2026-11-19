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
import kotlin.time.Instant

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
     * - UPDATE then UPDATE stays a single UPDATE
     * - CREATE/UPDATE then DELETE becomes DELETE. We can't assume the server never
     *   saw a CREATE: its upload may be in flight, or have succeeded with the
     *   response lost. A DELETE for a note the server doesn't have is a no-op.
     *
     * A coalesced record gets a newer `enqueuedAt`, which tells an in-flight
     * upload (see [removeIfUnchanged]) that the note changed again after it read it.
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
                insert(username, noteId, SyncOperation.DELETE, enqueuedAt)
            }

            // Note written again after a pending DELETE: upload it instead.
            pendingOp == SyncOperation.DELETE -> {
                queries.deleteForNote(username, noteId)
                insert(username, noteId, SyncOperation.UPDATE, enqueuedAt)
            }

            // CREATE or UPDATE over an existing CREATE/UPDATE: the pending record
            // already covers it (content is read fresh from the DB when syncing),
            // but bump its timestamp so an in-flight upload won't remove it.
            else -> {
                val record = existing.last()
                val bumped = maxOf(
                    enqueuedAt.toEpochMilliseconds(),
                    record.enqueuedAt.toEpochMilliseconds() + 1,
                )
                queries.touch(enqueuedAtEpochMs = bumped, queueId = record.queueId)
            }
        }
    }

    /**
     * Removes [record] after a successful upload, unless the note was enqueued
     * again meanwhile — then the record stays so the newer edit gets uploaded.
     */
    suspend fun removeIfUnchanged(record: PendingSyncRecord) = withContext(Dispatchers.IO) {
        queries.deleteByQueueIdIfUnchanged(
            queueId = record.queueId,
            enqueuedAtEpochMs = record.enqueuedAt.toEpochMilliseconds(),
        )
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
