package dev.havlicektomas.notemark.notes.data.sync

import dev.havlicektomas.notemark.notes.domain.sync.SyncInterval
import dev.havlicektomas.notemark.notes.domain.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * In-process scheduler for iOS: sync requests run immediately (the engine
 * no-ops offline and the queue keeps the changes). Periodic sync is a
 * foreground timer — it ticks while the process is alive and dies with it,
 * so the app re-asserts the schedule on launch/foreground via
 * `SyncManager.applyStoredSchedule()`. True background execution
 * (BGTaskScheduler) is intentionally out of scope.
 */
class IosSyncScheduler(private val syncEngine: NotesSyncEngine) : SyncScheduler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var periodicJob: Job? = null

    override fun requestSync() {
        scope.launch { syncEngine.sync() }
    }

    override fun schedulePeriodicSync(interval: SyncInterval) {
        periodicJob?.cancel()
        val minutes = interval.minutes ?: return
        periodicJob = scope.launch {
            while (isActive) {
                delay(minutes * 60_000L)
                syncEngine.sync()
            }
        }
    }

    override fun cancelPeriodicSync() {
        periodicJob?.cancel()
        periodicJob = null
    }
}
