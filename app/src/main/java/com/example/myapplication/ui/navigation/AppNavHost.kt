package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.settings.SettingsScreen
import com.example.myapplication.ui.taskdetail.TaskDetailScreen
import com.example.myapplication.ui.tasklist.TaskListScreen

object Routes {
    const val TASK_LIST = "task_list"
    const val SETTINGS = "settings"
    const val TASK_DETAIL = "task_detail"
    const val ARG_TASK_ID = "taskId"

    fun taskDetail(taskId: Long) = "$TASK_DETAIL/$taskId"
}

/**
 * [openTaskId] 由通知点击等外部入口带入；非 null 时会自动跳转到对应任务详情页，
 * 跳转完成后回调 [onTaskOpened] 以便宿主清除该一次性事件。
 */
@Composable
fun AppNavHost(
    openTaskId: Long? = null,
    onTaskOpened: () -> Unit = {},
) {
    val navController = rememberNavController()

    LaunchedEffect(openTaskId) {
        if (openTaskId != null) {
            navController.navigate(Routes.taskDetail(openTaskId))
            onTaskOpened()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.TASK_LIST,
    ) {
        composable(Routes.TASK_LIST) {
            TaskListScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenTask = { taskId -> navController.navigate(Routes.taskDetail(taskId)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "${Routes.TASK_DETAIL}/{${Routes.ARG_TASK_ID}}",
            arguments = listOf(navArgument(Routes.ARG_TASK_ID) { type = NavType.LongType }),
        ) { entry ->
            val taskId = entry.arguments?.getLong(Routes.ARG_TASK_ID)
            if (taskId != null) {
                TaskDetailScreen(
                    taskId = taskId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
