package dev.havlicektomas.notemark.notes.domain.sync

import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.EmptyResult
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/** Sync state and controls for the logged-in user (flows resolve the user at collect time). */
interface SyncManager {

    /** Null means never synced. */
    val lastSyncedAt: Flow<Instant?>

    val syncInterval: Flow<SyncInterval>

    /** Number of local changes waiting to be uploaded. */
    val pendingChangesCount: Flow<Long>

    /** Runs a full two-way sync inline and reports the outcome (Settings "Sync now"). */
    suspend fun syncNow(): EmptyResult<DataError.Network>

    /** Persists the interval and (re)schedules or cancels the periodic background sync. */
    suspend fun setSyncInterval(interval: SyncInterval)

    /** Fire-and-forget sync via the platform scheduler (after edits, login, app start). */
    fun requestBackgroundSync()

    /** Re-asserts the periodic schedule from the stored interval (call on app start). */
    suspend fun applyStoredSchedule()
}
