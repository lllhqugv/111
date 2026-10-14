package com.example.myapplication.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** 任务的周期提醒扫描任务：注册为唯一周期任务，重复调用不会重复排队。 */
object TaskReminderScheduler {

    private const val UNIQUE_WORK_NAME = "task_reminder_scan"

    /** WorkManager 周期任务的最小间隔为 15 分钟。 */
    private const val INTERVAL_MINUTES = 15L

    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<TaskReminderWorker>(
            INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        ).build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
