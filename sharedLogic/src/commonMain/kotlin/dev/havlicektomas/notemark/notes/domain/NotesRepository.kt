package dev.havlicektomas.notemark.notes.domain

import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.EmptyResult
import kotlinx.coroutines.flow.Flow

/**
 * Source of truth for notes. Since Milestone 4 this is offline-first: the local
 * database backs [notes], writes land locally first and are queued for upload,
 * and the backend is reconciled by the sync engine.
 */
interface NotesRepository {

    /** Observable local notes for the logged-in user, newest edit first. */
    val notes: Flow<List<Note>>

    /** Runs a full two-way sync (upload pending changes, then pull remote). */
    suspend fun fetchNotes(): EmptyResult<DataError.Network>

    suspend fun getNote(id: String): Note?

    /**
     * Creates or updates a note locally and enqueues the change for upload.
     * Never touches the network, so it succeeds offline.
     */
    suspend fun upsertNote(note: Note): EmptyResult<DataError.Network>

    /** Deletes a note locally and enqueues the deletion for upload. */
    suspend fun deleteNote(id: String): EmptyResult<DataError.Network>

    /**
     * Clears local notes except rows still referenced by the sync queue, which
     * survive so pending changes can upload after the next login ("Clear local
     * data" and logout-without-sync).
     */
    suspend fun clearSyncedLocalData()

    /** Wipes notes, sync queue, and sync metadata (logout after a successful sync). */
    suspend fun clearAllLocalData()
}
