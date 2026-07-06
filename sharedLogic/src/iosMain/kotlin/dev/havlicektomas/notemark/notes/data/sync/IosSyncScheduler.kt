package dev.havlicektomas.notemark.notes.data.sync

import dev.havlicektomas.notemark.notes.domain.sync.SyncInterval
import dev.havlicektomas.notemark.notes.domain.sync.SyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
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

    // Fire-and-forget sync must never crash the app: an escaping exception on
    // iOS reaches propagateExceptionFinalResort and terminates the process
    // (unlike Android, where the WorkManager CoroutineWorker absorbs failures).
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("NoteMark iOS sync failed: $throwable")
    }
    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    private var periodicJob: Job? = null

    override fun requestSync() {
        scope.launch { runSync() }
    }

    override fun schedulePeriodicSync(interval: SyncInterval) {
        periodicJob?.cancel()
        val minutes = interval.minutes ?: return
        periodicJob = scope.launch {
            while (isActive) {
                delay(minutes * 60_000L)
                // Swallow per-tick failures so a single bad sync doesn't kill the timer.
                runSync()
            }
        }
    }

    private suspend fun runSync() {
        try {
            syncEngine.sync()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            println("NoteMark iOS sync failed: $e")
        }
    }

    override fun cancelPeriodicSync() {
        periodicJob?.cancel()
        periodicJob = null
    }
}
