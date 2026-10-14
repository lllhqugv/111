package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.myapplication.notify.TaskNotifications
import com.example.myapplication.status.TaskStatusService
import com.example.myapplication.ui.navigation.AppNavHost
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.work.TaskReminderScheduler

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    /** 由通知点击带入、待处理的一次性跳转任务 id。 */
    private val pendingTaskId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        runCatching { TaskReminderScheduler.ensureScheduled(this) }
            .onFailure { Log.w(TAG, "注册提醒任务失败", it) }
        pendingTaskId.value = TaskNotifications.taskIdFrom(intent)

        setContent {
            MyApplicationTheme {
                AppNavHost(
                    openTaskId = pendingTaskId.value,
                    onTaskOpened = { pendingTaskId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTaskId.value = TaskNotifications.taskIdFrom(intent)
    }

    override fun onResume() {
        super.onResume()
        // 应用回到前台时，按当前设置确保状态提示服务处于正确状态。
        TaskStatusService.syncState(this)
    }
}
