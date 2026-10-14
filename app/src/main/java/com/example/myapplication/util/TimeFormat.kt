package com.example.myapplication.util

import android.content.Context
import com.example.myapplication.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 任务时间的统一格式化，供任务列表、详情页与悬浮窗共用。 */
object TimeFormat {

    private val DUE = DateTimeFormatter.ofPattern("M月d日 HH:mm")

    fun due(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(DUE)

    /**
     * 相对截止时间的可读剩余时长，如「3 小时 25 分后截止」「已过期」。
     * [dueAt] <= 0 视为未设置截止时间。
     */
    fun remaining(context: Context, dueAt: Long, now: Long = System.currentTimeMillis()): String {
        if (dueAt <= 0L) return context.getString(R.string.remaining_none)
        val diff = dueAt - now
        if (diff <= 0L) return context.getString(R.string.remaining_overdue)

        val totalMinutes = diff / 60_000L
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> context.getString(R.string.remaining_days, days, hours)
            hours > 0 -> context.getString(R.string.remaining_hours, hours, minutes)
            else -> context.getString(R.string.remaining_minutes, minutes)
        }
    }
}
