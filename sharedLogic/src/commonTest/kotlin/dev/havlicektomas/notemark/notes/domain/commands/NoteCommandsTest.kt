package dev.havlicektomas.notemark.notes.domain.commands

import dev.havlicektomas.notemark.auth.domain.AuthInfo
import dev.havlicektomas.notemark.auth.domain.AuthTokens
import dev.havlicektomas.notemark.auth.domain.SessionStorage
import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.EmptyResult
import dev.havlicektomas.notemark.core.domain.util.Result
import dev.havlicektomas.notemark.notes.domain.Note
import dev.havlicektomas.notemark.notes.domain.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class NoteCommandsTest {

    private val t0 = Instant.fromEpochMilliseconds(1_000)
    private val t1 = Instant.fromEpochMilliseconds(2_000)

    private val clock = object : Clock {
        var now = t1
        override fun now(): Instant = now
    }
    private val session = FakeSessionStorage(loggedIn = true)
    private val repository = FakeNotesRepository()
    private val commands = NoteCommands(repository, session, clock)

    private val existing = Note(
        id = "n1",
        title = "Groceries",
        content = "Milk",
        createdAt = t0,
        lastEditedAt = t0,
    )

    @Test
    fun createNote_trimsTitle_defaultsContent_andStoresNote() = runTest {
        val result = commands.createNote("  Ideas  ", null)

        val note = assertIs<Result.Success<Note>>(result).data
        assertEquals("Ideas", note.title)
        assertEquals("", note.content)
        assertEquals(t1, note.createdAt)
        assertEquals(t1, note.lastEditedAt)
        assertEquals(note, repository.notes.value.single())
    }

    @Test
    fun createNote_blankTitle_isRejected() = runTest {
        assertEquals(Result.Error(NoteCommandError.BLANK_TITLE), commands.createNote("   ", "x"))
        assertTrue(repository.notes.value.isEmpty())
    }

    @Test
    fun everyCommand_whenLoggedOut_returnsNotLoggedIn() = runTest {
        session.loggedIn = false
        val expected = Result.Error(NoteCommandError.NOT_LOGGED_IN)

        assertEquals(expected, commands.createNote("Title", null))
        assertEquals(expected, commands.updateNote("n1", "Title", null))
        assertEquals(expected, commands.getNote("n1"))
        assertEquals(expected, commands.findNotes("x", 5))
    }

    @Test
    fun updateNote_changesOnlyGivenFields_andBumpsLastEdited() = runTest {
        repository.notes.value = listOf(existing)

        val result = commands.updateNote("n1", title = null, content = "Milk, eggs")

        val note = assertIs<Result.Success<Note>>(result).data
        assertEquals(existing.copy(content = "Milk, eggs", lastEditedAt = t1), note)
        assertEquals(note, repository.notes.value.single())
    }

    @Test
    fun updateNote_errors() = runTest {
        repository.notes.value = listOf(existing)

        assertEquals(Result.Error(NoteCommandError.NOTE_NOT_FOUND), commands.updateNote("nope", "T", null))
        assertEquals(Result.Error(NoteCommandError.NOTHING_TO_UPDATE), commands.updateNote("n1", null, null))
        assertEquals(Result.Error(NoteCommandError.BLANK_TITLE), commands.updateNote("n1", " ", null))
        assertEquals(listOf(existing), repository.notes.value)
    }

    @Test
    fun findNotes_clampsLimit_andTrimsQuery() = runTest {
        commands.findNotes("  milk ", 0)
        assertEquals("milk" to NoteCommands.DEFAULT_SEARCH_LIMIT, repository.lastSearch)

        commands.findNotes(null, 1_000)
        assertEquals("" to NoteCommands.MAX_SEARCH_LIMIT, repository.lastSearch)

        commands.findNotes("x", 3)
        assertEquals("x" to 3, repository.lastSearch)
    }
}

private class FakeSessionStorage(var loggedIn: Boolean) : SessionStorage {
    override suspend fun get(): AuthInfo? =
        if (loggedIn) AuthInfo("alice", AuthTokens("access", "refresh")) else null

    override suspend fun set(info: AuthInfo?) = Unit
    override suspend fun clear() = Unit
}

private class FakeNotesRepository : NotesRepository {
    override val notes = MutableStateFlow<List<Note>>(emptyList())
    var lastSearch: Pair<String, Int>? = null

    override suspend fun fetchNotes(): EmptyResult<DataError.Network> = Result.Success(Unit)

    override suspend fun getNote(id: String): Note? = notes.value.firstOrNull { it.id == id }

    override suspend fun searchNotes(query: String, limit: Int): List<Note> {
        lastSearch = query to limit
        return notes.value.filter { query in it.title || query in it.content }.take(limit)
    }

    override suspend fun upsertNote(note: Note): EmptyResult<DataError.Network> {
        notes.value = notes.value.filterNot { it.id == note.id } + note
        return Result.Success(Unit)
    }

    override suspend fun deleteNote(id: String): EmptyResult<DataError.Network> {
        notes.value = notes.value.filterNot { it.id == id }
        return Result.Success(Unit)
    }

    override suspend fun clearSyncedLocalData() = Unit
    override suspend fun clearAllLocalData() = Unit
}
