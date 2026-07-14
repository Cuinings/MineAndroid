package com.cn.core.ui.popupwindow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.annotation.RequiresApi

/**
 * 自带背景模糊的 PopupWindow — 继承 [BasicPopupWindow]。
 *
 * 自动捕获弹框下方画面 → RenderEffect GPU 模糊 → 设为背景。
 * 兼容 Application Context（构造器自动解析 Activity）。
 *
 * ## 模糊策略（按优先级）
 *
 * 1. **Window 级 Cross-Window Blur**（API 31+，零延时）
 *    FLAG_BLUR_BEHIND + blurBehindRadius → SurfaceFlinger
 *
 * 2. **PixelCopy 预截图**（可靠 fallback）
 *    showAtLocation 前异步截 Activity 画面 → RenderEffect 模糊 → 背景
 *
 * ## 使用示例
 * ```kotlin
 * val popup = BlurPopupWindow(context).apply {
 *     contentView = myView
 *     width = WRAP_CONTENT; height = WRAP_CONTENT
 *     blurRadius = 25
 *     dimAmount = 0.3f
 * }
 * popup.show()
 * ```
 *
 * @author cn
 * @time 2026/6/29
 */
@SuppressLint("ObsoleteSdkInt", "NewApi")
open class BlurPopupWindow : BasicPopupWindow {

    companion object {
        private const val TAG = "BlurPopupWindow"
    }

    // ==================== 模糊属性 ====================

    var blurEnabled: Boolean = true
    var blurRadius: Int = 25
        set(v) { field = v.coerceIn(0, 25) }
    var dimAmount: Float = 0.25f
        set(v) { field = v.coerceIn(0f, 1f) }

    private var blurApplied = false
    private val resolvedActivity: Activity?
    private val handler = Handler(Looper.getMainLooper())

    /** 缓存的模糊背景，reapplyBlur 时复用 */
    private var cachedBlurDrawable: Drawable? = null

    // ==================== 构造 ====================

    constructor(context: Context) : super(context) {
        resolvedActivity = context.toActivity()
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        resolvedActivity = context.toActivity()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        resolvedActivity = context.toActivity()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context, attrs, defStyleAttr, defStyleRes
    ) {
        resolvedActivity = context.toActivity()
    }

