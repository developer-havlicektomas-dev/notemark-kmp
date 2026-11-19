package dev.havlicektomas.notemark.notes.domain.commands

import dev.havlicektomas.notemark.auth.domain.SessionStorage
import dev.havlicektomas.notemark.core.domain.util.Error
import dev.havlicektomas.notemark.core.domain.util.Result
import dev.havlicektomas.notemark.notes.domain.Note
import dev.havlicektomas.notemark.notes.domain.NotesRepository
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Note operations for callers outside the app's own UI, such as on-device
 * assistants (Android App Functions). Applies the same rules as the editor
 * (a note needs a non-blank, trimmed title) and goes through the offline-first
 * [NotesRepository], so changes are stored locally and queued for sync.
 */
class NoteCommands(
    private val notesRepository: NotesRepository,
    private val sessionStorage: SessionStorage,
    private val clock: Clock = Clock.System,
) {

    suspend fun createNote(title: String, content: String?): Result<Note, NoteCommandError> {
        if (!isLoggedIn()) return Result.Error(NoteCommandError.NOT_LOGGED_IN)
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return Result.Error(NoteCommandError.BLANK_TITLE)

        val now = clock.now()
        val note = Note(
            id = Uuid.random().toString(),
            title = trimmedTitle,
            content = content.orEmpty(),
            createdAt = now,
            lastEditedAt = now,
        )
        return save(note)
    }

    /** Null [title] / [content] keep the current value. */
    suspend fun updateNote(
        id: String,
        title: String?,
        content: String?,
    ): Result<Note, NoteCommandError> {
        if (!isLoggedIn()) return Result.Error(NoteCommandError.NOT_LOGGED_IN)
        if (title == null && content == null) return Result.Error(NoteCommandError.NOTHING_TO_UPDATE)
        val trimmedTitle = title?.trim()
        if (trimmedTitle != null && trimmedTitle.isEmpty()) {
            return Result.Error(NoteCommandError.BLANK_TITLE)
        }
        val existing = notesRepository.getNote(id)
            ?: return Result.Error(NoteCommandError.NOTE_NOT_FOUND)

        val updated = existing.copy(
            title = trimmedTitle ?: existing.title,
            content = content ?: existing.content,
            lastEditedAt = clock.now(),
        )
        return save(updated)
    }

    suspend fun getNote(id: String): Result<Note, NoteCommandError> {
        if (!isLoggedIn()) return Result.Error(NoteCommandError.NOT_LOGGED_IN)
        val note = notesRepository.getNote(id) ?: return Result.Error(NoteCommandError.NOTE_NOT_FOUND)
        return Result.Success(note)
    }

    /**
     * Notes matching [query] (newest edit first). A blank query returns the most
     * recently edited notes. [limit] is clamped to 1..[MAX_SEARCH_LIMIT]; values
     * below 1 (e.g. an omitted argument) use [DEFAULT_SEARCH_LIMIT].
     */
    suspend fun findNotes(query: String?, limit: Int): Result<List<Note>, NoteCommandError> {
        if (!isLoggedIn()) return Result.Error(NoteCommandError.NOT_LOGGED_IN)
        val effectiveLimit = if (limit < 1) DEFAULT_SEARCH_LIMIT else limit.coerceAtMost(MAX_SEARCH_LIMIT)
        return Result.Success(notesRepository.searchNotes(query?.trim().orEmpty(), effectiveLimit))
    }

    private suspend fun save(note: Note): Result<Note, NoteCommandError> =
        when (notesRepository.upsertNote(note)) {
            is Result.Success -> Result.Success(note)
            // upsertNote only fails when the session disappeared in between.
            is Result.Error -> Result.Error(NoteCommandError.NOT_LOGGED_IN)
        }

    private suspend fun isLoggedIn(): Boolean = sessionStorage.get() != null

    companion object {
        const val DEFAULT_SEARCH_LIMIT = 10
        const val MAX_SEARCH_LIMIT = 25
    }
}

enum class NoteCommandError : Error {
    NOT_LOGGED_IN,
    BLANK_TITLE,
    NOTHING_TO_UPDATE,
    NOTE_NOT_FOUND,
}
