package com.example.myapplication.status

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.Task
import com.example.myapplication.data.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 承载「状态提示」常驻通知的前台服务。订阅 Room 中的任务并实时刷新通知：
 * 只展示未过期任务；当没有任何有效任务或展示方式为关闭时，自行结束。
 */
class TaskStatusService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var stopping = false

    private var latestTasks: List<Task> = emptyList()

    // 定时刷新，让「剩余时间」文案保持最新。
    private val ticker = object : Runnable {
        override fun run() {
            render(latestTasks)
            handler.postDelayed(this, TICK_MILLIS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        stopping = false
        TaskStatusNotifier.ensureChannel(this)
        try {
            ServiceCompat.startForeground(
                this,
                TaskStatusNotifier.NOTIFICATION_ID,
                TaskStatusNotifier.placeholder(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } catch (t: Throwable) {
            // 前台服务被系统拒绝（如权限被收回/被 MIUI 拦截）时，直接结束，避免崩溃。
            Log.w(TAG, "启动状态提示前台服务失败", t)
            stopSelf()
            return
        }
        startObserving()
        handler.postDelayed(ticker, TICK_MILLIS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        stopping = true
        handler.removeCallbacks(ticker)
        scope.cancel()
        super.onDestroy()
    }

    private fun startObserving() {
        scope.launch {
            AppDatabase.getInstance(applicationContext).taskDao().observeAll().collect { tasks ->
                latestTasks = tasks
                render(tasks)
            }
        }
    }

    private fun render(tasks: List<Task>) {
        if (stopping) return

        val mode = StatusSettings.mode(this)
        if (mode == StatusDisplayMode.OFF) {
            stopSelfCleanly()
            return
        }

        val now = System.currentTimeMillis()
        val active = tasks.filter { it.isActive(now) }
        if (active.isEmpty()) {
            // 全部完成/过期：移除通知并结束服务。
            stopSelfCleanly()
            return
        }

        latestTasks = active
        NotificationManagerCompat.from(this)
            .notify(TaskStatusNotifier.NOTIFICATION_ID, TaskStatusNotifier.build(this, mode, active))
    }

    private fun stopSelfCleanly() {
        if (stopping) return
        stopping = true
        handler.removeCallbacks(ticker)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val TAG = "TaskStatusService"
        private const val TICK_MILLIS = 60_000L

        /**
         * 根据当前设置同步服务状态：关闭则停止，否则确保前台服务在运行。
         * 应用回到前台或设置变更后调用。
         */
        fun syncState(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, TaskStatusService::class.java)
            runCatching {
                if (StatusSettings.mode(appContext) == StatusDisplayMode.OFF) {
                    appContext.stopService(intent)
                } else {
                    ContextCompat.startForegroundService(appContext, intent)
                }
            }.onFailure { Log.w(TAG, "同步状态提示服务失败", it) }
        }
    }
}
