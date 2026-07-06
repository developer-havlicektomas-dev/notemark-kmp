package dev.havlicektomas.notemark.notes.data

import kotlinx.serialization.Serializable

/**
 * Create/update payload. The backend upserts by [id] (POST and PUT behave the
 * same), and trusts the client-supplied [id] and timestamps. Timestamps are
 * ISO-8601 strings round-tripped verbatim by the server.
 */
@Serializable
data class NoteRequest(
    val id: String,
    val title: String,
    val content: String = "",
    val createdAt: String,
    val lastEditedAt: String,
)

@Serializable
data class NoteResponse(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val lastEditedAt: String,
)

@Serializable
data class NotesResponse(
    val notes: List<NoteResponse>,
    val total: Long,
)
