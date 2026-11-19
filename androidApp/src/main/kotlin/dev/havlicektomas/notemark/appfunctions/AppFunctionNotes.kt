package dev.havlicektomas.notemark.appfunctions

import androidx.appfunctions.AppFunctionSerializable
import dev.havlicektomas.notemark.notes.domain.Note
import java.time.Instant
import kotlin.time.toJavaInstant

/** A note in the user's NoteMark notebook. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppFunctionNote(
    /** Stable identifier of the note. Pass it to updateNote or getNote. */
    val id: String,
    /** Title of the note. */
    val title: String,
    /** Full text content of the note. May be empty. */
    val content: String,
    /** When the note was created. */
    val createdAt: Instant,
    /** When the note was last edited. */
    val lastEditedAt: Instant,
)

/** A note found by a search, with a shortened preview of its content. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppFunctionNoteSearchResult(
    /** Stable identifier of the note. Pass it to updateNote or getNote. */
    val id: String,
    /** Title of the note. */
    val title: String,
    /** The beginning of the note's content. Call getNote with the id for the full text. */
    val contentPreview: String,
    /** True when contentPreview is shorter than the full content. */
    val isContentTruncated: Boolean,
    /** When the note was last edited. */
    val lastEditedAt: Instant,
)

internal const val CONTENT_PREVIEW_LENGTH = 300

internal fun Note.toAppFunctionNote() = AppFunctionNote(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt.toJavaInstant(),
    lastEditedAt = lastEditedAt.toJavaInstant(),
)

internal fun Note.toAppFunctionSearchResult() = AppFunctionNoteSearchResult(
    id = id,
    title = title,
    contentPreview = content.take(CONTENT_PREVIEW_LENGTH),
    isContentTruncated = content.length > CONTENT_PREVIEW_LENGTH,
    lastEditedAt = lastEditedAt.toJavaInstant(),
)
