package com.mohitarora.dualclone.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mohitarora.dualclone.data.repository.Reconciler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Reconciles every profile with the device. Replaces the old User 95 only sync. */
@HiltWorker
class ReconcileWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reconciler: Reconciler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        val report = reconciler.run()
        Timber.tag("ReconcileWorker").d("Reconcile finished: %s", report)
        Result.success()
    } catch (t: Throwable) {
        Timber.tag("ReconcileWorker").e(t, "Reconcile worker failed")
        Result.retry()
    }
}

@Singleton
class ReconcileScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    /** Registers the 12 hour battery friendly schedule. Safe to call on every start. */
    fun ensurePeriodic() {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(LEGACY_WORK_NAME)
        val request = PeriodicWorkRequestBuilder<ReconcileWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        wm.enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun runNow() {
        val request = OneTimeWorkRequestBuilder<ReconcileWorker>()
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ON_DEMAND_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val PERIODIC_NAME = "reconcile_periodic"
        const val ON_DEMAND_NAME = "reconcile_now"
        const val LEGACY_WORK_NAME = "user95_sync"
    }
}
