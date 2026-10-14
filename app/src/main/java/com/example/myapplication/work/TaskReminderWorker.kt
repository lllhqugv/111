package com.example.myapplication.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.notify.NotificationPermission
import com.example.myapplication.notify.TaskNotifications

/**
 * 周期性扫描 Room 中的任务，对「24 小时内到期且尚未提醒」的任务推送系统通知。
 */
class TaskReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        // 通知权限关闭时不推送、也不标记，待用户开启后仍可提醒。
        if (!NotificationPermission.isEnabled(appContext)) return Result.success()

        val dao = AppDatabase.getInstance(appContext).taskDao()
        val now = System.currentTimeMillis()
        val threshold = now + REMIND_WINDOW_MILLIS

        dao.unnotifiedTasksDueWithin(now, threshold).forEach { task ->
            if (TaskNotifications.notify(appContext, task)) {
                dao.markNotified(task.id, now)
            }
        }
        return Result.success()
    }

    companion object {
        /** 提前提醒的时间窗：截止前 24 小时。 */
        const val REMIND_WINDOW_MILLIS = 24 * 60 * 60 * 1000L
    }
}
