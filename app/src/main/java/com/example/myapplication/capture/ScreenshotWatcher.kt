package com.example.myapplication.capture

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log

/**
 * 监听系统相册中「截图」目录的变化，新截图落盘时回调其 Uri。
 * 依赖 READ_MEDIA_IMAGES 权限；未授权时查询会被系统拒绝，静默不回调。
 */
class ScreenshotWatcher(
    context: Context,
    private val onNewScreenshot: (Uri) -> Unit,
) {
    private val resolver = context.applicationContext.contentResolver
    private val handler = Handler(Looper.getMainLooper())
    private var lastSeenId: Long = -1L

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            checkLatest()
        }
    }

    fun start() {
        // 记录当前最新一张，避免启动时把既有截图当成新截图。
        lastSeenId = latestScreenshotId() ?: -1L
        resolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        Log.i(TAG, "开始监听系统截图，baseline id=$lastSeenId")
    }

    fun stop() {
        runCatching { resolver.unregisterContentObserver(observer) }
    }

    private fun checkLatest() {
        val id = latestScreenshotId() ?: return
        if (id == lastSeenId) return
        lastSeenId = id
        onNewScreenshot(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id))
    }

    companion object {
        private const val TAG = "ScreenshotWatcher"
    }

    private fun latestScreenshotId(): Long? = runCatching {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val args = arrayOf("%Screenshots%")
        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            sort,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }.getOrNull()
}
