package dev.havlicektomas.notemark.appfunctions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionPermissionRequiredException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dev.havlicektomas.notemark.core.domain.util.Result
import dev.havlicektomas.notemark.notes.domain.commands.NoteCommandError
import dev.havlicektomas.notemark.notes.domain.commands.NoteCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Exposes NoteMark notes to on-device agents (e.g. Gemini) via Android App Functions.
 *
 * KSP generates the concrete `NotesAppFunctionService` (declared in the manifest) and the
 * `notes_app_functions.xml` asset describing these functions. The service runs in the app
 * process, so [dev.havlicektomas.notemark.NoteMarkApplication] has already started Koin.
 * All writes go through the offline-first repository: stored locally, then synced.
 *
 * Default parameter values only mark a parameter as optional for the agent. The library ignores
 * them and passes `null` (nullable types) or `0` (Int) when an argument is omitted, which
 * [NoteCommands] handles.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@AppFunctionServiceEntryPoint(
    serviceName = "NotesAppFunctionService",
    appFunctionXmlFileName = "notes_app_functions",
)
abstract class BaseNotesAppFunctionService : AppFunctionService(), KoinComponent {

    private val noteCommands: NoteCommands by inject()

    /**
     * Creates a new note in the user's NoteMark notebook.
     *
     * @param title The note's title. Required and must not be blank.
     * @param content The note's text body. Omit for an empty note.
     * @return The created note, including its id.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun createNote(title: String, content: String? = null): AppFunctionNote =
        withContext(Dispatchers.IO) {
            noteCommands.createNote(title, content).getOrThrow().toAppFunctionNote()
        }

    /**
     * Changes the title and/or content of an existing note. Fields that are omitted stay
     * unchanged. The content is replaced, not appended to: to add text, read the note with
     * getNote and pass the combined content.
     *
     * @param noteId The id of the note, as returned by findNotes, getNote or createNote.
     * @param title The new title. Omit to keep the current title. Must not be blank.
     * @param content The new full content. Omit to keep the current content.
     * @return The updated note.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun updateNote(
        noteId: String,
        title: String? = null,
        content: String? = null,
    ): AppFunctionNote =
        withContext(Dispatchers.IO) {
            noteCommands.updateNote(noteId, title, content).getOrThrow().toAppFunctionNote()
        }

    /**
     * Returns a single note with its full content.
     *
     * @param noteId The id of the note, as returned by findNotes or createNote.
     * @return The note.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getNote(noteId: String): AppFunctionNote =
        withContext(Dispatchers.IO) {
            noteCommands.getNote(noteId).getOrThrow().toAppFunctionNote()
        }

    /**
     * Searches the user's notes. Matches notes whose title or content contains the query
     * (case-insensitive), most recently edited first. Use it to find a note's id before
     * updating it.
     *
     * @param query Text to look for in titles and contents. Omit or leave blank to list the
     *   most recently edited notes.
     * @param limit Maximum number of notes to return, from 1 to 25. Defaults to 10.
     * @return The matching notes with a preview of their content; empty when nothing matches.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun findNotes(
        query: String? = null,
        limit: Int = NoteCommands.DEFAULT_SEARCH_LIMIT,
    ): List<AppFunctionNoteSearchResult> =
        withContext(Dispatchers.IO) {
            noteCommands.findNotes(query, limit).getOrThrow().map { it.toAppFunctionSearchResult() }
        }
}

private fun <T> Result<T, NoteCommandError>.getOrThrow(): T = when (this) {
    is Result.Success -> data
    is Result.Error -> throw when (error) {
        NoteCommandError.NOT_LOGGED_IN -> AppFunctionPermissionRequiredException(
            "The user is not logged in to NoteMark. Ask them to open the app and log in.",
        )
        NoteCommandError.BLANK_TITLE -> AppFunctionInvalidArgumentException(
            "A note title must not be blank.",
        )
        NoteCommandError.NOTHING_TO_UPDATE -> AppFunctionInvalidArgumentException(
            "Provide a new title, a new content, or both.",
        )
        NoteCommandError.NOTE_NOT_FOUND -> AppFunctionElementNotFoundException(
            "No note with this id. Use findNotes to look up the note's id.",
        )
    }
}
