package dev.havlicektomas.notemark.di

import dev.havlicektomas.notemark.core.domain.ConnectivityObserver
import dev.havlicektomas.notemark.core.domain.util.DataError
import dev.havlicektomas.notemark.core.domain.util.Result
import dev.havlicektomas.notemark.notes.domain.sync.SyncInterval
import dev.havlicektomas.notemark.notes.domain.sync.SyncManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Handle Swift keeps to stop a [IosSyncService] watcher (e.g. in `deinit`/`onDisappear`). */
class IosWatchHandle internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}

/**
 * Swift-friendly facade over [SyncManager] and [ConnectivityObserver]. Kotlin
 * `Flow`s become watch functions: the callback fires on the main thread with
 * every new value until the returned [IosWatchHandle] is cancelled. Instants
 * become epoch millis (nullable `KotlinLong` in Swift; nil = never synced).
 */
class IosSyncService : KoinComponent {

    private val syncManager: SyncManager by inject()
    private val connectivityObserver: ConnectivityObserver by inject()

    // A watcher failure must not reach propagateExceptionFinalResort (which
    // crashes the process on iOS); log it and keep the app alive instead.
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("NoteMark iOS watch failed: $throwable")
    }
    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main + exceptionHandler)

    fun watchIsConnected(onChange: (Boolean) -> Unit): IosWatchHandle =
        connectivityObserver.isConnected.watch(onChange)

    fun watchLastSyncedAt(onChange: (Long?) -> Unit): IosWatchHandle =
        syncManager.lastSyncedAt.watch { onChange(it?.toEpochMilliseconds()) }

    fun watchSyncInterval(onChange: (SyncInterval) -> Unit): IosWatchHandle =
        syncManager.syncInterval.watch(onChange)

    fun watchPendingChanges(onChange: (Long) -> Unit): IosWatchHandle =
        syncManager.pendingChangesCount.watch(onChange)

    /** Full two-way sync; returns the error or `null` on success. */
    suspend fun syncNow(): DataError.Network? =
        (syncManager.syncNow() as? Result.Error)?.error

    /** Persists the interval and (re)schedules or cancels the periodic timer. */
    suspend fun setSyncInterval(interval: SyncInterval) =
        syncManager.setSyncInterval(interval)

    /** Fire-and-forget sync (call on app foreground while logged in). */
    fun requestBackgroundSync() = syncManager.requestBackgroundSync()

    /** Re-arms the periodic timer from the stored interval (call on app start). */
    suspend fun applyStoredSchedule() = syncManager.applyStoredSchedule()

    private fun <T> Flow<T>.watch(onChange: (T) -> Unit): IosWatchHandle =
        IosWatchHandle(
            onEach { onChange(it) }
                .catch { println("NoteMark iOS watch failed: $it") }
                .launchIn(scope),
        )
}
