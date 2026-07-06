package dev.havlicektomas.notemark.notes.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.havlicektomas.notemark.notes.domain.sync.SyncInterval
import dev.havlicektomas.notemark.notes.domain.sync.SyncScheduler
import java.util.concurrent.TimeUnit

/**
 * WorkManager-backed sync: requests wait for connectivity, retry with backoff,
 * and periodic work persists across app restarts and device reboots.
 */
class WorkManagerSyncScheduler(private val context: Context) : SyncScheduler {

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_NOW_WORK,
            // Append so edits made while a sync runs still get their own pass.
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    override fun schedulePeriodicSync(interval: SyncInterval) {
        val minutes = interval.minutes ?: return
        val request = PeriodicWorkRequestBuilder<SyncWorker>(minutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override fun cancelPeriodicSync() {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_SYNC_WORK)
    }

    private companion object {
        const val SYNC_NOW_WORK = "notemark_sync_now"
        const val PERIODIC_SYNC_WORK = "notemark_periodic_sync"
    }
}
