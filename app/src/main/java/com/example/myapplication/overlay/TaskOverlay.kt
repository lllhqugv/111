package com.example.myapplication.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.myapplication.parse.ParsedTask
import com.example.myapplication.ui.theme.MyApplicationTheme

/**
 * 系统悬浮窗（TYPE_APPLICATION_OVERLAY）中的任务确认弹窗。
 * 同一时刻只保留一个，默认 [AUTO_DISMISS_MILLIS] 毫秒后自动消失。
 */
object TaskOverlay {

    const val AUTO_DISMISS_MILLIS = 8_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: ComposeView? = null
    private var overlayOwner: OverlayLifecycleOwner? = null
    private var windowManager: WindowManager? = null
    private var autoDismiss: Runnable? = null

    fun show(
        context: Context,
        task: ParsedTask,
        originalText: String?,
        onSave: (String) -> Unit,
        onIgnore: () -> Unit,
    ) {
        val appContext = context.applicationContext
        if (!OverlayPermission.canDrawOverlays(appContext)) return
        dismiss()

        val manager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val owner = OverlayLifecycleOwner().apply { onCreate() }

        val composeView = ComposeView(appContext).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                MyApplicationTheme {
                    OverlayTaskCard(
                        task = task,
                        originalText = originalText,
                        onNameEdited = { cancelAutoDismiss() },
                        onSave = { name ->
                            dismiss()
                            onSave(name)
                        },
                        onIgnore = {
                            dismiss()
                            onIgnore()
                        },
                    )
                }
            }
        }

        // 需要可获取输入焦点（去掉 FLAG_NOT_FOCUSABLE），任务名输入框才能弹出键盘。
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        try {
            manager.addView(composeView, params)
        } catch (t: Throwable) {
            owner.onDestroy()
            return
        }

        overlayView = composeView
        overlayOwner = owner
        windowManager = manager

        val dismissRunnable = Runnable { dismiss() }
        autoDismiss = dismissRunnable
        mainHandler.postDelayed(dismissRunnable, AUTO_DISMISS_MILLIS)
    }

    /** 用户开始编辑任务名后不再自动消失，避免输入到一半弹窗被收回。 */
    private fun cancelAutoDismiss() {
        autoDismiss?.let { mainHandler.removeCallbacks(it) }
        autoDismiss = null
    }

    fun dismiss() {
        autoDismiss?.let { mainHandler.removeCallbacks(it) }
        autoDismiss = null

        val view = overlayView
        overlayView = null
        if (view != null) {
            runCatching { windowManager?.removeViewImmediate(view) }
        }
        windowManager = null

        overlayOwner?.onDestroy()
        overlayOwner = null
    }
}

/** ComposeView 在 WindowManager 中需要一个自持的 owner，承载生命周期、ViewModelStore 与保存状态。 */
private class OverlayLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun onCreate() {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
