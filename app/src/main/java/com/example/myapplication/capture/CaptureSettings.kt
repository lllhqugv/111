package com.example.myapplication.capture

import android.content.Context

/** 自动截图识别的开关偏好。 */
object CaptureSettings {

    private const val PREFS_NAME = "capture_prefs"
    private const val KEY_AUTO_ENABLED = "auto_capture_enabled"

    /** 默认开启；实际生效还需辅助服务运行且已授予相册读取权限。 */
    fun autoCaptureEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_ENABLED, true)

    fun setAutoCaptureEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_ENABLED, enabled)
            .apply()
    }
}
