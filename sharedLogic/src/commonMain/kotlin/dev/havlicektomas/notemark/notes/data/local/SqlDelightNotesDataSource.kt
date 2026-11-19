package dev.havlicektomas.notemark.notes.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.havlicektomas.notemark.db.NoteEntity
import dev.havlicektomas.notemark.db.NoteMarkDatabase
import dev.havlicektomas.notemark.notes.domain.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Instant

class SqlDelightNotesDataSource(database: NoteMarkDatabase) {

    private val queries = database.noteQueries

    fun notesForUser(username: String): Flow<List<Note>> =
        queries.notesForUser(username)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toNote() } }

    suspend fun searchNotes(username: String, query: String, limit: Int): List<Note> =
        withContext(Dispatchers.IO) {
            queries.searchNotesForUser(
                username = username,
                pattern = "%${query.escapeLikePattern()}%",
                limit = limit.toLong(),
            ).executeAsList().map { it.toNote() }
        }

    suspend fun getNote(id: String): Note? = withContext(Dispatchers.IO) {
        queries.noteById(id).executeAsOneOrNull()?.toNote()
    }

    suspend fun getNoteForUser(id: String, username: String): Note? = withContext(Dispatchers.IO) {
        queries.noteByIdForUser(id, username).executeAsOneOrNull()?.toNote()
    }

    suspend fun upsertNote(username: String, note: Note) = withContext(Dispatchers.IO) {
        queries.upsertNote(
            id = note.id,
            username = username,
            title = note.title,
            content = note.content,
            createdAtEpochMs = note.createdAt.toEpochMilliseconds(),
            lastEditedAtEpochMs = note.lastEditedAt.toEpochMilliseconds(),
        )
    }

    suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        queries.deleteNoteById(id)
    }

    suspend fun deleteSyncedForUser(username: String) = withContext(Dispatchers.IO) {
        queries.deleteSyncedForUser(username)
    }

    suspend fun deleteAllForUser(username: String) = withContext(Dispatchers.IO) {
        queries.deleteAllForUser(username)
    }
}

/** Escapes LIKE wildcards so user input matches literally (paired with `ESCAPE '\'` in Note.sq). */
private fun String.escapeLikePattern(): String =
    replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

private fun NoteEntity.toNote(): Note = Note(
    id = id,
    title = title,
    content = content,
    createdAt = Instant.fromEpochMilliseconds(createdAtEpochMs),
    lastEditedAt = Instant.fromEpochMilliseconds(lastEditedAtEpochMs),
)
