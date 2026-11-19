package dev.havlicektomas.notemark.notes.data

import dev.havlicektomas.notemark.notes.domain.Note
import kotlin.time.Instant

fun NoteResponse.toNote(): Note = Note(
    id = id,
    title = title,
    content = content,
    createdAt = Instant.parse(createdAt),
    lastEditedAt = Instant.parse(lastEditedAt),
)

fun Note.toRequest(): NoteRequest = NoteRequest(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt.toString(),
    lastEditedAt = lastEditedAt.toString(),
)
