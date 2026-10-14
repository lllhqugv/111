package com.example.myapplication.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * 相册读取权限（Android 13+ 的 READ_MEDIA_IMAGES）。
 * Android 14+ 若用户只授予「选定照片」，只能看到已选内容、读不到新截图，需提示改为「允许全部」。
 */
object MediaPermission {

    const val READ_IMAGES = Manifest.permission.READ_MEDIA_IMAGES
    private const val READ_VISUAL_USER_SELECTED = Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED

    fun hasFullAccess(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, READ_IMAGES) == PackageManager.PERMISSION_GRANTED

    /** 仅授予了「选定照片」的部分访问，无法自动读取新截图。 */
    fun hasLimitedAccess(context: Context): Boolean =
        !hasFullAccess(context) &&
            ContextCompat.checkSelfPermission(context, READ_VISUAL_USER_SELECTED) ==
            PackageManager.PERMISSION_GRANTED
}
