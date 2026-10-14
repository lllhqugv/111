package com.example.myapplication.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.Task
import com.example.myapplication.util.TimeFormat

/** 任务到期提醒通知的渠道创建与推送。点击通知跳转到任务详情页。 */
object TaskNotifications {

    const val CHANNEL_ID = "task_reminders"
    const val EXTRA_TASK_ID = "extra_task_id"

    /** 从启动 Intent 中读取要打开的任务 id，无则返回 null。 */
    fun taskIdFrom(intent: Intent?): Long? {
        val id = intent?.getLongExtra(EXTRA_TASK_ID, -1L) ?: -1L
        return if (id >= 0L) id else null
    }

    /**
     * 推送一条任务到期通知。通知权限未开启时静默跳过并返回 false，
     * 以便调用方据此决定是否标记为已提醒。
     */
    fun notify(context: Context, task: Task): Boolean {
        val appContext = context.applicationContext
        if (!NotificationPermission.isEnabled(appContext)) return false

        ensureChannel(appContext)

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val requestCode = task.id.toInt()
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dueText = TimeFormat.due(task.dueAt)
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(appContext.getString(R.string.notify_title))
            .setContentText(appContext.getString(R.string.notify_content, task.text, dueText))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(appContext.getString(R.string.notify_content, task.text, dueText)),
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return runCatching {
            NotificationManagerCompat.from(appContext).notify(requestCode, notification)
            true
        }.getOrDefault(false)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notify_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notify_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }
}
