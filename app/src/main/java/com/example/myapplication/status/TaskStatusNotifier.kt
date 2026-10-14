package com.example.myapplication.status

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.Task
import com.example.myapplication.notify.TaskNotifications
import com.example.myapplication.util.TimeFormat

/**
 * 「状态提示」常驻通知的构建。两种展示方式共用同一条通知，仅自定义布局不同：
 * - 灵动岛样式：单行胶囊，显示最近一条待办。
 * - 侧边提示条：纵向列出最近三条待办。
 */
object TaskStatusNotifier {

    const val CHANNEL_ID = "task_status"
    const val NOTIFICATION_ID = 2001

    private const val SIDEBAR_MAX_LINES = 3

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.status_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.status_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    /** 前台服务启动瞬间使用的占位通知（尚无任务数据）。 */
    fun placeholder(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.status_channel_name))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    /** 依据展示方式构建常驻通知；[tasks] 必须为非空的有效任务列表。 */
    fun build(
        context: Context,
        mode: StatusDisplayMode,
        tasks: List<Task>,
    ): Notification {
        val ordered = tasks.sortedBy { if (it.dueAt > 0L) it.dueAt else Long.MAX_VALUE }
        val nearest = ordered.first()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(TaskNotifications.EXTRA_TASK_ID, nearest.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())

        when (mode) {
            StatusDisplayMode.DYNAMIC_ISLAND -> {
                val remaining = TimeFormat.remaining(context, nearest.dueAt)
                val remote = RemoteViews(context.packageName, R.layout.status_island).apply {
                    setTextViewText(R.id.status_island_text, nearest.text)
                    setTextViewText(R.id.status_island_remaining, remaining)
                }
                builder.setCustomContentView(remote)
                builder.setContentTitle(nearest.text)
                builder.setContentText(remaining)
            }

            StatusDisplayMode.SIDE_BAR -> {
                val title = context.getString(R.string.status_sidebar_title, ordered.size)
                val lineIds = intArrayOf(
                    R.id.status_sidebar_line1,
                    R.id.status_sidebar_line2,
                    R.id.status_sidebar_line3,
                )
                val remote = RemoteViews(context.packageName, R.layout.status_sidebar).apply {
                    setTextViewText(R.id.status_sidebar_title, title)
                    lineIds.forEachIndexed { index, id ->
                        val task = ordered.getOrNull(index)
                        setTextViewText(id, task?.let { context.getString(R.string.status_sidebar_line, it.text) } ?: "")
                    }
                }
                builder.setCustomBigContentView(remote)
                builder.setContentTitle(title)
                builder.setContentText(context.getString(R.string.status_sidebar_line, nearest.text))
            }

            StatusDisplayMode.OFF -> Unit
        }

        return builder.build()
    }
}
