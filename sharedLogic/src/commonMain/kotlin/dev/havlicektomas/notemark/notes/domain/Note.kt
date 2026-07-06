package dev.havlicektomas.notemark.notes.domain

import kotlinx.datetime.Instant

/**
 * A note as the app reasons about it. Timestamps are real [Instant]s; the data
 * layer round-trips them to/from the backend's ISO-8601 strings. The [id] is
 * generated client-side (a UUID) so notes can be created offline in Milestone 4.
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Instant,
    val lastEditedAt: Instant,
)
