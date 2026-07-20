package com.cn.core.ui.view.frosted

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import com.cn.core.ui.BuildConfig
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * 背景模糊工具 — 通过 Aggregator 反射创建系统 BackgroundBlurDrawable，失败则 Fallback。
 *
 * ## 系统路径
 * ```
 * View.getViewRootImpl() → ViewRootImpl
 *   → Aggregator(ViewRootImpl) → createBackgroundBlurDrawable(Context)
 *   → 设为 View 背景 → 系统通过 setBounds 自动管理模糊区域
 *   → Aggregator.dispatchBlurTransactionIfNeeded() → float[] → SurfaceFlinger
 * ```
 *
 * @author cn
 * @time 2026/6/25
 */
object BackgroundBlurDrawable {
    private const val TAG = "BgBlurDrawable"
    private const val DEBUG = true
    private inline fun logD(msg: () -> String) { if (DEBUG) Log.d(TAG, msg()) }
    private inline fun logW(msg: () -> String) { Log.w(TAG, msg()) }
    private inline fun logE(msg: () -> String) { Log.e(TAG, msg()) }

    @Volatile private var sSysInit = false
    private var sSysAvailable = false
    private var sGetViewRootImpl: Method? = null
    private var sWindowSetBgBlurRadius: Method? = null // Window.setBackgroundBlurRadius(int)
    private var sVrAggField: java.lang.reflect.Field? = null
    private var sCreateMethod: Method? = null
    private var sSetBlurRadius: Method? = null
    private var sAggHasUpdates: Method? = null
    private var sAggHasRegions: Method? = null

