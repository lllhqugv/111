package com.example.myapplication.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.parse.ParsedTask
import com.example.myapplication.util.TimeFormat

/**
 * 任务确认卡片：任务名可编辑（默认填 AI 给出的名字），下方展示截止时间与识别原文预览。
 */
@Composable
fun OverlayTaskCard(
    task: ParsedTask,
    originalText: String?,
    onNameEdited: () -> Unit,
    onSave: (String) -> Unit,
    onIgnore: () -> Unit,
) {
    var name by remember { mutableStateOf(task.content) }

    Card(
        modifier = Modifier
            .padding(12.dp)
            .widthIn(min = 240.dp, max = 320.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.overlay_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onNameEdited()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.overlay_name_label)) },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = task.dueAt
                    ?.let { stringResource(R.string.overlay_due, TimeFormat.due(it)) }
                    ?: stringResource(R.string.overlay_due_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val original = originalText?.takeIf { it.isNotBlank() }
            if (original != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = original,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onIgnore) {
                    Text(stringResource(R.string.overlay_ignore))
                }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = { onSave(name.trim()) },
                    enabled = name.isNotBlank(),
                ) {
                    Text(stringResource(R.string.overlay_save))
                }
            }
        }
    }
}
