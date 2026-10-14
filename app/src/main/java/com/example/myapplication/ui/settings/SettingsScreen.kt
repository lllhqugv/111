package com.example.myapplication.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.myapplication.R
import com.example.myapplication.ai.AiSettings
import com.example.myapplication.capture.CaptureSettings
import com.example.myapplication.capture.MediaPermission
import com.example.myapplication.notify.NotificationPermission
import com.example.myapplication.overlay.OverlayPermission
import com.example.myapplication.service.ScreenCaptureAccessibilityService
import com.example.myapplication.status.MiuiPermission
import com.example.myapplication.status.StatusDisplayMode
import com.example.myapplication.status.StatusSettings
import com.example.myapplication.status.TaskStatusService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var serviceEnabled by remember {
        mutableStateOf(ScreenCaptureAccessibilityService.isEnabled(context))
    }
    var overlayAllowed by remember {
        mutableStateOf(OverlayPermission.canDrawOverlays(context))
    }
    var notificationsEnabled by remember {
        mutableStateOf(NotificationPermission.isEnabled(context))
    }
    var statusMode by remember { mutableStateOf(StatusSettings.mode(context)) }
    var autoCapture by remember { mutableStateOf(CaptureSettings.autoCaptureEnabled(context)) }
    var mediaFull by remember { mutableStateOf(MediaPermission.hasFullAccess(context)) }
    var mediaLimited by remember { mutableStateOf(MediaPermission.hasLimitedAccess(context)) }
    var apiKey by remember { mutableStateOf(AiSettings.apiKey(context)) }
    var apiKeyVisible by remember { mutableStateOf(false) }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        mediaFull = MediaPermission.hasFullAccess(context)
        mediaLimited = MediaPermission.hasLimitedAccess(context)
    }

    // 从系统设置返回后刷新各权限状态
    LifecycleResumeEffect(Unit) {
        serviceEnabled = ScreenCaptureAccessibilityService.isEnabled(context)
        overlayAllowed = OverlayPermission.canDrawOverlays(context)
        notificationsEnabled = NotificationPermission.isEnabled(context)
        mediaFull = MediaPermission.hasFullAccess(context)
        mediaLimited = MediaPermission.hasLimitedAccess(context)
        onPauseOrDispose { }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_media_projection)) },
                supportingContent = {
                    Text(
                        text = stringResource(
                            if (serviceEnabled) R.string.settings_status_enabled
                            else R.string.settings_status_disabled,
                        ),
                    )
                },
                trailingContent = {
                    TextButton(
                        onClick = { ScreenCaptureAccessibilityService.openAccessibilitySettings(context) },
                    ) {
                        Text(stringResource(R.string.action_go_enable))
                    }
                },
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_overlay)) },
                supportingContent = {
                    Text(
                        text = stringResource(
                            if (overlayAllowed) R.string.settings_status_enabled
                            else R.string.settings_status_disabled,
                        ),
                    )
                },
                trailingContent = {
                    TextButton(onClick = { OverlayPermission.openSettings(context) }) {
                        Text(stringResource(R.string.action_go_enable))
                    }
                },
            )
            if (OverlayPermission.isXiaomiDevice && !overlayAllowed) {
                HintText(stringResource(R.string.settings_overlay_xiaomi_hint))
            }
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_notification)) },
                supportingContent = {
                    Text(
                        text = stringResource(
                            if (notificationsEnabled) R.string.settings_status_enabled
                            else R.string.settings_status_disabled,
                        ),
                    )
                },
                trailingContent = {
                    TextButton(onClick = { NotificationPermission.openSettings(context) }) {
                        Text(stringResource(R.string.action_go_enable))
                    }
                },
            )
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_auto_capture))
            HintText(stringResource(R.string.settings_auto_capture_hint))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_enable)) },
                trailingContent = {
                    Switch(
                        checked = autoCapture,
                        onCheckedChange = { checked ->
                            autoCapture = checked
                            CaptureSettings.setAutoCaptureEnabled(context, checked)
                        },
                    )
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_media_permission)) },
                supportingContent = {
                    Text(
                        text = stringResource(
                            if (mediaFull) R.string.settings_status_enabled
                            else R.string.settings_status_disabled,
                        ),
                    )
                },
                trailingContent = {
                    if (!mediaFull) {
                        TextButton(onClick = { mediaPermissionLauncher.launch(MediaPermission.READ_IMAGES) }) {
                            Text(stringResource(R.string.action_grant))
                        }
                    }
                },
            )
            if (mediaLimited) {
                HintText(stringResource(R.string.settings_media_permission_limited))
            }
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_ai_section))
            HintText(stringResource(R.string.settings_ai_hint))
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    AiSettings.setApiKey(context, it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = { Text(stringResource(R.string.settings_ai_key)) },
                singleLine = true,
                visualTransformation = if (apiKeyVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Text(
                            stringResource(
                                if (apiKeyVisible) R.string.action_hide else R.string.action_show,
                            ),
                        )
                    }
                },
            )
            HintText(
                stringResource(
                    if (apiKey.isBlank()) R.string.settings_ai_key_missing
                    else R.string.settings_ai_key_configured,
                ),
            )
            HintText(stringResource(R.string.settings_ai_key_hint))
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_status_display))
            HintText(stringResource(R.string.settings_status_display_hint))

            StatusModeRow(
                label = stringResource(R.string.status_mode_off),
                selected = statusMode == StatusDisplayMode.OFF,
                onSelect = {
                    statusMode = StatusDisplayMode.OFF
                    StatusSettings.setMode(context, StatusDisplayMode.OFF)
                    TaskStatusService.syncState(context)
                },
            )
            StatusModeRow(
                label = stringResource(R.string.status_mode_sidebar),
                selected = statusMode == StatusDisplayMode.SIDE_BAR,
                onSelect = {
                    statusMode = StatusDisplayMode.SIDE_BAR
                    StatusSettings.setMode(context, StatusDisplayMode.SIDE_BAR)
                    TaskStatusService.syncState(context)
                },
            )
            StatusModeRow(
                label = stringResource(R.string.status_mode_island),
                selected = statusMode == StatusDisplayMode.DYNAMIC_ISLAND,
                onSelect = {
                    statusMode = StatusDisplayMode.DYNAMIC_ISLAND
                    StatusSettings.setMode(context, StatusDisplayMode.DYNAMIC_ISLAND)
                    TaskStatusService.syncState(context)
                },
            )

            if (OverlayPermission.isXiaomiDevice) {
                HorizontalDivider()
                SectionHeader(stringResource(R.string.settings_miui_section))
                HintText(stringResource(R.string.settings_miui_hint))
                MiuiRow(
                    label = stringResource(R.string.settings_miui_autostart),
                    onClick = { MiuiPermission.openAutoStart(context) },
                )
                MiuiRow(
                    label = stringResource(R.string.settings_miui_background_popup),
                    onClick = { MiuiPermission.openBackgroundPopup(context) },
                )
                MiuiRow(
                    label = stringResource(R.string.settings_miui_battery),
                    onClick = { MiuiPermission.openBatterySettings(context) },
                )
                SectionHeader(stringResource(R.string.settings_miui_keepalive))
                HintText(stringResource(R.string.settings_miui_keepalive_hint))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun StatusModeRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onSelect),
        headlineContent = { Text(label) },
        trailingContent = {
            RadioButton(selected = selected, onClick = onSelect)
        },
    )
}

@Composable
private fun MiuiRow(label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.action_open))
            }
        },
    )
}
