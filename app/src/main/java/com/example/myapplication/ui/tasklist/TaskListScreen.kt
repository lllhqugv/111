package com.example.myapplication.ui.tasklist

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.R
import com.example.myapplication.ai.AiSettings
import com.example.myapplication.ai.DeepSeekClient
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.Task
import com.example.myapplication.ocr.OcrEngine
import com.example.myapplication.overlay.OverlayPermission
import com.example.myapplication.overlay.TaskOverlay
import com.example.myapplication.service.ScreenCaptureAccessibilityService
import com.example.myapplication.util.TimeFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "TaskListScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onOpenSettings: () -> Unit,
    onOpenTask: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getInstance(context).taskDao() }

    val tasks by remember {
        dao.observeAll()
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // 每分钟刷新一次，使剩余时间提示保持最新。
    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            nowTick = System.currentTimeMillis()
        }
    }

    var recognizing by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }

    fun fail(text: String) {
        message = text
        messageIsError = true
    }

    fun captureAndRecognize() {
        if (!ScreenCaptureAccessibilityService.isEnabled(context)) {
            fail(context.getString(R.string.capture_service_disabled))
            return
        }
        val service = ScreenCaptureAccessibilityService.instance
        if (service == null) {
            // 权限已开启但服务实例为空：多为服务被系统断开。
            fail(context.getString(R.string.capture_service_disconnected))
            return
        }

        recognizing = true
        message = null
        messageIsError = false
        scope.launch {
            try {
                val bitmap = try {
                    service.captureScreen()
                } catch (t: Throwable) {
                    Log.w(TAG, "截屏失败", t)
                    fail(context.getString(R.string.capture_failed, t.message ?: t::class.java.simpleName))
                    recognizedText = null
                    return@launch
                }

                val text = try {
                    OcrEngine.recognize(bitmap)
                } catch (t: Throwable) {
                    Log.w(TAG, "OCR 识别失败", t)
                    fail(context.getString(R.string.ocr_failed, t.message ?: t::class.java.simpleName))
                    recognizedText = null
                    return@launch
                } finally {
                    bitmap.recycle()
                }

                recognizedText = text
                Log.i(TAG, "OCR 识别结果:\n$text")

                val apiKey = AiSettings.apiKey(context)
                if (apiKey.isEmpty()) {
                    fail(context.getString(R.string.ai_key_missing))
                    return@launch
                }

                val parsed = try {
                    DeepSeekClient.parseTask(apiKey, text)
                } catch (t: Throwable) {
                    Log.w(TAG, "AI 识别失败", t)
                    fail(context.getString(R.string.ai_failed, t.message ?: t::class.java.simpleName))
                    return@launch
                }

                when {
                    parsed == null -> fail(context.getString(R.string.ai_no_task))

                    !OverlayPermission.canDrawOverlays(context) ->
                        fail(context.getString(R.string.overlay_permission_missing))

                    else -> TaskOverlay.show(
                        context = context,
                        task = parsed,
                        originalText = text,
                        onSave = { name ->
                            scope.launch {
                                try {
                                    dao.insert(
                                        Task(
                                            text = name,
                                            createdAt = System.currentTimeMillis(),
                                            dueAt = parsed.dueAt ?: 0L,
                                            originalText = text,
                                        )
                                    )
                                    message = context.getString(R.string.task_saved)
                                    messageIsError = false
                                } catch (t: Throwable) {
                                    Log.w(TAG, "保存任务失败", t)
                                    fail(context.getString(R.string.task_save_failed, t.message ?: t::class.java.simpleName))
                                }
                            }
                        },
                        onIgnore = { },
                    )
                }
            } catch (t: Throwable) {
                Log.w(TAG, "处理流程异常", t)
                fail(t.message ?: t::class.java.simpleName)
                recognizedText = null
            } finally {
                recognizing = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_task_list)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { if (!recognizing) captureAndRecognize() }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.action_add_task),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (recognizing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.ocr_in_progress))
            }

            message?.let {
                Text(
                    text = it,
                    color = if (messageIsError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
            }

            val text = recognizedText
            when {
                text != null -> {
                    Text(
                        text = stringResource(R.string.ocr_result_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    )
                }

                !recognizing && message == null && tasks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.task_list_empty),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                tasks.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = tasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                remainingText = TimeFormat.remaining(context, task.dueAt, nowTick),
                                onOpen = { onOpenTask(task.id) },
                                onToggleCompleted = { checked ->
                                    scope.launch { dao.setCompleted(task.id, checked) }
                                },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            dao.delete(task)
                                            message = context.getString(R.string.task_deleted)
                                            messageIsError = false
                                        } catch (t: Throwable) {
                                            Log.w(TAG, "删除任务失败", t)
                                            fail(context.getString(R.string.task_delete_failed, t.message ?: t::class.java.simpleName))
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    remainingText: String,
    onOpen: () -> Unit,
    onToggleCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onOpen),
        leadingContent = {
            Checkbox(checked = task.completed, onCheckedChange = onToggleCompleted)
        },
        headlineContent = {
            Text(
                text = task.text,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
            )
        },
        supportingContent = {
            Text(
                if (task.completed) stringResource(R.string.task_completed) else remainingText,
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                )
            }
        },
    )
}
