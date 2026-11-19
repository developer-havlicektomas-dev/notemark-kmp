package dev.havlicektomas.notemark.notes.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.havlicektomas.notemark.notes.domain.sync.isPermanentSyncFailure
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import dev.havlicektomas.notemark.core.domain.util.Result as DomainResult

/** Runs a full two-way notes sync; WorkManager retries with backoff on failure. */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val syncEngine: NotesSyncEngine by inject()

    override suspend fun doWork(): Result = when (val result = syncEngine.sync()) {
        is DomainResult.Success -> Result.success()
        // The server rejected a record; retrying with backoff won't change that.
        is DomainResult.Error if result.error.isPermanentSyncFailure() -> Result.success()
        is DomainResult.Error -> if (runAttemptCount < MAX_RETRIES) {
            Result.retry()
        } else {
            Result.failure()
        }
    }

    private companion object {
        const val MAX_RETRIES = 5
    }
}
