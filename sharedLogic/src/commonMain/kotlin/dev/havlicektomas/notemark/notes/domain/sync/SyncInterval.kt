package dev.havlicektomas.notemark.notes.domain.sync

/** How often notes sync in the background. [minutes] is null for manual-only. */
enum class SyncInterval(val minutes: Long?) {
    MANUAL_ONLY(null),
    MINUTES_15(15),
    MINUTES_30(30),
    HOUR_1(60),
}
