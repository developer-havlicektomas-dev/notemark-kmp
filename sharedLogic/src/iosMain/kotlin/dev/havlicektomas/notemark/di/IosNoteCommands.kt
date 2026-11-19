package dev.havlicektomas.notemark.di

import dev.havlicektomas.notemark.core.domain.util.Result
import dev.havlicektomas.notemark.notes.domain.Note
import dev.havlicektomas.notemark.notes.domain.commands.NoteCommandError
import dev.havlicektomas.notemark.notes.domain.commands.NoteCommands
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** One note or the [NoteCommandError] that prevented it (`error == null` means success). */
class IosNoteCommandResult(
    val note: IosNote?,
    val error: NoteCommandError?,
)

/** A list of notes or the [NoteCommandError] that prevented it (`error == null` means success). */
class IosNoteListCommandResult(
    val notes: List<IosNote>,
    val error: NoteCommandError?,
)

/**
 * Swift-friendly facade over [NoteCommands] for App Intents (Siri, Shortcuts,
 * Apple Intelligence) — the iOS counterpart of the Android App Functions.
 * Like [IosNotesService], it hides the generic `Result` and `Instant` behind
 * plain values: timestamps are epoch millis and a nullable error means success.
 */
class IosNoteCommands : KoinComponent {

    private val commands: NoteCommands by inject()

    @Throws(Exception::class)
    suspend fun createNote(title: String, content: String?): IosNoteCommandResult =
        commands.createNote(title, content).toIosResult()

    /** Null [title] / [content] keep the current value. */
    @Throws(Exception::class)
    suspend fun updateNote(id: String, title: String?, content: String?): IosNoteCommandResult =
        commands.updateNote(id, title, content).toIosResult()

    @Throws(Exception::class)
    suspend fun getNote(id: String): IosNoteCommandResult =
        commands.getNote(id).toIosResult()

    /**
     * Resolves several ids at once (App Intents `entities(for:)`). Ids that no
     * longer exist are left out; only NOT_LOGGED_IN is reported as an error.
     */
    @Throws(Exception::class)
    suspend fun getNotes(ids: List<String>): IosNoteListCommandResult {
        val notes = mutableListOf<IosNote>()
        for (id in ids) {
            when (val result = commands.getNote(id)) {
                is Result.Success -> notes += result.data.toIos()
                is Result.Error -> if (result.error == NoteCommandError.NOT_LOGGED_IN) {
                    return IosNoteListCommandResult(emptyList(), result.error)
                }
            }
        }
        return IosNoteListCommandResult(notes, null)
    }

    /**
     * Notes matching [query], newest edit first. A blank/null query returns the
     * most recent notes; [limit] below 1 uses the default, above 25 is capped.
     */
    @Throws(Exception::class)
    suspend fun findNotes(query: String?, limit: Int): IosNoteListCommandResult =
        when (val result = commands.findNotes(query, limit)) {
            is Result.Success -> IosNoteListCommandResult(result.data.map { it.toIos() }, null)
            is Result.Error -> IosNoteListCommandResult(emptyList(), result.error)
        }

    private fun Result<Note, NoteCommandError>.toIosResult(): IosNoteCommandResult = when (this) {
        is Result.Success -> IosNoteCommandResult(data.toIos(), null)
        is Result.Error -> IosNoteCommandResult(null, error)
    }
}
