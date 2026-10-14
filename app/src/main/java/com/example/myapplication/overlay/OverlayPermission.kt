package com.example.myapplication.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** 悬浮窗（TYPE_APPLICATION_OVERLAY）权限的查询与引导。 */
object OverlayPermission {

    /** 是否为小米/MIUI/Redmi/POCO 设备，这类机型悬浮窗入口与原生不同。 */
    val isXiaomiDevice: Boolean = run {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
            brand.contains("redmi") || brand.contains("poco")
    }

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** 打开悬浮窗权限设置，按机型选择合适的入口。 */
    fun openSettings(context: Context) {
        if (isXiaomiDevice && startSafely(context, miuiPermissionEditor(context.packageName))) return
        if (startSafely(context, standardOverlaySettings(context.packageName))) return
        startSafely(context, appDetails(context.packageName))
    }

    private fun standardOverlaySettings(packageName: String) =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))

    private fun appDetails(packageName: String) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

    private fun miuiPermissionEditor(packageName: String) =
        Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity",
            )
            putExtra("extra_pkgname", packageName)
            putExtra("extra_package_name", packageName)
        }

    private fun startSafely(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }
}
