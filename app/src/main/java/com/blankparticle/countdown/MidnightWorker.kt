package com.blankparticle.countdown

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Refreshes all widgets just after midnight so the day count rolls over,
 * then schedules itself for the next midnight.
 */
class MidnightWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        CountdownWidget().updateAll(applicationContext)
        schedule(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "midnight_widget_refresh"

        fun schedule(context: Context) {
            val nextMidnight = LocalDate.now().plusDays(1).atStartOfDay()
            // Small buffer past midnight so the date has definitely changed
            val delay = Duration.between(LocalDateTime.now(), nextMidnight).toMillis() + 60_000

            val request = OneTimeWorkRequestBuilder<MidnightWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