    // ==================== 显示 / 隐藏 ====================

    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        if (!blurEnabled || blurRadius <= 0) {
            super.showAtLocation(parent, gravity, x, y)
            return
        }
        // 先尝试零延时 Window 级模糊，失败则异步预截图
        showWithBlur(parent, gravity, x, y)
    }

    override fun dismiss() {
        removeBlur()
        super.dismiss()
    }

    // ==================== 核心：双路径模糊 ====================

    private fun showWithBlur(parent: View?, gravity: Int, x: Int, y: Int) {
        val act = resolvedActivity
        val clampedRadius = blurRadius.coerceIn(1, 25)

        // 同时走两条路径 — Window 级模糊零延时，预截图作为可靠保障
        // 先显示弹框
        super.showAtLocation(parent, gravity, x, y)

        if (!isShowing) return

        // 路径 1: Window 级 Cross-Window Blur（零延时，立即生效）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (tryWindowBlur(clampedRadius)) {
                Log.d(TAG, "Window blur OK → SurfaceFlinger")
                blurApplied = true
                cachedBlurDrawable = null
                return  // 成功，无需 fallback
            }
        }

        // 路径 2: 异步 PixelCopy 预截图（可靠 fallback）
        if (act != null) {
            captureAndApply(act, clampedRadius)
        } else {
            Log.w(TAG, "No Activity → blur unavailable")
            // 至少设个暗色遮罩
            setBackgroundDrawable(ColorDrawable(Color.argb((dimAmount * 255).toInt(), 0, 0, 0)))
            blurApplied = true
        }
    }

    // ---------- 路径 1: Window 级模糊 ----------

    @RequiresApi(Build.VERSION_CODES.S)
    private fun tryWindowBlur(radius: Int): Boolean {
        try {
            val decorView = findPopupDecorView() ?: return false
            val wm = decorView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (!wm.isCrossWindowBlurEnabled) return false

            val lp = decorView.layoutParams as WindowManager.LayoutParams
            lp.blurBehindRadius = radius
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            wm.updateViewLayout(decorView, lp)

            // 同时设暗色遮罩
            setBackgroundDrawable(ColorDrawable(Color.argb((dimAmount * 255).toInt(), 0, 0, 0)))
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Window blur error: ${e.message}")
            return false
        }
    }

    // ---------- 路径 2: PixelCopy 预截图 ----------

    private fun captureAndApply(act: Activity, radius: Int) {
        // 立即设暗色遮罩（PixelCopy 完成前过渡）
        setBackgroundDrawable(ColorDrawable(Color.argb((dimAmount * 255).toInt(), 0, 0, 0)))
        blurApplied = true

        val dm = act.resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels

        // 异步截图（不阻塞主线程）
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val latch = java.util.concurrent.CountDownLatch(1)
        var captureOk = false

        handler.post {
            try {
                PixelCopy.request(act.window, bmp, { result ->
                    captureOk = (result == PixelCopy.SUCCESS)
                    latch.countDown()
                }, handler)
            } catch (e: Exception) {
                Log.w(TAG, "PixelCopy error: ${e.message}")
                latch.countDown()
            }
        }

        // 在独立线程等待 PixelCopy 结果
        Thread {
            try { latch.await(2000, java.util.concurrent.TimeUnit.MILLISECONDS) }
            catch (_: Exception) { }

            if (!captureOk || !isShowing) {
                bmp.recycle()
                return@Thread
            }

            try {
                // 中位图缩放降采样（加速模糊）
                val scale = 0.25f
                val sw = (w * scale).toInt().coerceAtLeast(1)
                val sh = (h * scale).toInt().coerceAtLeast(1)
                val small = Bitmap.createScaledBitmap(bmp, sw, sh, true)
                bmp.recycle()

                // RenderEffect 模糊
                val node = RenderNode("blur-overlay")
                node.setRenderEffect(
                    RenderEffect.createBlurEffect(
                        radius * scale, radius * scale, Shader.TileMode.CLAMP
                    )
                )
                node.setPosition(0, 0, sw, sh)
                val rc = node.beginRecording()
                rc.drawBitmap(small, 0f, 0f, null)
                node.endRecording()

                // 渲染到最终 Bitmap（放大回原始尺寸 → GPU 双线性过滤天然平滑）
                val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(out)
                canvas.scale(1f / scale, 1f / scale)
                canvas.drawRenderNode(node)
                canvas.setBitmap(null)
                small.recycle()
                node.discardDisplayList()

                // 回调主线程设置背景
                handler.post {
                    if (!isShowing) { out.recycle(); return@post }
                    cachedBlurDrawable = BitmapDrawable(act.resources, out)
                    setBackgroundDrawable(cachedBlurDrawable)
                    Log.d(TAG, "PixelCopy blur OK: ${out.width}x${out.height}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Blur render error: ${e.message}", e)
                bmp.let { if (!it.isRecycled) it.recycle() }
            }
        }.start()
    }

    // ==================== 工具方法 ====================

    /**
     * 从 contentView 向上找到 PopupDecorView（持有 WindowManager.LayoutParams）。
     * PopupWindow 内部结构: PopupDecorView → [PopupBackgroundView] → contentView
     */
    private fun findPopupDecorView(): View? {
        var v: View? = contentView ?: return null
        repeat(3) {
            if (v?.layoutParams is WindowManager.LayoutParams) return v
            v = v?.parent as? View
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun clearWindowBlur() {
        try {
            val dv = findPopupDecorView() ?: return
            val lp = dv.layoutParams as WindowManager.LayoutParams
            lp.blurBehindRadius = 0
            lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
            val wm = dv.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.updateViewLayout(dv, lp)
        } catch (_: Exception) { }
    }

    private fun removeBlur() {
        if (!blurApplied) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) clearWindowBlur()
        cachedBlurDrawable?.let {
            (it as? BitmapDrawable)?.bitmap?.recycle()
        }
        cachedBlurDrawable = null
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        blurApplied = false
    }
}

/** 从任意 Context 向上解析 Activity */
fun Context.toActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
