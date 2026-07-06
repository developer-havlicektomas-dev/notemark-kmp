package dev.havlicektomas.notemark.di

import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.Result
import dev.havlicektomas.notemark.notes.domain.Note
import dev.havlicektomas.notemark.notes.domain.NotesRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Swift-friendly view of a note: timestamps are epoch millis (a Swift `Date` is
 * one initializer away) instead of `kotlinx.datetime.Instant`.
 */
data class IosNote(
    val id: String,
    val title: String,
    val content: String,
    val createdAtEpochMs: Long,
    val lastEditedAtEpochMs: Long,
)

/** A notes snapshot plus an optional error, so Swift gets both in one call. */
class IosNotesResult(
    val notes: List<IosNote>,
    val error: DataError.Network?,
)

/**
 * Swift-friendly facade over the notes domain. Like [IosAuthService], it hides
 * the generic `Result` (returning a nullable [DataError.Network] — `null` means
 * success) and the `Flow`/`Instant` types behind plain values.
 */
class IosNotesService : KoinComponent {

    private val notesRepository: NotesRepository by inject()

    /** Runs a full sync, then returns the current local notes and any error. */
    suspend fun getNotes(): IosNotesResult {
        val error = notesRepository.fetchNotes().errorOrNull()
        val notes = notesRepository.notes.first().map { it.toIos() }
        return IosNotesResult(notes = notes, error = error)
    }

    /** Current local snapshot without touching the network (list refreshes). */
    suspend fun getLocalNotes(): List<IosNote> =
        notesRepository.notes.first().map { it.toIos() }

    /**
     * Upserts a note under a caller-provided id, so repeated autosaves of the
     * same draft keep hitting one row (M4). `lastEditedAt` is stamped here;
     * `createdAt` round-trips from the draft.
     */
    suspend fun saveNote(
        id: String,
        title: String,
        content: String,
        createdAtEpochMs: Long,
    ): DataError.Network? {
        val note = Note(
            id = id,
            title = title.trim(),
            content = content,
            createdAt = Instant.fromEpochMilliseconds(createdAtEpochMs),
            lastEditedAt = Clock.System.now(),
        )
        return notesRepository.upsertNote(note).errorOrNull()
    }

    suspend fun deleteNote(id: String): DataError.Network? =
        notesRepository.deleteNote(id).errorOrNull()

    /** Clears local notes; rows with pending sync changes survive (M4 semantics). */
    suspend fun clearLocalData() = notesRepository.clearSyncedLocalData()

    /** Full local wipe (notes + sync queue) for M3 logout. */
    suspend fun clearAllLocalData() = notesRepository.clearAllLocalData()

    private fun <T> Result<T, DataError.Network>.errorOrNull(): DataError.Network? =
        (this as? Result.Error)?.error

    private fun Note.toIos(): IosNote = IosNote(
        id = id,
        title = title,
        content = content,
        createdAtEpochMs = createdAt.toEpochMilliseconds(),
        lastEditedAtEpochMs = lastEditedAt.toEpochMilliseconds(),
    )
}
