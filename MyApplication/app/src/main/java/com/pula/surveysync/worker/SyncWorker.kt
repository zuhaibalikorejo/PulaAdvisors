package com.pula.surveysync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.pula.surveysync.domain.sync.SyncEngine
import com.pula.surveysync.domain.model.TerminationReason
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for background sync operations
 * Integrates with Hilt for dependency injection
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncEngine: SyncEngine
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "survey_sync_work"
        private const val KEY_SUCCESS_COUNT = "success_count"
        private const val KEY_FAILURE_COUNT = "failure_count"
        private const val KEY_TERMINATION_REASON = "termination_reason"

        /**
         * Schedule periodic background sync
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        /**
         * Trigger immediate one-time sync
         */
        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_immediate",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
        }

        /**
         * Cancel all sync work
         */
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            // Set foreground info for long-running task
            setForeground(createForegroundInfo())

            // Perform sync
            val syncResult = syncEngine.sync()

            // Create output data
            val outputData = workDataOf(
                KEY_SUCCESS_COUNT to syncResult.successCount,
                KEY_FAILURE_COUNT to syncResult.failureCount,
                KEY_TERMINATION_REASON to syncResult.terminationReason.name
            )

            // Determine result based on sync outcome
            when {
                syncResult.isFullySuccessful -> Result.success(outputData)
                syncResult.terminationReason == TerminationReason.NETWORK_DEGRADED -> {
                    // Retry later for network issues
                    Result.retry()
                }
                syncResult.successCount > 0 -> {
                    // Partial success - consider it success but log failures
                    Result.success(outputData)
                }
                else -> {
                    // Complete failure
                    Result.failure(outputData)
                }
            }
        } catch (e: Exception) {
            Result.failure(
                workDataOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        // In a real app, you would create a notification here
        // For now, we'll create a minimal notification
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            "sync_channel"
        )
            .setContentTitle("Syncing surveys")
            .setContentText("Uploading survey responses...")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .build()

        return ForegroundInfo(1, notification)
    }
}

