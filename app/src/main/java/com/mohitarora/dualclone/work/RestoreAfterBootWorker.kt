package com.mohitarora.dualclone.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mohitarora.dualclone.data.repository.BootRestorer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/** Runs [BootRestorer] a while after the boot broadcast and retries while Shizuku is not ready. */
@HiltWorker
class RestoreAfterBootWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val restorer: BootRestorer
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        when (restorer.run(inputData.getLong(KEY_BROADCAST_MILLIS, 0L))) {
            BootRestorer.Outcome.DONE, BootRestorer.Outcome.DISABLED -> Result.success()
            BootRestorer.Outcome.RETRY -> retryOrGiveUp()
        }
    } catch (t: Throwable) {
        Timber.tag("RestoreAfterBoot").e(t, "Restore after boot failed")
        retryOrGiveUp()
    }

    private fun retryOrGiveUp(): Result = if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()

    companion object {
        const val KEY_BROADCAST_MILLIS = "boot_broadcast_millis"
        private const val UNIQUE_NAME = "restore_after_boot"

        // NOTE: Samsung's cleanup ran 9 to 39 seconds after the Dual Messenger profile unlocked on a Tab S10 Ultra. 90 seconds leaves a wide margin.
        private const val INITIAL_DELAY_SECONDS = 90L
        private const val BACKOFF_SECONDS = 30L
        private const val MAX_ATTEMPTS = 8

        /** Called by the boot receiver. A second boot replaces a run that has not finished. */
        fun schedule(context: Context, broadcastMillis: Long) {
            val request = OneTimeWorkRequestBuilder<RestoreAfterBootWorker>()
                .setInitialDelay(INITIAL_DELAY_SECONDS, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .setInputData(workDataOf(KEY_BROADCAST_MILLIS to broadcastMillis))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
