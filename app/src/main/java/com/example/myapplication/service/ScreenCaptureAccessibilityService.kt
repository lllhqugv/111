package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.myapplication.R
import com.example.myapplication.ai.AiSettings
import com.example.myapplication.ai.DeepSeekClient
import com.example.myapplication.capture.CaptureSettings
import com.example.myapplication.capture.ScreenshotWatcher
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.Task
import com.example.myapplication.ocr.OcrEngine
import com.example.myapplication.overlay.OverlayPermission
import com.example.myapplication.overlay.TaskOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 借助无障碍能力截取屏幕，并在后台监听系统截图。
 * 仅在服务被系统连接后才可用。
 */
class ScreenCaptureAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var screenshotWatcher: ScreenshotWatcher? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        startWatchingScreenshots()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        // 服务被系统解绑（如用户在设置中关闭无障碍）时清理实例，
        // 后续调用 captureScreen 会因 instance 为 null 而被上层拦截并提示重新开启。
        stopWatchingScreenshots()
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stopWatchingScreenshots()
        scope.cancel()
        if (instance === this) instance = null
        super.onDestroy()
    }

    /** 截取当前屏幕，返回软件位图（ARGB_8888）。 */
    suspend fun captureScreen(): Bitmap = suspendCancellableCoroutine { continuation ->
        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    val hardwareBuffer = screenshot.hardwareBuffer
                    val bitmap = try {
                        Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshot.colorSpace)
                    } finally {
                        hardwareBuffer.close()
                    }
                    if (bitmap == null) {
                        continuation.resumeWithException(IllegalStateException("无法解码屏幕图像"))
                    } else {
                        continuation.resume(bitmap.copy(Bitmap.Config.ARGB_8888, false))
                    }
                }

                override fun onFailure(errorCode: Int) {
                    continuation.resumeWithException(IllegalStateException("截屏失败，错误码 $errorCode"))
                }
            },
        )
    }

    // ---- 系统截图自动识别 ----

    private fun startWatchingScreenshots() {
        if (screenshotWatcher != null) return
        screenshotWatcher = ScreenshotWatcher(this) { uri -> handleScreenshot(uri) }.also { it.start() }
    }

    private fun stopWatchingScreenshots() {
        screenshotWatcher?.stop()
        screenshotWatcher = null
    }

    /** 识别新截图中的任务，并弹出确认悬浮窗。 */
    private fun handleScreenshot(uri: Uri) {
        if (!CaptureSettings.autoCaptureEnabled(this)) return
        if (!OverlayPermission.canDrawOverlays(this)) {
            Log.w(TAG, "已截到新截图，但缺少悬浮窗权限，无法弹出确认框")
            return
        }

        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeScaledBitmap(uri) }
            if (bitmap == null) {
                Log.w(TAG, "无法读取截图：$uri")
                return@launch
            }

            val text = try {
                OcrEngine.recognize(bitmap)
            } catch (t: Throwable) {
                Log.w(TAG, "自动截图 OCR 失败", t)
                return@launch
            } finally {
                bitmap.recycle()
            }

            val apiKey = AiSettings.apiKey(this@ScreenCaptureAccessibilityService)
            if (apiKey.isEmpty()) {
                Log.w(TAG, "未配置 DeepSeek API Key，跳过自动识别")
                Toast.makeText(
                    this@ScreenCaptureAccessibilityService,
                    R.string.ai_key_missing,
                    Toast.LENGTH_LONG,
                ).show()
                return@launch
            }

            val parsed = try {
                DeepSeekClient.parseTask(apiKey, text)
            } catch (t: Throwable) {
                Log.w(TAG, "自动截图 AI 识别失败", t)
                Toast.makeText(
                    this@ScreenCaptureAccessibilityService,
                    getString(R.string.ai_failed, t.message ?: t::class.java.simpleName),
                    Toast.LENGTH_LONG,
                ).show()
                return@launch
            }
            if (parsed == null) {
                Log.i(TAG, "自动截图未识别出任务")
                return@launch
            }

            Log.i(TAG, "自动截图识别出任务：${parsed.content}")
            TaskOverlay.show(
                context = this@ScreenCaptureAccessibilityService,
                task = parsed,
                originalText = text,
                onSave = { name ->
                    scope.launch {
                        runCatching {
                            AppDatabase.getInstance(applicationContext).taskDao().insert(
                                Task(
                                    text = name,
                                    createdAt = System.currentTimeMillis(),
                                    dueAt = parsed.dueAt ?: 0L,
                                    originalText = text,
                                )
                            )
                        }.onFailure { Log.w(TAG, "自动保存任务失败", it) }
                    }
                },
                onIgnore = { },
            )
        }
    }

    /** 读取截图并按最长边 [maxDimension] 缩放，避免大图占用过多内存。 */
    private fun decodeScaledBitmap(uri: Uri, maxDimension: Int = 2048): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) return@runCatching null

        var sampleSize = 1
        while (maxOf(width, height) / sampleSize > maxDimension) sampleSize *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

    companion object {
        private const val TAG = "ScreenCaptureService"

        /** 服务被系统连接后的实例，未开启时为 null。 */
        @Volatile
        var instance: ScreenCaptureAccessibilityService? = null
            private set

        /** 该无障碍服务是否已在系统设置中开启。 */
        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false

            val expected = ComponentName(context, ScreenCaptureAccessibilityService::class.java)
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabled)
            for (component in splitter) {
                if (ComponentName.unflattenFromString(component) == expected) return true
            }
            return false
        }

        /** 跳转到系统无障碍设置页，引导用户开启本服务。 */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
