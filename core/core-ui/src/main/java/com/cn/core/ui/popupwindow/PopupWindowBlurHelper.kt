package com.cn.core.ui.popupwindow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import androidx.core.graphics.withSave

/**
 * PopupWindow 背景模糊工具 — 可独立用于任意 PopupWindow。
 *
 * 推荐直接使用 [BlurPopupWindow]，内置双路径模糊逻辑。
 * 本工具用于需在不继承 BlurPopupWindow 的场景下手动配置。
 *
 * @author cn
 * @time 2026/6/29
 */
@SuppressLint("ObsoleteSdkInt")
object PopupWindowBlurHelper {

    private const val TAG = "PwBlurHelper"

    // ================================================================
    //               Window 级 Cross-Window Blur（API 31+）
    // ================================================================

    @JvmStatic
    fun applyWindowBlur(pw: PopupWindow, blurRadius: Int = 25, dimAmount: Float = 0.25f): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        if (blurRadius <= 0) return false
        try {
            val dv = findPopupDecorView(pw) ?: return false
            val wm = dv.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (!wm.isCrossWindowBlurEnabled) return false

            val lp = dv.layoutParams as WindowManager.LayoutParams
            val clamped = blurRadius.coerceIn(1, 25)
            lp.blurBehindRadius = clamped
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            wm.updateViewLayout(dv, lp)

            pw.setBackgroundDrawable(ColorDrawable(Color.argb((dimAmount * 255).toInt(), 0, 0, 0)))
            Log.d(TAG, "Window blur OK: radius=$clamped")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Window blur: ${e.message}")
            return false
        }
    }

    // ================================================================
    //               PixelCopy 截图模糊（通用 fallback）
    // ================================================================

    /**
     * 异步捕获 Activity 画面 → RenderEffect 模糊 → 设为 PopupWindow 背景。
     * 最可靠的 fallback，只需 Activity 引用，无需 root。
     */
    @JvmStatic
    fun applyPixelCopyBlur(pw: PopupWindow, blurRadius: Int, dimAmount: Float, activity: Activity) {
        val radius = blurRadius.coerceIn(1, 25)
        val handler = Handler(Looper.getMainLooper())
        val dm = activity.resources.displayMetrics
        val w = dm.widthPixels; val h = dm.heightPixels

        // 立即设暗色遮罩
        pw.setBackgroundDrawable(ColorDrawable(Color.argb((dimAmount * 255).toInt(), 0, 0, 0)))

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val latch = java.util.concurrent.CountDownLatch(1)
        var ok = false

        handler.post {
            try {
                PixelCopy.request(activity.window, bmp, { r ->
                    ok = r == PixelCopy.SUCCESS; latch.countDown()
                }, handler)
            } catch (_: Exception) { latch.countDown() }
        }

        Thread {
            try { latch.await(2000, java.util.concurrent.TimeUnit.MILLISECONDS) }
            catch (_: Exception) { }
            if (!ok || !pw.isShowing) { bmp.recycle(); return@Thread }

            val scale = 0.25f
            val sw = (w * scale).toInt().coerceAtLeast(1)
            val sh = (h * scale).toInt().coerceAtLeast(1)
            val small = Bitmap.createScaledBitmap(bmp, sw, sh, true)
            bmp.recycle()

            val node = RenderNode("blur")
            node.setRenderEffect(RenderEffect.createBlurEffect(radius * scale, radius * scale, Shader.TileMode.CLAMP))
            node.setPosition(0, 0, sw, sh)
            val rc = node.beginRecording(); rc.drawBitmap(small, 0f, 0f, null); node.endRecording()

            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(out); c.scale(1f / scale, 1f / scale); c.drawRenderNode(node)
            small.recycle(); node.discardDisplayList()

            handler.post {
                if (pw.isShowing) {
                    pw.setBackgroundDrawable(BitmapDrawable(activity.resources, out))
                    Log.d(TAG, "PixelCopy blur OK")
                } else out.recycle()
            }
        }.start()
    }

    // ================================================================
    //               清理
    // ================================================================

    @JvmStatic
    fun removeBlur(pw: PopupWindow) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) clearWindowBlur(pw)
        pw.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun clearWindowBlur(pw: PopupWindow) {
        try {
            val dv = findPopupDecorView(pw) ?: return
            val lp = dv.layoutParams as WindowManager.LayoutParams
            lp.blurBehindRadius = 0
            lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
            (dv.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).updateViewLayout(dv, lp)
        } catch (_: Exception) { }
    }

    private fun findPopupDecorView(pw: PopupWindow): View? {
        var v: View? = pw.contentView ?: return null
        repeat(3) {
            if (v?.layoutParams is WindowManager.LayoutParams) return v
            v = v?.parent as? View
        }
        return null
    }
}

// ================================================================
//               扩展函数
// ================================================================

fun PopupWindow.enableBlur(blurRadius: Int = 25, dimAmount: Float = 0.25f, activity: Activity? = null) {
    if (PopupWindowBlurHelper.applyWindowBlur(this, blurRadius, dimAmount)) return
    activity?.let { PopupWindowBlurHelper.applyPixelCopyBlur(this, blurRadius, dimAmount, it) }
}

fun PopupWindow.disableBlur() = PopupWindowBlurHelper.removeBlur(this)
