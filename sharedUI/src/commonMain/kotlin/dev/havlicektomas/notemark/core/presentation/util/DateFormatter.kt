package dev.havlicektomas.notemark.core.presentation.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val monthAbbreviations = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** Short "12 Jun" form used on note cards and detail metadata. */
fun Instant.toShortDate(): String {
    val dateTime = toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.dayOfMonth} ${monthAbbreviations[dateTime.monthNumber - 1]}"
}

/**
 * Sync-status form per the M4 spec: "Just now", relative within a day, then a
 * full "12 Jun 2026, 14:05" timestamp. "Never synced" is the null case,
 * handled by the caller.
 */
fun Instant.toSyncStatusTime(now: Instant = Clock.System.now()): String {
    val seconds = (now - this).inWholeSeconds
    if (seconds < 86_400) return toRelativeTime(now)
    val dateTime = toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "${dateTime.dayOfMonth} ${monthAbbreviations[dateTime.monthNumber - 1]} " +
        "${dateTime.year}, $hour:$minute"
}

/**
 * Relative form for the "Edited …" line on the View screen: "Just now",
 * "N minutes ago", "N hours ago", falling back to [toShortDate] past a day.
 */
fun Instant.toRelativeTime(now: Instant = Clock.System.now()): String {
    val seconds = (now - this).inWholeSeconds
    return when {
        seconds < 60 -> "Just now"
        seconds < 3600 -> {
            val minutes = seconds / 60
            "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
        }
        seconds < 86_400 -> {
            val hours = seconds / 3600
            "$hours ${if (hours == 1L) "hour" else "hours"} ago"
        }
        else -> toShortDate()
    }
}
