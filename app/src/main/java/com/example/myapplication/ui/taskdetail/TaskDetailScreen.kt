package com.example.myapplication.ui.taskdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.R
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.util.TimeFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(taskId) { AppDatabase.getInstance(context).taskDao() }
    val task by dao.observeById(taskId).collectAsStateWithLifecycle(initialValue = null)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_task_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    val current = task
                    if (current != null) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    dao.delete(current)
                                    onBack()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val current = task
        if (current == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.task_detail_missing),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = current.text,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = if (current.completed) stringResource(R.string.task_completed)
                        else TimeFormat.remaining(context, current.dueAt),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Button(
                        onClick = { scope.launch { dao.setCompleted(current.id, !current.completed) } },
                    ) {
                        Text(
                            stringResource(
                                if (current.completed) R.string.action_mark_undone
                                else R.string.action_mark_done,
                            )
                        )
                    }
                }
                HorizontalDivider()

                ListItem(
                    headlineContent = { Text(stringResource(R.string.detail_status)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                if (current.completed) R.string.detail_status_done
                                else R.string.detail_status_pending,
                            )
                        )
                    },
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.detail_created)) },
                    supportingContent = { Text(TimeFormat.due(current.createdAt)) },
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.detail_due)) },
                    supportingContent = {
                        Text(
                            if (current.dueAt > 0L) TimeFormat.due(current.dueAt)
                            else stringResource(R.string.remaining_none),
                        )
                    },
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.detail_original_text)) },
                    supportingContent = {
                        Text(
                            current.originalText?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.detail_original_none),
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
