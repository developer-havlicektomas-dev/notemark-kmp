package dev.havlicektomas.notemark.notes.presentation.list

import dev.havlicektomas.notemark.core.presentation.util.toShortDate
import dev.havlicektomas.notemark.notes.domain.Note

/** A note shaped for the list grid: pre-formatted date and a content preview. */
data class NoteUi(
    val id: String,
    val title: String,
    val preview: String,
    val formattedDate: String,
)

fun Note.toNoteUi(): NoteUi = NoteUi(
    id = id,
    title = title,
    preview = content,
    formattedDate = createdAt.toShortDate(),
)