    @SuppressLint("BlockedPrivateApi")
    private fun ensureSysInit() {
        if (sSysInit) return
        synchronized(this) {
            if (sSysInit) return
            sSysInit = true
            try {
                sGetViewRootImpl = View::class.java.getDeclaredMethod("getViewRootImpl").apply { isAccessible = true }
                logD { "getViewRootImpl() found" }

                val vrCls = Class.forName("android.view.ViewRootImpl")
                val aggCls = Class.forName("com.android.internal.graphics.drawable.BackgroundBlurDrawable\$Aggregator")
                val drawableCls = Class.forName("com.android.internal.graphics.drawable.BackgroundBlurDrawable")

                for (f in vrCls.declaredFields) {
                    if (aggCls.isAssignableFrom(f.type)) {
                        f.isAccessible = true
                        sVrAggField = f
                        logD { "ViewRootImpl Aggregator field: ${f.name} (${f.type.simpleName})" }
                        break
                    }
                }

                sCreateMethod = aggCls.getDeclaredMethod("createBackgroundBlurDrawable", Context::class.java).apply { isAccessible = true }
                sAggHasUpdates = aggCls.getDeclaredMethod("hasUpdates").apply { isAccessible = true }
                sAggHasRegions = aggCls.getDeclaredMethod("hasRegions").apply { isAccessible = true }
                logD { "createBackgroundBlurDrawable(Context), hasUpdates(), hasRegions() found" }

                try { sSetBlurRadius = drawableCls.getDeclaredMethod("setBlurRadius", Int::class.javaPrimitiveType).apply { isAccessible = true }; logD { "setBlurRadius found" } }
                catch (_: Exception) { }

                try {
                    val windowCls = Class.forName("android.view.Window")
                    sWindowSetBgBlurRadius = windowCls.getDeclaredMethod("setBackgroundBlurRadius", Int::class.javaPrimitiveType)
                    sWindowSetBgBlurRadius!!.isAccessible = true
                    logD { "Window.setBackgroundBlurRadius(int) found" }
                } catch (e: Exception) {
                    logD { "Window.setBackgroundBlurRadius not available: ${e.message}" }
                }

                sSysAvailable = true
                logD { "System init OK (vrAggField=${sVrAggField != null}, windowSetBgBlur=${sWindowSetBgBlurRadius != null})" }
            } catch (e: Exception) {
                logW { "System init: ${e.message}" }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @JvmStatic
    fun tryCreateSystem(view: View, blurRadius: Int = 25, enableWindowPipeline: Boolean = true): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        ensureSysInit()
        if (!sSysAvailable || !view.isAttachedToWindow) return null
        return try {
            if (enableWindowPipeline) {
                if (sWindowSetBgBlurRadius == null) {
                    logW { "Window.setBackgroundBlurRadius not available" }
                    return null
                }
                val window = resolveWindow(view)
                if (window == null) { logW { "Window null" }; return null }
                sWindowSetBgBlurRadius!!.invoke(window, blurRadius.coerceAtLeast(0))
                logW { ">>> Window.setBackgroundBlurRadius($blurRadius) CALLED on ${window.javaClass.simpleName} — this is WINDOW-LEVEL blur!" }
            } else {
                logD { "Window.setBackgroundBlurRadius SKIPPED (enableWindowPipeline=false)" }
            }

            val vr = sGetViewRootImpl!!.invoke(view) ?: return null.also { logW { "ViewRootImpl null" } }

            val agg: Any = if (sVrAggField != null) {
                val existing = sVrAggField!!.get(vr)
                if (existing != null) {
                    logD { "Using ViewRootImpl's existing Aggregator" }
                    existing
                } else {
                    val aggCls = Class.forName("com.android.internal.graphics.drawable.BackgroundBlurDrawable\$Aggregator")
                    val ctor = aggCls.getDeclaredConstructor(Class.forName("android.view.ViewRootImpl"))
                    ctor.isAccessible = true
                    logD { "Creating new Aggregator and storing in ViewRootImpl" }
                    val newAgg = ctor.newInstance(vr)
                    sVrAggField!!.set(vr, newAgg)
                    newAgg
                }
            } else {
                val aggCls = Class.forName("com.android.internal.graphics.drawable.BackgroundBlurDrawable\$Aggregator")
                val ctor = aggCls.getDeclaredConstructor(Class.forName("android.view.ViewRootImpl"))
                ctor.isAccessible = true
                logD { "Creating new Aggregator (no field access)" }
                ctor.newInstance(vr)
            }

            val d = sCreateMethod!!.invoke(agg, view.context) as? Drawable ?: return null
            sSetBlurRadius?.invoke(d, blurRadius.coerceAtLeast(0))

            val hasU = sAggHasUpdates?.invoke(agg) as? Boolean
            val hasR = sAggHasRegions?.invoke(agg) as? Boolean
            logD { "createSystem OK: ${(d as Any).javaClass.name}, hasUpdates=$hasU, hasRegions=$hasR" }
            d
        } catch (e: Exception) {
            logW { "createSystem: ${e.message}" }
            null
        }
    }

    @JvmStatic
    fun isSystemAvailable(): Boolean { ensureSysInit(); return sSysAvailable }

    @JvmStatic
    fun setBlurRadiusSys(drawable: Drawable?, radius: Int) {
        if (drawable == null || sSetBlurRadius == null) return
        try { sSetBlurRadius!!.invoke(drawable, radius.coerceAtLeast(0)) } catch (_: Exception) { }
    }

    /** 清零 Window.setBackgroundBlurRadius（机制 B 清理） */
    @JvmStatic
    fun clearWindowBackgroundBlurRadius(window: android.view.Window?) {
        if (window == null) { Log.w(TAG, "clearWindowBgBlurR: window=null"); return }
        if (sWindowSetBgBlurRadius == null) { Log.w(TAG, "clearWindowBgBlurR: sWindowSetBgBlurRadius=null"); return }
        try { sWindowSetBgBlurRadius!!.invoke(window, 0); Log.d(TAG, "clearWindowBgBlurR: OK (${window.javaClass.simpleName})") }
        catch (e: Exception) { Log.w(TAG, "clearWindowBgBlurR: ${e.message}") }
    }

    private fun resolveWindow(view: View): android.view.Window? {
        var ctx: Context = view.context
        while (ctx is ContextWrapper) { if (ctx is Activity) return ctx.window; ctx = ctx.baseContext }
        return null
    }

    // ================================================================
    //               Fallback Drawable
    // ================================================================
    class FallbackBlurDrawable(private var blurRadius: Float) : Drawable() {
        companion object {
            private const val REFRESH_MS = 2000L
            private const val MAX_FAILED_REFRESH_MS = 16000L
            private const val SHARED_CAPTURE_TTL_MS = 2000L // 共享截图有效期

            // 共享截图：同 Activity 的多个 FallbackBlurDrawable 复用同一张全屏截图
            private var sharedCapture: Bitmap? = null
            private var sharedCaptureTime = 0L
            private var sharedCaptureW = 0; private var sharedCaptureH = 0

            @Synchronized
            private fun tryGetSharedCapture(w: Int, h: Int): Bitmap? {
                val bmp = sharedCapture
                if (bmp != null && !bmp.isRecycled && sharedCaptureW == w && sharedCaptureH == h
                    && System.currentTimeMillis() - sharedCaptureTime < SHARED_CAPTURE_TTL_MS) {
                    return bmp.copy(bmp.config ?: Bitmap.Config.ARGB_8888, false) // 返回副本，各 View 独立裁剪
                }
                return null
            }

            @Synchronized
            private fun storeSharedCapture(bmp: Bitmap, w: Int, h: Int) {
                sharedCapture?.recycle(); sharedCapture = bmp.copy(bmp.config ?: Bitmap.Config.ARGB_8888, false)
                sharedCaptureTime = System.currentTimeMillis(); sharedCaptureW = w; sharedCaptureH = h
            }
        }

        private val mRegionRect = android.graphics.Rect()
        private var mView: View? = null
        private var mAttached = false
        private var mBlurNode: RenderNode? = null
        private var mCapture: Bitmap? = null
        private var mScreenW = 0; private var mScreenH = 0
        private val mHandler = Handler(Looper.getMainLooper())
        private var mRefresh: Runnable? = null
        private var mBusy = false
        // 性能优化：移动复用 + 指数退避
        private var mLastRegionL = -1; private var mLastRegionT = -1 // 上次位置
        private var mRenderEffect: RenderEffect? = null // 缓存
        private var mCachedRadius = -1f
        private var mFailedRefreshMs = if (REFRESH_MS < 2000) 2000L else REFRESH_MS // 失败退避时长

        fun setBlurRegion(l: Int, t: Int, r: Int, b: Int) {
            val moved = mRegionRect.left != l || mRegionRect.top != t
            mRegionRect.set(l, t, r, b)
            // 位置变化且屏幕内容未过期：仅平移裁剪，不重截屏
            if (moved && mCapture != null && !mCapture!!.isRecycled && !mBusy) {
                // 下次定时刷新会重新截屏；本次 draw 已自动通过 translate 使用新位置
                invalidateSelf()
            }
        }
        fun setBlurRadius(r: Float) {
            val nr = r.coerceAtLeast(1f)
            if (blurRadius == nr) return
            blurRadius = nr
            mRenderEffect = null // 下次 rebuild
            postCapture()
        }

        fun attach(view: View) {
            mView = view; mAttached = true
            val dm = view.context.resources.displayMetrics
            mScreenW = dm.widthPixels; mScreenH = dm.heightPixels
            mBlurNode = RenderNode("bg-blur-fb")
            postCapture(); scheduleRefresh()
        }
        fun detach() {
            mAttached = false; mView = null; stopRefresh()
            mCapture?.recycle(); mCapture = null
            mBlurNode?.discardDisplayList(); mBlurNode = null
            mRenderEffect = null
        }

        override fun draw(canvas: Canvas) {
            if (!mAttached) return
            val node = mBlurNode ?: return
            if (mRegionRect.isEmpty || !node.hasDisplayList()) return
            canvas.withSave {
                canvas.translate(-mRegionRect.left.toFloat(), -mRegionRect.top.toFloat())
                canvas.drawRenderNode(node)
            }
        }

        override fun setAlpha(a: Int) { }
        override fun setColorFilter(cf: ColorFilter?) { }
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        private fun postCapture() { if (!mBusy) { mBusy = true; mHandler.post { doCapture() } } }

        private fun doCapture() {
            val v = mView; if (v == null || !v.isAttachedToWindow) { mBusy = false; return }
            try {
                // 先查共享缓存（同屏其他 View 已截图）
                var bmp = tryGetSharedCapture(mScreenW, mScreenH)
                if (bmp == null) {
                    bmp = capture(v)
                    if (bmp != null) storeSharedCapture(bmp, mScreenW, mScreenH)
                }
                if (bmp == null) {
                    // 指数退避：失败后延迟翻倍，最多 MAX_FAILED_REFRESH_MS
                    mFailedRefreshMs = (mFailedRefreshMs * 2).coerceAtMost(MAX_FAILED_REFRESH_MS)
                    mHandler.postDelayed({ mBusy = false; postCapture() }, mFailedRefreshMs)
                    return
                }
                mFailedRefreshMs = REFRESH_MS // 成功后重置
                mCapture?.recycle(); mCapture = bmp
                buildBlurNode(bmp)
                invalidateSelf()
            } finally { mBusy = false }
        }

        private fun buildBlurNode(bmp: Bitmap) {
            val node = mBlurNode ?: return
            // 复用 RenderEffect
            if (mRenderEffect == null || mCachedRadius != blurRadius) {
                mRenderEffect = FrostedAnimatedGlowView.getOrCreateBlurEffect(blurRadius)
                mCachedRadius = blurRadius
            }
            node.setRenderEffect(mRenderEffect)
            node.setPosition(0, 0, mScreenW, mScreenH)
            val rc = node.beginRecording(); rc.drawBitmap(bmp, 0f, 0f, null); node.endRecording()
        }

        private fun capture(view: View): Bitmap? {
            // 优先使用缓存的反射方法
            var bmp = ScrShotCache.tryCapture(mScreenW, mScreenH)
            if (bmp == null) bmp = tryPixelCopy(view)
            if (bmp == null) logE { "capture: ALL failed" }
            return bmp
        }

        private fun tryPixelCopy(view: View): Bitmap? {
            val w = resolveWindow(view) ?: return null
            val bmp = createBitmap(mScreenW, mScreenH, Bitmap.Config.ARGB_8888)
            val ref = java.util.concurrent.atomic.AtomicReference<Bitmap?>()
            val latch = java.util.concurrent.CountDownLatch(1)
            mHandler.post {
                try { PixelCopy.request(w, bmp, { r -> if (r == PixelCopy.SUCCESS) ref.set(bmp) else bmp.recycle(); latch.countDown() }, mHandler) }
                catch (_: Exception) { bmp.recycle(); latch.countDown() }
            }
            try { latch.await(2000, java.util.concurrent.TimeUnit.MILLISECONDS) } catch (_: Exception) { }
            ref.get()?.let { logD { "PixelCopy OK" }; return it }; return null
        }

        private fun resolveWindow(view: View): android.view.Window? {
            var ctx: Context = view.context
            while (ctx is ContextWrapper) { if (ctx is Activity) return ctx.window; ctx = ctx.baseContext }
            return null
        }

        private fun scheduleRefresh() {
            stopRefresh()
            mRefresh = object : Runnable {
                override fun run() {
                    if (mAttached && mView?.isAttachedToWindow == true) {
                        postCapture()
                        mHandler.postDelayed(this, REFRESH_MS)
                    }
                }
            }
            mHandler.postDelayed(mRefresh!!, REFRESH_MS)
        }
        private fun stopRefresh() { mRefresh?.let { mHandler.removeCallbacks(it) }; mRefresh = null }
    }

    // ================================================================
    //               缓存的 SurfaceControl 反射 (Fallback 复用)
    // ================================================================
    private object ScrShotCache {
        @Volatile private var inited = false
        private var methodV1: Method? = null
        private var methodV2: Method? = null
        private var methodV3: Method? = null
        private var displayToken: Any? = null
        private val screenRectCache = android.graphics.Rect()
        private val handler = Handler(Looper.getMainLooper())

        fun tryCapture(w: Int, h: Int): Bitmap? {
            if (!inited) init()
            // v1: screenshot(Rect, int, int, int) — 无 token
            methodV1?.let { m ->
                try { screenRectCache.set(0, 0, w, h); return (m.invoke(null, screenRectCache, w, h, 0) as? Bitmap)?.also { logD { "SC v1 OK" } } } catch (_: Exception) { }
            }
            // v2: screenshot(int, int) — 无 token，无 Rect
            methodV2?.let { m ->
                try { return (m.invoke(null, w, h) as? Bitmap)?.also { logD { "SC v2 OK" } } } catch (_: Exception) { }
            }
            // v3: screenshot(IBinder, Rect, int, int, int) — 需要 display token
            if (methodV3 != null && displayToken != null) {
                try { screenRectCache.set(0, 0, w, h); return (methodV3!!.invoke(null, displayToken, screenRectCache, w, h, 0) as? Bitmap)?.also { logD { "SC v3 OK" } } } catch (_: Exception) { }
            }
            return null
        }

        @SuppressLint("BlockedPrivateApi")
        private fun init() {
            synchronized(this) {
                if (inited) return
                inited = true
                try {
                    val cls = Class.forName("android.view.SurfaceControl")
                    try { methodV1 = cls.getDeclaredMethod("screenshot", android.graphics.Rect::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType).apply { isAccessible = true } } catch (_: Exception) { }
                    try { methodV2 = cls.getDeclaredMethod("screenshot", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType).apply { isAccessible = true } } catch (_: Exception) { }
                    try {
                        methodV3 = cls.getDeclaredMethod("screenshot", android.os.IBinder::class.java, android.graphics.Rect::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType).apply { isAccessible = true }
                        val dc = Class.forName("android.view.Display"); val gi = dc.getDeclaredMethod("getInternal").apply { isAccessible = true }
                        val disp = gi.invoke(null)
                        val gt = cls.getDeclaredMethod("getPhysicalDisplayToken", android.os.IBinder::class.java).apply { isAccessible = true }
                        displayToken = gt.invoke(null, disp)
                    } catch (_: Exception) { }
                    logD { "ScrShotCache init: v1=${methodV1 != null}, v2=${methodV2 != null}, v3=${methodV3 != null}, tok=${displayToken != null}" }
                } catch (e: Exception) {
                    logW { "ScrShotCache init failed: ${e.message}" }
                }
            }
        }
    }
}
