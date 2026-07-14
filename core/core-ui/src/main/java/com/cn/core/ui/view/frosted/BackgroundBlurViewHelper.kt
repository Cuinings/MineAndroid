package com.cn.core.ui.view.frosted

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import com.cn.core.ui.BuildConfig

class BackgroundBlurViewHelper(private val view: View) {
    companion object {
        private const val TAG = "BgBlurHelper"
        private inline fun logD(msg: () -> String) { if (BuildConfig.DEBUG) Log.d(TAG, msg()) }
        @JvmStatic fun attachTo(view: View, blurRadius: Float = 25f) = BackgroundBlurViewHelper(view).also { it.attach(blurRadius) } }

    private var blurDrawable: Drawable? = null
    private var usingSystem = false
    private var attached = false
    private val scrollCb = ViewTreeObserver.OnScrollChangedListener { onPositionChanged() }
    private val layoutCb = ViewTreeObserver.OnGlobalLayoutListener { onPositionChanged() }
    private var tracking = false
    private val posCache = IntArray(2) // 复用数组

    fun attach(blurRadius: Float = 25f) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || attached || !view.isAttachedToWindow) return
        blurDrawable = BackgroundBlurDrawable.tryCreateSystem(view, blurRadius.toInt())
        usingSystem = blurDrawable != null
        logD { "attach: ${if (usingSystem) "SYSTEM" else "FALLBACK"}" }
        if (!usingSystem) {
            val fb = BackgroundBlurDrawable.FallbackBlurDrawable(blurRadius); fb.attach(view); blurDrawable = fb
        } else {
            setupWindow(blurRadius.toInt())
        }
        view.background = blurDrawable
        installTracking(); onPositionChanged(); attached = true
    }

    fun detach() {
        if (!attached) return; uninstallTracking(); view.background = null
        val d = blurDrawable; blurDrawable = null; attached = false
        if (d is BackgroundBlurDrawable.FallbackBlurDrawable) d.detach()
    }

    fun onPositionChanged() {
        val d = blurDrawable ?: return
        if (usingSystem) return  // Aggregator 自动管理
        (d as? BackgroundBlurDrawable.FallbackBlurDrawable)?.let { fb ->
            if (!view.isAttachedToWindow || view.visibility != View.VISIBLE || view.width <= 0) return
            view.getLocationOnScreen(posCache)
            fb.setBlurRegion(posCache[0], posCache[1], posCache[0] + view.width, posCache[1] + view.height)
        }
    }

    fun setBlurRadius(r: Float) {
        val d = blurDrawable ?: return
        if (usingSystem) BackgroundBlurDrawable.setBlurRadiusSys(d, r.toInt())
        else (d as? BackgroundBlurDrawable.FallbackBlurDrawable)?.setBlurRadius(r)
    }

    private fun setupWindow(radius: Int) {
        val a = resolve() ?: return
        try { val p = a.window.attributes; p.blurBehindRadius = radius; p.flags = p.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND; a.window.attributes = p } catch (_: Exception) { }
    }
    private fun resolve(): Activity? { var c: Context = view.context; while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }; return null }

    private fun installTracking() { if (!tracking) { val v = view.viewTreeObserver; if (v.isAlive) { v.addOnScrollChangedListener(scrollCb); v.addOnGlobalLayoutListener(layoutCb); tracking = true } } }
    private fun uninstallTracking() { if (tracking) { val v = view.viewTreeObserver; if (v.isAlive) { v.removeOnScrollChangedListener(scrollCb); v.removeOnGlobalLayoutListener(layoutCb) }; tracking = false } }
}
