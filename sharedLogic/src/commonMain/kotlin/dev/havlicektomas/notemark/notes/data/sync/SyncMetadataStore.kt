package dev.havlicektomas.notemark.notes.data.sync

import com.russhwolf.settings.Settings
import dev.havlicektomas.notemark.notes.domain.sync.SyncInterval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

/**
 * Sync bookkeeping in multiplatform-settings: per-user last-sync timestamp and
 * the automatic-sync interval. A revision counter makes reads observable
 * in-process (all writes go through this singleton).
 */
class SyncMetadataStore(private val settings: Settings) {

    private val revision = MutableStateFlow(0)

    fun lastSyncedAt(username: String): Flow<Instant?> = revision
        .map { getLastSyncedAt(username) }
        .distinctUntilChanged()

    fun getLastSyncedAt(username: String): Instant? =
        settings.getLongOrNull(lastSyncKey(username))?.let(Instant::fromEpochMilliseconds)

    fun setLastSyncedAt(username: String, instant: Instant) {
        settings.putLong(lastSyncKey(username), instant.toEpochMilliseconds())
        bumpRevision()
    }

    fun clearLastSyncedAt(username: String) {
        settings.remove(lastSyncKey(username))
        bumpRevision()
    }

    fun syncInterval(): Flow<SyncInterval> = revision
        .map { getSyncInterval() }
        .distinctUntilChanged()

    fun getSyncInterval(): SyncInterval =
        settings.getStringOrNull(KEY_SYNC_INTERVAL)
            ?.let { stored -> SyncInterval.entries.find { it.name == stored } }
            ?: SyncInterval.MANUAL_ONLY

    fun setSyncInterval(interval: SyncInterval) {
        settings.putString(KEY_SYNC_INTERVAL, interval.name)
        bumpRevision()
    }

    private fun bumpRevision() = revision.update { it + 1 }

    private fun lastSyncKey(username: String) = "${KEY_LAST_SYNCED_AT_PREFIX}$username"

    private companion object {
        const val KEY_LAST_SYNCED_AT_PREFIX = "last_synced_at_"
        const val KEY_SYNC_INTERVAL = "sync_interval"
    }
}
