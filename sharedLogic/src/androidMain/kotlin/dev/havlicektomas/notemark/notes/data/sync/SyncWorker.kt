package dev.havlicektomas.notemark.notes.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import dev.havlicektomas.notemark.core.domain.util.Result as DomainResult

/** Runs a full two-way notes sync; WorkManager retries with backoff on failure. */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val syncEngine: NotesSyncEngine by inject()

    override suspend fun doWork(): Result = when (syncEngine.sync()) {
        is DomainResult.Success -> Result.success()
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
