package com.example.myapplication.status

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * MIUI 后台限制相关入口：自启动、后台弹出界面、应用详情（省电策略）。
 * 这些限制会导致前台服务/通知被系统清理，需引导用户手动放行。
 */
object MiuiPermission {

    /** 打开 MIUI「自启动」管理页。 */
    fun openAutoStart(context: Context): Boolean = startSafely(
        context,
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
        ),
    )

    /** 打开 MIUI「权限管理」页，其中包含「后台弹出界面」开关。 */
    fun openBackgroundPopup(context: Context): Boolean = startSafely(
        context,
        Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity",
            )
            putExtra("extra_pkgname", context.packageName)
            putExtra("extra_package_name", context.packageName)
        },
    )

    /** 打开应用详情页（可在此调整省电策略）。 */
    fun openBatterySettings(context: Context): Boolean = startSafely(
        context,
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ),
    )

    private fun startSafely(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }
}
