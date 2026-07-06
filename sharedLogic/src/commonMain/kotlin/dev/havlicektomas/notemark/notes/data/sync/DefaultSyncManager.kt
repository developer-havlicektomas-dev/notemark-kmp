package dev.havlicektomas.notemark.notes.data.sync

import dev.havlicektomas.notemark.auth.domain.SessionStorage
import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.EmptyResult
import dev.havlicektomas.notemark.notes.domain.sync.SyncInterval
import dev.havlicektomas.notemark.notes.domain.sync.SyncManager
import dev.havlicektomas.notemark.notes.domain.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant

class DefaultSyncManager(
    private val engine: NotesSyncEngine,
    private val metadata: SyncMetadataStore,
    private val scheduler: SyncScheduler,
    private val queue: SqlDelightSyncQueueDataSource,
    private val sessionStorage: SessionStorage,
) : SyncManager {

    override val lastSyncedAt: Flow<Instant?> = flow {
        val username = sessionStorage.get()?.username
        if (username == null) emitAll(flowOf(null)) else emitAll(metadata.lastSyncedAt(username))
    }

    override val syncInterval: Flow<SyncInterval> = metadata.syncInterval()

    override val pendingChangesCount: Flow<Long> = flow {
        val username = sessionStorage.get()?.username
        if (username == null) emitAll(flowOf(0L)) else emitAll(queue.pendingCountForUser(username))
    }

    override suspend fun syncNow(): EmptyResult<DataError.Network> = engine.sync()

    override suspend fun setSyncInterval(interval: SyncInterval) {
        metadata.setSyncInterval(interval)
        applySchedule(interval)
    }

    override fun requestBackgroundSync() = scheduler.requestSync()

    override suspend fun applyStoredSchedule() = applySchedule(metadata.getSyncInterval())

    private fun applySchedule(interval: SyncInterval) {
        if (interval == SyncInterval.MANUAL_ONLY) {
            scheduler.cancelPeriodicSync()
        } else {
            scheduler.schedulePeriodicSync(interval)
        }
    }
}
