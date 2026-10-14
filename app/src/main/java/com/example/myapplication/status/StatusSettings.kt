package com.example.myapplication.status

import android.content.Context

/** 状态提示的展示方式。 */
enum class StatusDisplayMode {
    /** 关闭，不显示任何状态提示。 */
    OFF,

    /** 侧边提示条：纵向列出最近待办。 */
    SIDE_BAR,

    /** 灵动岛样式：单行胶囊，只显示最近一条待办。 */
    DYNAMIC_ISLAND,
}

/** 状态提示偏好的持久化（SharedPreferences）。 */
object StatusSettings {

    private const val PREFS_NAME = "status_prefs"
    private const val KEY_MODE = "display_mode"

    fun mode(context: Context): StatusDisplayMode {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_MODE, null)
        return StatusDisplayMode.entries.firstOrNull { it.name == raw } ?: StatusDisplayMode.OFF
    }

    fun setMode(context: Context, mode: StatusDisplayMode) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }
}
