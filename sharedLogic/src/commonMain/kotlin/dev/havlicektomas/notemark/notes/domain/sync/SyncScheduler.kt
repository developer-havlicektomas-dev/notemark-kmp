package dev.havlicektomas.notemark.notes.domain.sync

/**
 * Schedules background sync runs. On Android this is WorkManager (network-constrained,
 * survives reboots); on iOS a lightweight in-process fallback until the iOS UI lands.
 */
interface SyncScheduler {
    /** Ask for a sync as soon as connectivity allows. Safe to call while offline. */
    fun requestSync()

    fun schedulePeriodicSync(interval: SyncInterval)

    fun cancelPeriodicSync()
}
