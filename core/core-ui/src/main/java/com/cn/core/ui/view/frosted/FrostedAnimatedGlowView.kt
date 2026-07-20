package com.cn.core.ui.view.frosted

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.content.withStyledAttributes
import com.cn.core.ui.BuildConfig
import com.cn.core.ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 毛玻璃发光视图 — Compositor Blur + Bitmap fallback 双后端。
 *
 * 主路径：blurBehindRadius + BlurMaskManager → SurfaceFlinger GPU 模糊
 * 降级：WallpaperManager Bitmap + RenderNode + canvas.translate 裁剪
 */
@SuppressLint("Recycle", "ClickableViewAccessibility", "NewApi")
open class FrostedAnimatedGlowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : AnimatedStatefulGlowView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "BlurView"
        private const val MIN_RADIUS = 0
        private const val MAX_RADIUS = 100
        private const val MAX_DRAW_PIXELS = 4096L * 4096L
        private const val DEBUG = true
        private inline fun logD(msg: () -> String) { if (DEBUG) Log.d(TAG, msg()) }
        private inline fun logW(msg: () -> String) { Log.w(TAG, msg()) }
        private inline fun logE(msg: () -> String) { Log.e(TAG, msg()) }

        // ========== A3 overlay 反射缓存 (SurfaceControl 子层方案) ==========
        @Volatile private var ovRefInit = false
        private var ovGetViewRootImpl: java.lang.reflect.Method? = null
        private var ovGetSurfaceControl: java.lang.reflect.Method? = null
        private var ovTxCtor: java.lang.reflect.Constructor<*>? = null
        private var ovTxBgBlurRadius: java.lang.reflect.Method? = null
        private var ovTxSetPosition: java.lang.reflect.Method? = null
        private var ovTxSetBufferSize: java.lang.reflect.Method? = null
        private var ovTxSetLayer: java.lang.reflect.Method? = null
        private var ovTxSetRelativeLayer: java.lang.reflect.Method? = null
        private var ovTxShow: java.lang.reflect.Method? = null
        private var ovTxApply: java.lang.reflect.Method? = null

        @SuppressLint("BlockedPrivateApi", "SoonBlockedPrivateApi", "DiscouragedPrivateApi")
        private fun ensureOverlayRefInit() {
            if (ovRefInit) return
            synchronized(this) {
                if (ovRefInit) return
                ovRefInit = true
                try {
                    ovGetViewRootImpl = View::class.java.getDeclaredMethod("getViewRootImpl")
                        .apply { isAccessible = true }
                    val vrCls = Class.forName("android.view.ViewRootImpl")
                    ovGetSurfaceControl = vrCls.getDeclaredMethod("getSurfaceControl")
                        .apply { isAccessible = true }
                    val txCls = Class.forName("android.view.SurfaceControl\$Transaction")
                    ovTxCtor = txCls.getDeclaredConstructor().apply { isAccessible = true }
                    val scCls = Class.forName("android.view.SurfaceControl")
                    // setBackgroundBlurRadius(sc, int) — API 31+
                    ovTxBgBlurRadius = txCls.getDeclaredMethod(
                        "setBackgroundBlurRadius", scCls, Int::class.javaPrimitiveType
                    ).apply { isAccessible = true }
                    // setPosition(sc, float, float) — public but reflect for hidden compat
                    ovTxSetPosition = txCls.getDeclaredMethod(
                        "setPosition", scCls, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType
                    ).apply { isAccessible = true }
                    // setBufferSize(sc, int, int) — hidden
                    ovTxSetBufferSize = txCls.getDeclaredMethod(
                        "setBufferSize", scCls, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
                    ).apply { isAccessible = true }
                    // setLayer(sc, int) — hidden, 子层下沉到 parent 内容下方
                    ovTxSetLayer = txCls.getDeclaredMethod(
                        "setLayer", scCls, Int::class.javaPrimitiveType
                    ).apply { isAccessible = true }
                    // setRelativeLayer(sc, relativeToSC, int) — hidden, 相对 z-order
                    ovTxSetRelativeLayer = txCls.getDeclaredMethod(
                        "setRelativeLayer", scCls, scCls, Int::class.javaPrimitiveType
                    ).apply { isAccessible = true }
                    // show(sc) & apply() — 缓存避免每次反射
                    ovTxShow = txCls.getDeclaredMethod("show", scCls).apply { isAccessible = true }
                    ovTxApply = txCls.getDeclaredMethod("apply").apply { isAccessible = true }
                    if (BuildConfig.DEBUG) Log.d(TAG, "overlayRef OK (SC child)")
                } catch (e: Exception) {
                    logW { "overlayRef init: ${e.message}" }
                }
            }
        }

        // RenderEffect 全局缓存：相同 blurRadius 的 View 复用同一 GPU 对象，避免 N 份重复分配
        private val blurEffectCache = java.util.WeakHashMap<Float, android.graphics.RenderEffect>()
        fun getOrCreateBlurEffect(radius: Float): RenderEffect {
            return blurEffectCache.getOrPut(radius) {
                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            }
        }

        // TextureView 帧缓存：同一 TextureView 的多 View 共享一次 bitmap 捕获
        private val textureFrameCache = java.util.WeakHashMap<TextureView, Bitmap>()
        private var textureFrameCacheTime = 0L
        private const val TEXTURE_FRAME_CACHE_TTL_MS = 50L // 同一帧内复用（16ms vsync × 3）

        @Synchronized
        fun getCachedTextureFrame(tv: TextureView): Bitmap? {
            val bmp = textureFrameCache[tv]
            if (bmp != null && !bmp.isRecycled && System.currentTimeMillis() - textureFrameCacheTime < TEXTURE_FRAME_CACHE_TTL_MS) {
                return bmp.copy(bmp.config ?: android.graphics.Bitmap.Config.ARGB_8888, false)
            }
            return null
        }

        @Synchronized
        fun cacheTextureFrame(tv: TextureView, bmp: Bitmap) {
            textureFrameCache[tv]?.recycle()
            textureFrameCache[tv] = bmp.copy(bmp.config ?: android.graphics.Bitmap.Config.ARGB_8888, false)
            textureFrameCacheTime = System.currentTimeMillis()
        }

        /**
         * 反射获取 TextureView 的 SurfaceControl。
         * TextureView → mSurface (SurfaceTextureLayer) → mSurfaceControl (SurfaceControl)
         */
        @SuppressLint("BlockedPrivateApi")
        fun getTextureViewSurfaceControl(tv: TextureView): Any? {
            try {
                val mSurfaceField = TextureView::class.java.getDeclaredField("mSurface")
                mSurfaceField.isAccessible = true
                val surfaceLayer = mSurfaceField.get(tv) ?: return null
                // SurfaceTextureLayer 继承自 SurfaceTexture，有 mSurfaceControl 字段
                val scField = surfaceLayer.javaClass.getDeclaredField("mSurfaceControl")
                scField.isAccessible = true
                return scField.get(surfaceLayer)
            } catch (e: Exception) {
                Log.w(TAG, "getTextureViewSC: ${e.message}")
                return null
            }
        }
    }

    // ==================== 模糊模式 ====================
    enum class BlurMode { NONE, SYSTEM, FALLBACK, BITMAP, WALLPAPER, OVERLAY }

    // ==================== 可配置属性 ====================
    var blurEnabled: Boolean = true
        set(v) { if (field == v) return; field = v; if (isAttachedToWindow) { if (v) activateBlur() else deactivateBlur() } }

    var blurRadius: Int = 25
        set(v) { val c = v.coerceIn(MIN_RADIUS, MAX_RADIUS); if (field == c) return; field = c; if (blurEnabled) onRadiusChanged() }

    var useHiddenBlurPipeline: Boolean = false
        set(v) { if (field == v) return; field = v; if (blurEnabled && isAttachedToWindow) { deactivateBlur(); activateBlur() } }

    var sourceBitmap: Bitmap? = null
        set(v) { if (field === v) return; field = v; if (isAttachedToWindow) { if (blurEnabled) { deactivateBlur(); activateBlur() } else logW { "sourceBitmap set but blurEnabled=false" } } }

    var blurScaleFactor: Float = 0.1f
        set(v) { val c = v.coerceIn(0.01f, 1.0f); if (field == c) return; field = c
            when (blurMode) { BlurMode.BITMAP -> sourceBitmapDrawable?.invalidateCache(); BlurMode.WALLPAPER -> updateWallpaperRegion(); else -> {} } }

    var blurredDrawable: Drawable? = null
        set(v) { if (field === v) return; field = v; if (isAttachedToWindow) applyBlurredDrawable() }

    var blurredWallpaperBitmap: Bitmap? = null
        set(v) { if (field === v) return; field = v; if (isAttachedToWindow) applyWallpaperBitmap() }

    var forceFallback: Boolean = false
        set(v) { if (field == v) return; field = v; if (blurEnabled && isAttachedToWindow) { deactivateBlur(); activateBlur() } }

    var backgroundBlurOnly: Boolean = false
        set(v) { if (field == v) return; field = v; if (blurEnabled && isAttachedToWindow) { deactivateBlur(); activateBlur() } }

    /**
     * 动态壁纸时使用 A3 overlay 窗口方案（默认 true）。
     * overlay 方案：创建独立透明窗口精确覆盖 View 区域，FLAG_BLUR_BEHIND
     * 作用在该独立窗口上，实现 per-View 精确区域模糊，全屏壁纸不受影响。
     */
    var useLiveWallpaperOverlay: Boolean = true
        set(v) { if (field == v) return; field = v; if (blurEnabled && isAttachedToWindow) { deactivateBlur(); activateBlur() } }

    // ---- 纹理覆盖模糊 (TextureView SurfaceControl 子层方案) ----
    /** 绑定目标 TextureView，模糊即作用在该 TextureView 的 Surface 视频内容上 */
    var targetTextureView: TextureView? = null
        set(v) {
            if (field === v) return
            val wasActive = blurEnabled && blurMode != BlurMode.NONE
            if (wasActive) deactivateBlur()
            field = v
            if (wasActive) activateBlur()
        }

    var blurMode: BlurMode = BlurMode.NONE
        private set

    // ==================== 内部状态 ====================
    private var systemBlurDrawable: Drawable? = null
    private var fallbackDrawable: BackgroundBlurDrawable.FallbackBlurDrawable? = null
    private var sourceBitmapDrawable: SourceBitmapBlurDrawable? = null
    private var trackingInstalled = false
    private var selfSetWindowBlur = false
    private var selfSetBackgroundBlurRadius = false  // 机制 B 追踪
    private var clearedExternalBlurBehind = false
    private var clearedExternalBlurRadius = 0
    private var safeBitmapCache: Bitmap? = null
    private var cachedActivity: Activity? = null
    private val screenPosCache = IntArray(2)
    private var wallpaperCropDrawable: WallpaperCropDrawable? = null

    // ========== A3 overlay 状态 ==========
    private var overlaySC: Any? = null // SurfaceControl 子层 (壁纸模糊)
    private var textureCaptureDrawable: TextureCaptureBlurDrawable? = null // TextureView 帧捕获模糊

    // ==================== 生命周期 ====================
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cachedActivity = resolveActivity()
        when {
            blurredDrawable != null -> applyBlurredDrawable()
            blurredWallpaperBitmap != null -> applyWallpaperBitmap()
            blurEnabled -> activateBlur()
        }
    }

    override fun onDetachedFromWindow() {
        deactivateBlur()
        removeCallbacks(deferredActivator)
        cachedActivity = null
        super.onDetachedFromWindow()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (!isAttachedToWindow) return
        if (isVisible) {
            if (blurEnabled && blurMode == BlurMode.NONE && blurredDrawable == null && blurredWallpaperBitmap == null)
                post(deferredActivator)
        } else {
            if (blurMode != BlurMode.NONE) deactivateBlur()
        }
    }

    private fun recycleSafeBitmap() {
        safeBitmapCache?.let { if (!it.isRecycled) it.recycle() }
        safeBitmapCache = null
    }

    private val deferredActivator = Runnable {
        if (isAttachedToWindow && blurEnabled && width > 0 && height > 0) activateBlur()
    }

    private fun applyBlurredDrawable() {
        val d = blurredDrawable
        if (d != null) { deactivateBlur(); recycleSafeBitmap(); setLayerType(LAYER_TYPE_NONE, null); setBackgroundInternal(downscaleIfTooLarge(d)); blurMode = BlurMode.NONE }
        else { setBackgroundInternal(null); recycleSafeBitmap(); blurMode = BlurMode.NONE; if (blurEnabled) activateBlur() }
    }

    private fun downscaleIfTooLarge(d: Drawable): Drawable {
        val bd = d as? android.graphics.drawable.BitmapDrawable ?: return d
        val src = bd.bitmap; if (src.isRecycled) return d
        val pixels = src.width.toLong() * src.height.toLong(); if (pixels <= MAX_DRAW_PIXELS) return d
        val scale = Math.sqrt(MAX_DRAW_PIXELS.toDouble() / pixels).toFloat()
        val nw = (src.width * scale).toInt().coerceAtLeast(1); val nh = (src.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, nw, nh, true)
        safeBitmapCache?.recycle(); safeBitmapCache = scaled
        return android.graphics.drawable.BitmapDrawable(resources, scaled)
    }

    private fun applyWallpaperBitmap() {
        wallpaperCropDrawable = null
        val bmp = blurredWallpaperBitmap
        if (bmp != null && !bmp.isRecycled) {
            deactivateBlur(); val d = WallpaperCropDrawable(bmp, blurScaleFactor); wallpaperCropDrawable = d
            setLayerType(LAYER_TYPE_NONE, null); setBackgroundInternal(d); blurMode = BlurMode.WALLPAPER; updateWallpaperRegion()
        } else { setBackgroundInternal(null); blurMode = BlurMode.NONE; if (blurEnabled) activateBlur() }
    }

    // ==================== 位置追踪 ====================
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) { super.onLayout(changed, l, t, r, b); if (changed) onPositionChanged() }
    override fun setTranslationX(tx: Float) { super.setTranslationX(tx); if (blurEnabled && blurMode != BlurMode.NONE && blurMode != BlurMode.SYSTEM) onPositionChanged() }
    override fun setTranslationY(ty: Float) { super.setTranslationY(ty); if (blurEnabled && blurMode != BlurMode.NONE && blurMode != BlurMode.SYSTEM) onPositionChanged() }
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { super.onSizeChanged(w, h, ow, oh); if (!blurEnabled) return
        when { blurMode == BlurMode.FALLBACK -> updateFallbackRegion(); blurMode == BlurMode.OVERLAY -> { if (textureCaptureDrawable != null) updateTextureOverlayPosition() else updateOverlayPosition() }; else -> onPositionChanged() } }

    private fun onPositionChanged() {
        if (!blurEnabled || blurMode == BlurMode.NONE) return
        when (blurMode) {
            BlurMode.OVERLAY -> if (textureCaptureDrawable != null) updateTextureOverlayPosition() else updateOverlayPosition()
            BlurMode.SYSTEM -> {}
            BlurMode.BITMAP -> sourceBitmapDrawable?.updateRegion()
            BlurMode.WALLPAPER -> updateWallpaperRegion()
            else -> {}
        }
    }

    // ==================== 激活/停用 ====================
    private fun activateBlur() {
        if (blurredDrawable != null) return
        if (blurredWallpaperBitmap != null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) { logW { "API < 31" }; return }
        if (!isAttachedToWindow) return
        if (blurMode != BlurMode.NONE) return
        if (width <= 0 || height <= 0) { post(deferredActivator); return }

        val radius = blurRadius.coerceIn(MIN_RADIUS, MAX_RADIUS)
        logD { "activateBlur: radius=$radius, forceFallback=$forceFallback, bgBlurOnly=$backgroundBlurOnly, useHiddenPipeline=$useHiddenBlurPipeline, useLiveOverlay=$useLiveWallpaperOverlay" }

        val bmp = sourceBitmap
        if (bmp != null && !bmp.isRecycled) {
            logD { "activateBlur: path=BITMAP" }
            val d = SourceBitmapBlurDrawable(bmp, blurScaleFactor, radius.toFloat()); d.attach(this)
            sourceBitmapDrawable = d; setLayerType(LAYER_TYPE_HARDWARE, null); setBackgroundInternal(d); blurMode = BlurMode.BITMAP; return
        }

        // TextureView 覆盖 — 仅在目标可见时走帧捕获路径
        val tv = targetTextureView
        if (tv != null && tv.visibility == View.VISIBLE) {
            if (tryActivateTextureOverlay(tv, radius)) {
                logD { "activateBlur: path=TEXTURE_OVERLAY" }
                setLayerType(LAYER_TYPE_NONE, null); blurMode = BlurMode.OVERLAY; return
            }
            logW { "activateBlur: TextureView overlay failed → fall through" }
        } else if (tv != null) {
            logD { "activateBlur: TextureView not visible (visibility=${tv.visibility}), falling through to system path" }
        }

        // 动态壁纸 → 尝试 A3 overlay
        if (useLiveWallpaperOverlay && cachedActivity != null) {
            if (tryActivateOverlay(radius)) {
                logD { "activateBlur: path=OVERLAY" }
                setLayerType(LAYER_TYPE_NONE, null); blurMode = BlurMode.OVERLAY; return
            }
        }

        if (!forceFallback) {
            val act = cachedActivity
            val crossWindowOk = act?.let { BlurMaskManager.isCrossWindowBlurEnabled(it) } ?: false
            val hasMask = act?.let { BlurMaskManager.hasMask(it) } ?: false
            val windowFlags = try { act?.window?.attributes?.flags ?: 0 } catch (_: Exception) { 0 }
            val windowBlurR = try { act?.window?.attributes?.blurBehindRadius ?: 0 } catch (_: Exception) { 0 }
            val preSetBlurBehind = (windowFlags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND) != 0 && windowBlurR > 0
            logD { "activateBlur: SYSTEM check — crossWindowBlur=$crossWindowOk, hasMask=$hasMask, preSetBlurBehind=$preSetBlurBehind (flags=0x${Integer.toHexString(windowFlags)}, blurR=$windowBlurR)" }

            // P1: OEM 禁用跨窗口模糊时跳过 SYSTEM 路径，直接降级 FALLBACK
            if (act != null && !crossWindowOk) {
                logW { "activateBlur: SYSTEM skipped (OEM disabled cross-window blur) → FALLBACK" }
            } else {
                // 多 View 场景启用 Window pipeline：BlurMaskManager 共享一次 Window.setBackgroundBlurRadius
                val enablePipeline = if (backgroundBlurOnly) false else if (hasMask) (useHiddenBlurPipeline || preSetBlurBehind) else !useHiddenBlurPipeline
                selfSetBackgroundBlurRadius = enablePipeline
                logD { "activateBlur: SYSTEM — enablePipeline=$enablePipeline, selfSetBgBlurR=$selfSetBackgroundBlurRadius" }
                systemBlurDrawable = BackgroundBlurDrawable.tryCreateSystem(this, radius, enablePipeline)
                if (systemBlurDrawable != null) { logD { "activateBlur: path=SYSTEM" }; onSystemBlurReady(radius); return }
                logW { "activateBlur: tryCreateSystem returned null → FALLBACK" }
            }
        } else {
            logD { "activateBlur: forceFallback=true, skip SYSTEM" }
        }

        logD { "activateBlur: path=FALLBACK" }
        fallbackDrawable = BackgroundBlurDrawable.FallbackBlurDrawable(radius.toFloat()).also { fb -> fb.attach(this); updateFallbackRegion() }
        setBackgroundInternal(fallbackDrawable); blurMode = BlurMode.FALLBACK; installTracking()
    }

    private fun onSystemBlurReady(radius: Int) {
        val d = systemBlurDrawable ?: return
        setLayerType(LAYER_TYPE_NONE, null); setBackgroundInternal(d); applyBlurCornerRadius(d); blurMode = BlurMode.SYSTEM
        val act = cachedActivity
        val hasMask = act?.let { BlurMaskManager.hasMask(it) } ?: false
        logD { "onSystemBlurReady: radius=$radius, bgBlurOnly=$backgroundBlurOnly, hasMask=$hasMask, selfSetBgBlurR=$selfSetBackgroundBlurRadius" }
        if (backgroundBlurOnly) {
            logD { "onSystemBlurReady: backgroundBlurOnly → clearExternalBlurBehind()" }
            clearExternalBlurBehind()
        } else if (hasMask) {
            logD { "onSystemBlurReady: hasMask=true → setupWindowBlur($radius)" }
            setupWindowBlur(radius)
        } else {
            logD { "onSystemBlurReady: Window pipeline enabled (shared blur for all views)" }
            setupWindowBlur(radius)
        }
        uninstallTracking()
    }

    private fun clearExternalBlurBehind() {
        val act = cachedActivity ?: return
        try { val w = act.window; val p = w.attributes; val hasFlag = (p.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND) != 0
            if (hasFlag || p.blurBehindRadius > 0) {
                clearedExternalBlurBehind = true; clearedExternalBlurRadius = p.blurBehindRadius
                p.flags = p.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv(); p.blurBehindRadius = 0; w.attributes = p } }
        catch (_: Exception) { }
    }

    private fun restoreExternalBlurBehind() {
        if (!clearedExternalBlurBehind) return
        val act = cachedActivity ?: run { clearedExternalBlurBehind = false; return }
        try { val w = act.window; val p = w.attributes; p.flags = p.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            p.blurBehindRadius = clearedExternalBlurRadius; w.attributes = p } catch (_: Exception) { }
        clearedExternalBlurBehind = false; clearedExternalBlurRadius = 0
    }

    private fun applyBlurCornerRadius(drawable: Drawable) {
        val r = radii[0]; if (r <= 0f) return
        try { val m = (drawable as Any).javaClass.getDeclaredMethod("setCornerRadius", Float::class.javaPrimitiveType); m.isAccessible = true; m.invoke(drawable, r) } catch (_: Exception) { }
    }

    private fun setupWindowBlur(radius: Int) {
        try { val w = cachedActivity?.window ?: return; val p = w.attributes
            val alreadySet = (p.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND) != 0 && p.blurBehindRadius > 0
            logD { "setupWindowBlur: radius=$radius, alreadySet=$alreadySet, selfSetWindowBlur=$selfSetWindowBlur, currentFlags=0x${Integer.toHexString(p.flags)}, currentBlurR=${p.blurBehindRadius}" }
            if (radius > 0 && alreadySet) { logD { "setupWindowBlur: skip (already set)" }; return }
            if (radius <= 0 && !selfSetWindowBlur) { logD { "setupWindowBlur: skip (not self-set)" }; return }
            p.blurBehindRadius = radius
            p.flags = if (radius > 0) p.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND else p.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
            w.attributes = p; selfSetWindowBlur = radius > 0
            logD { "setupWindowBlur: done → flags=0x${Integer.toHexString(p.flags)}, blurR=$radius, selfSetWindowBlur=$selfSetWindowBlur" }
        } catch (e: Exception) { logW { "setupWindowBlur: ${e.message}" } }
    }

    private fun resolveActivity(): Activity? {
        var c: Context = context
        while (c is android.content.ContextWrapper) { if (c is Activity) return c; c = c.baseContext }; return null
    }

    private fun deactivateBlur() {
        logD { "deactivateBlur: mode=$blurMode, selfSetWindowBlur=$selfSetWindowBlur, selfSetBgBlurR=$selfSetBackgroundBlurRadius, bgBlurOnly=$backgroundBlurOnly" }
        when (blurMode) {
            BlurMode.OVERLAY -> if (textureCaptureDrawable != null) deactivateTextureOverlay() else deactivateOverlay()
            BlurMode.SYSTEM -> { setBackgroundInternal(null); systemBlurDrawable = null
                if (backgroundBlurOnly) { logD { "deactivateBlur: restoreExternalBlurBehind()" }; restoreExternalBlurBehind() }
                else if (selfSetWindowBlur) { logD { "deactivateBlur: setupWindowBlur(0)" }; setupWindowBlur(0) }
                if (selfSetBackgroundBlurRadius) { logD { "deactivateBlur: clearWindowBackgroundBlurRadius" }; BackgroundBlurDrawable.clearWindowBackgroundBlurRadius(cachedActivity?.window); selfSetBackgroundBlurRadius = false }
                setLayerType(LAYER_TYPE_HARDWARE, null) }
            BlurMode.FALLBACK -> { setBackgroundInternal(null); fallbackDrawable?.detach(); fallbackDrawable = null; uninstallTracking() }
            BlurMode.BITMAP -> { setBackgroundInternal(null); sourceBitmapDrawable?.detach(); sourceBitmapDrawable = null }
            BlurMode.WALLPAPER -> { setBackgroundInternal(null); wallpaperCropDrawable = null }
            BlurMode.NONE -> {}
        }; blurMode = BlurMode.NONE
    }

    private fun onRadiusChanged() {
        when (blurMode) {
            BlurMode.OVERLAY -> if (textureCaptureDrawable != null) updateTextureOverlayRadius() else updateOverlayRadius()
            BlurMode.SYSTEM -> BackgroundBlurDrawable.setBlurRadiusSys(systemBlurDrawable, blurRadius)
            BlurMode.FALLBACK -> fallbackDrawable?.setBlurRadius(blurRadius.toFloat())
            BlurMode.BITMAP -> sourceBitmapDrawable?.setBlurRadius(blurRadius.toFloat())
            else -> {}
        }
    }

    private fun updateFallbackRegion() {
        val fb = fallbackDrawable ?: return
        if (!isAttachedToWindow || visibility != View.VISIBLE || width <= 0 || height <= 0) return
        getLocationOnScreen(screenPosCache); fb.setBlurRegion(screenPosCache[0], screenPosCache[1], screenPosCache[0] + width, screenPosCache[1] + height)
    }

    private fun updateWallpaperRegion() {
        val wd = wallpaperCropDrawable ?: return
        if (!isAttachedToWindow || width <= 0 || height <= 0) return
        getLocationOnScreen(screenPosCache); wd.setCropRegion(screenPosCache[0], screenPosCache[1], width, height)
    }

    // ==================== 位置追踪 ====================
    private val scrollCb = ViewTreeObserver.OnScrollChangedListener {
        when (blurMode) { BlurMode.FALLBACK -> updateFallbackRegion(); BlurMode.OVERLAY -> if (textureCaptureDrawable != null) updateTextureOverlayPosition() else updateOverlayPosition(); else -> {} } }
    private val layoutCb = ViewTreeObserver.OnGlobalLayoutListener {
        when (blurMode) { BlurMode.FALLBACK -> updateFallbackRegion(); BlurMode.OVERLAY -> if (textureCaptureDrawable != null) updateTextureOverlayPosition() else updateOverlayPosition(); else -> {} } }

    private fun installTracking() { if (!trackingInstalled) { val v = viewTreeObserver; if (v.isAlive) { v.addOnScrollChangedListener(scrollCb); v.addOnGlobalLayoutListener(layoutCb); trackingInstalled = true } } }
    private fun uninstallTracking() { if (trackingInstalled) { val v = viewTreeObserver; if (v.isAlive) { v.removeOnScrollChangedListener(scrollCb); v.removeOnGlobalLayoutListener(layoutCb) }; trackingInstalled = false } }

    // ================================================================
    //   A3 OVERLAY — 动态壁纸 per-View 精确模糊（SurfaceControl 子层方案）
    //
    //   层级：Wallpaper(-2) → BlurChild(-1, 采样壁纸) → AppContent(0)
    //   BlurChild 由 App SurfaceControl 的子层承载，setLayer(-1) 下沉到
    //   App 内容下方、壁纸上方，setBackgroundBlurRadius 模糊壁纸。
    //   不创建 Window、不截图、零 CPU 开销、per-View 精确区域。
    // ================================================================

    /**
     * 以 App 窗口 SurfaceControl 为 parent 创建子层，
     * setLayer(-1) 下沉 + setBackgroundBlurRadius 模糊壁纸。
     */
    private fun tryActivateOverlay(radius: Int): Boolean {
        return false // 路径2(A3 Overlay)已屏蔽
        /* val act = cachedActivity ?: return false
        if (!isLiveWallpaper(context)) return false
        if (!BlurMaskManager.isCrossWindowBlurEnabled(act)) return false
        ensureOverlayRefInit()

        val r = radius.coerceIn(0, 25)
        val loc = IntArray(2); getLocationOnScreen(loc)
        val vw = width.coerceAtLeast(1); val vh = height.coerceAtLeast(1)

        // 获取 App 窗口的 SurfaceControl（作为 parent 和 z-order 基准）
        val appSC: Any
        try {
            val appVRI = ovGetViewRootImpl?.invoke(this@BlurView)
                ?: return false.also { logW { "overlay: ViewRootImpl null" } }
            appSC = ovGetSurfaceControl?.invoke(appVRI)
                ?: return false.also { logW { "overlay: SurfaceControl null" } }
        } catch (e: Exception) { logW { "overlay: getAppSC ${e.message}" }; return false }

        // 创建子 SurfaceControl，无 buffer，仅承载 blur 效果
        val blurSC: Any
        try {
            val builderCls = Class.forName("android.view.SurfaceControl\$Builder")
            val builder = builderCls.getDeclaredConstructor().newInstance()
            builderCls.getDeclaredMethod("setParent", Class.forName("android.view.SurfaceControl"))
                .invoke(builder, appSC)
            builderCls.getDeclaredMethod("setName", String::class.java)
                .invoke(builder, "BlurOverlay")
            blurSC = builderCls.getDeclaredMethod("build").invoke(builder)
        } catch (e: Exception) { logW { "overlay: build SC ${e.message}" }; return false }

        // Transaction: position + size + blur + z=-1(sub-content)
        try {
            val t = ovTxCtor!!.newInstance()
            ovTxSetPosition?.invoke(t, blurSC, loc[0].toFloat(), loc[1].toFloat())
            ovTxSetBufferSize?.invoke(t, blurSC, vw, vh)
            ovTxBgBlurRadius?.invoke(t, blurSC, r)
            ovTxSetLayer?.invoke(t, blurSC, -1)
            t.javaClass.getDeclaredMethod("show", Class.forName("android.view.SurfaceControl"))
                .invoke(t, blurSC)
            t.javaClass.getDeclaredMethod("apply").invoke(t)
        } catch (e: Exception) { logW { "overlay: tx ${e.message}" }; releaseSC(blurSC); return false }

        overlaySC = blurSC
        installTracking()
        logD { "OVERLAY OK: (${loc[0]},${loc[1]}) ${vw}x${vh} r=$r childOfApp" }
        return true */
    }

    private fun deactivateOverlay() {
        uninstallTracking()
        overlaySC?.let { releaseSC(it) }; overlaySC = null
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private fun updateOverlayPosition() {
        val sc = overlaySC ?: return
        if (!isAttachedToWindow || visibility != View.VISIBLE || width <= 0 || height <= 0) return
        val loc = IntArray(2); getLocationOnScreen(loc)
        val vw = width.coerceAtLeast(1); val vh = height.coerceAtLeast(1)
        try {
            val t = ovTxCtor!!.newInstance()
            ovTxSetPosition?.invoke(t, sc, loc[0].toFloat(), loc[1].toFloat())
            ovTxSetBufferSize?.invoke(t, sc, vw, vh)
            t.javaClass.getDeclaredMethod("apply").invoke(t)
        } catch (_: Exception) { }
    }

    private fun updateOverlayRadius() {
        val sc = overlaySC ?: return
        val r = blurRadius.coerceIn(0, 25)
        try {
            val t = ovTxCtor!!.newInstance()
            ovTxBgBlurRadius?.invoke(t, sc, r)
            t.javaClass.getDeclaredMethod("apply").invoke(t)
        } catch (_: Exception) { }
    }

    private fun releaseSC(sc: Any) {
        try {
            val t = ovTxCtor!!.newInstance()
            t.javaClass.getDeclaredMethod("hide", Class.forName("android.view.SurfaceControl"))
                .invoke(t, sc)
            t.javaClass.getDeclaredMethod("apply").invoke(t)
            Class.forName("android.view.SurfaceControl").getDeclaredMethod("release")
                .invoke(sc)
        } catch (_: Exception) { }
    }

    private fun isLiveWallpaper(ctx: Context): Boolean = try { android.app.WallpaperManager.getInstance(ctx).wallpaperInfo != null } catch (_: Exception) { false }

    // ================================================================
    //   TextureView 覆盖模糊 — 帧捕获 + RenderEffect (方案 B 实现)
    //
    //   优化点 (不覆写 SurfaceTexture.OnFrameAvailableListener, 避免破坏视频播放器):
    //   1. Choreographer.FrameCallback 持续循环捕获, 30fps 上限
    //   2. Bitmap 复用池, 尺寸不变时 Canvas 覆盖写入, 减少 GC
    //   3. Rect 预分配, 避免 per-frame new
    //   4. 脏标记跳过无变化时 RenderNode 重建
    //   5. alpha/visibility/size=0 时暂停捕获 (stopChoreographer)
    // ================================================================

    private fun tryActivateTextureOverlay(tv: TextureView, radius: Int): Boolean {
        if (tv.width <= 0 || tv.height <= 0) {
            logW { "tvCapture: TextureView size=0" }; return false
        }
        val d = TextureCaptureBlurDrawable(tv, radius.toFloat(), blurScaleFactor)
        d.attach(this)
        textureCaptureDrawable = d
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setBackgroundInternal(d)
        installTracking()
        logD { "TEXTURE_CAPTURE OK: radius=$radius scale=${blurScaleFactor}" }
        return true
    }

    private fun deactivateTextureOverlay() {
        uninstallTracking()
        textureCaptureDrawable?.detach()
        textureCaptureDrawable = null
        setBackgroundInternal(null)
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private fun updateTextureOverlayPosition() {
        textureCaptureDrawable?.requestRefresh()
    }

    private fun updateTextureOverlayRadius() {
        textureCaptureDrawable?.setBlurRadius(blurRadius.toFloat())
    }

    // ================================================================
    //   TextureCaptureBlurDrawable — 帧捕获 + GPU 模糊 (优化版)
    // ================================================================
    @SuppressLint("NewApi")
    private inner class TextureCaptureBlurDrawable(
        private val textureView: TextureView,
        private var blurRadius: Float,
        private var scaleFactor: Float,
    ) : Drawable() {

        // ── 位图缓冲 (复用，减少 GC) ──
        private var scaledBitmap: Bitmap? = null
        private var scaledW = 0; private var scaledH = 0

        // ── GPU 模糊 ──
        private var blurNode: RenderNode? = null
        private var renderEffect: RenderEffect? = null
        private var cachedRadius = -1f

        // ── 脏标记 (避免无变化时重建 RenderNode) ──
        private var dirtyNode = true
        private var lastSx = -1; private var lastSy = -1
        private var lastCropW = -1; private var lastCropH = -1
        private var nodeW = 0; private var nodeH = 0

        // ── 状态 ──
        private var isCapturing = false
        var isAttached = false; private set
        private val choreographer = android.view.Choreographer.getInstance()
        private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        // ── 预分配对象 ──
        private val srcRect = Rect()
        private val dstRect = Rect()

        // ── 帧节流 ──
        private var lastFrameTime = 0L
        private val minFrameIntervalMs = 33L // ~30fps 上限

        // ═══════════════════════════════════════════
        //  Lifecycle
        // ═══════════════════════════════════════════

        fun attach(view: View) {
            isAttached = true
            nodeW = view.width.coerceAtLeast(1); nodeH = view.height.coerceAtLeast(1)
            scheduleChoreographer()
            requestRefresh()
        }

        fun detach() {
            isAttached = false
            stopChoreographer()
            scope.cancel()
            releaseScaledBitmap()
            blurNode?.discardDisplayList(); blurNode = null
            renderEffect = null
        }

        // ═══════════════════════════════════════════
        //  Public
        // ═══════════════════════════════════════════

        fun setBlurRadius(r: Float) {
            val nr = r.coerceAtLeast(0.1f)
            if (blurRadius == nr) return
            blurRadius = nr
            renderEffect = null; cachedRadius = -1f
            dirtyNode = true
            requestRefresh()
        }

        fun requestRefresh() {
            if (!isAttached) return
            if (!shouldCapture()) return
            invalidateSelf()
            invalidateBlurNodeBounds()
            triggerCapture()
        }

        // ═══════════════════════════════════════════
        //  Choreographer 节流 (~30fps 上限)
        // ═══════════════════════════════════════════

        private val frameCb = android.view.Choreographer.FrameCallback { frameTimeNanos ->
            if (!isAttached) return@FrameCallback
            val nowMs = frameTimeNanos / 1_000_000L
            if (nowMs - lastFrameTime < minFrameIntervalMs) {
                // 距上一帧太近，继续等下一 vsync
                scheduleChoreographer()
                return@FrameCallback
            }
            lastFrameTime = nowMs
            if (shouldCapture()) captureFrame()
            scheduleChoreographer() // 持续循环
        }

        private fun scheduleChoreographer() {
            stopChoreographer()
            choreographer.postFrameCallback(frameCb)
        }

        private fun stopChoreographer() {
            choreographer.removeFrameCallback(frameCb)
        }

        private fun triggerCapture() {
            // 直接触发(跳过 vsync 等待); 在 choreographer 循环中也足够快
            if (shouldCapture() && !isCapturing) {
                val nowMs = System.nanoTime() / 1_000_000L
                if (nowMs - lastFrameTime >= minFrameIntervalMs) {
                    lastFrameTime = nowMs
                    captureFrame()
                }
            }
        }

        // ═══════════════════════════════════════════
        //  捕获 & 模糊
        // ═══════════════════════════════════════════

        private fun shouldCapture(): Boolean {
            if (!isAttached) return false
            val v = this@FrostedAnimatedGlowView
            if (v.visibility != View.VISIBLE || v.alpha <= 0f || v.width <= 0 || v.height <= 0) return false
            val tv = textureView
            return tv.width > 0 && tv.height > 0
        }

        private fun captureFrame() {
            if (isCapturing || !isAttached) return
            val tv = textureView
            if (tv.width <= 0 || tv.height <= 0) return
            isCapturing = true
            scope.launch(Dispatchers.Default) {
                try {
                    // 先从共享缓存获取（同 TextureView 的多 View 复用一次 bitmap 捕获）
                    var bitmap = getCachedTextureFrame(tv)
                    val fromCache = bitmap != null
                    if (!fromCache) {
                        bitmap = tv.bitmap ?: return@launch
                        cacheTextureFrame(tv, bitmap)
                    }
                    val targetW = (bitmap.width * scaleFactor).toInt().coerceAtLeast(1)
                    val targetH = (bitmap.height * scaleFactor).toInt().coerceAtLeast(1)

                    // Bitmap 复用: 尺寸不变时用 in-place 缩放
                    val scaled: Bitmap
                    if (scaledBitmap != null && scaledW == targetW && scaledH == targetH
                        && scaledBitmap!!.isMutable && !scaledBitmap!!.isRecycled) {
                        // 复用已有 buffer，用 Canvas 画缩放后的内容
                        val reuseCanvas = Canvas(scaledBitmap!!)
                        reuseCanvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR)
                        srcRect.set(0, 0, bitmap.width, bitmap.height)
                        dstRect.set(0, 0, targetW, targetH)
                        reuseCanvas.drawBitmap(bitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                        scaled = scaledBitmap!!
                    } else {
                        releaseScaledBitmap()
                        scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, false)
                        scaledBitmap = scaled; scaledW = targetW; scaledH = targetH
                        dirtyNode = true // 新 bitmap，RenderNode 必须重建
                    }
                    // 回收原始帧 (仅当 != scaled 且非缓存)
                    if (bitmap !== scaled) bitmap.recycle()

                    withContext(Dispatchers.Main) {
                        if (!isAttached) { scaled.recycle(); return@withContext }
                        // swapped: old buffer 已在上面被复用或释放，capturedBitmap 现在 = scaled
                        if (scaled !== scaledBitmap) {
                            releaseScaledBitmap()
                            scaledBitmap = scaled
                        }
                        buildBlurNode()
                        invalidateSelf()
                    }
                } catch (_: Exception) {
                } finally {
                    isCapturing = false
                }
            }
        }

        // ═══════════════════════════════════════════
        //  RenderNode 重建
        // ═══════════════════════════════════════════

        private fun invalidateBlurNodeBounds() {
            // View 尺寸变化 → 重建 RenderNode
            val v = this@FrostedAnimatedGlowView
            val nw = v.width.coerceAtLeast(1); val nh = v.height.coerceAtLeast(1)
            if (nodeW != nw || nodeH != nh) {
                nodeW = nw; nodeH = nh
                blurNode?.discardDisplayList(); blurNode = null
                dirtyNode = true
            }
        }

        private fun buildBlurNode() {
            val bmp = scaledBitmap ?: return
            if (bmp.isRecycled) return

            // RenderEffect 缓存
            if (renderEffect == null || cachedRadius != blurRadius) {
                renderEffect = getOrCreateBlurEffect(blurRadius)
                cachedRadius = blurRadius
                dirtyNode = true
            }

            // RenderNode 尺寸变化
            invalidateBlurNodeBounds()

            // 计算裁剪区域
            getLocationOnScreen(screenPosCache)
            val sx = (screenPosCache[0] * scaleFactor).toInt().coerceIn(0, bmp.width - 1)
            val sy = (screenPosCache[1] * scaleFactor).toInt().coerceIn(0, bmp.height - 1)
            val cw = (nodeW * scaleFactor).toInt().coerceAtLeast(1).coerceAtMost(bmp.width - sx)
            val ch = (nodeH * scaleFactor).toInt().coerceAtLeast(1).coerceAtMost(bmp.height - sy)

            // 裁剪区域未变化且节点无需重建 → 跳过
            if (!dirtyNode && lastSx == sx && lastSy == sy && lastCropW == cw && lastCropH == ch) return

            // 记录本次参数
            lastSx = sx; lastSy = sy; lastCropW = cw; lastCropH = ch
            dirtyNode = false

            if (blurNode == null) {
                blurNode = RenderNode("TVCaptureBlur")
                blurNode!!.setPosition(0, 0, nodeW, nodeH)
            }
            blurNode!!.setRenderEffect(renderEffect)

            // 绘制: 从全帧中裁剪 BlurView 对应区域
            srcRect.set(sx, sy, sx + cw, sy + ch)
            dstRect.set(0, 0, nodeW, nodeH)
            val rc = blurNode!!.beginRecording()
            rc.drawBitmap(bmp, srcRect, dstRect, null)
            blurNode!!.endRecording()
        }

        // ═══════════════════════════════════════════
        //  绘制
        // ═══════════════════════════════════════════

        override fun draw(canvas: Canvas) {
            if (!isAttached) return
            val node = blurNode ?: return
            if (!node.hasDisplayList()) return
            canvas.drawRenderNode(node)
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(cf: ColorFilter?) {}
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        // ═══════════════════════════════════════════
        //  辅助
        // ═══════════════════════════════════════════

        private fun releaseScaledBitmap() {
            scaledBitmap?.let { if (!it.isRecycled) it.recycle() }
            scaledBitmap = null; scaledW = 0; scaledH = 0
        }
    }

    // ================================================================
    //               SourceBitmapBlurDrawable (BITMAP 路径)
    // ================================================================
    @SuppressLint("NewApi")
    private inner class SourceBitmapBlurDrawable(
        private var source: Bitmap, private var scale: Float, private var radius: Float,
    ) : Drawable() {
        private var scaledBmp: Bitmap? = null; private var blurNode: RenderNode? = null; private var nodeW = 0; private var nodeH = 0
        private val cropRect = Rect(); private val dstRect = Rect(); private var attached = false
        private var renderEffect: RenderEffect? = null; private var cachedRadius = -1f

        fun attach(view: View) { attached = true; buildScaledBitmap(); updateRegion(); buildBlurNode() }
        fun detach() { attached = false; scaledBmp?.recycle(); scaledBmp = null; blurNode?.discardDisplayList(); blurNode = null; renderEffect = null }
        fun setBlurRadius(r: Float) { if (radius == r) return; radius = r.coerceAtLeast(0f); renderEffect = null; blurNode?.setRenderEffect(getOrCreateEffect()); invalidateSelf() }
        fun invalidateCache() { scaledBmp?.recycle(); scaledBmp = null; buildScaledBitmap(); updateRegion(); buildBlurNode(); invalidateSelf() }

        fun updateRegion() {
            if (!attached || width <= 0 || height <= 0) return
            val sc = scaledBmp; if (sc == null || sc.isRecycled) { buildScaledBitmap(); if (scaledBmp == null) return }; val bmp = scaledBmp ?: return
            getLocationOnScreen(screenPosCache)
            val sx = (screenPosCache[0] * scale).toInt().coerceIn(0, bmp.width - 1); val sy = (screenPosCache[1] * scale).toInt().coerceIn(0, bmp.height - 1)
            val sw = ((width * scale).toInt()).coerceAtLeast(1).coerceAtMost(bmp.width - sx); val sh = ((height * scale).toInt()).coerceAtLeast(1).coerceAtMost(bmp.height - sy)
            val useFull = sw <= 2 || sh <= 2 || (source.width * scale < width * scale && source.height * scale < height * scale)
            val (csx, csy, csw, csh) = if (useFull) arrayOf(0, 0, bmp.width, bmp.height) else arrayOf(sx, sy, sw, sh)
            if (cropRect.left != csx || cropRect.top != csy) { cropRect.set(csx, csy, csx + csw, csy + csh); buildBlurNode() }
        }

        override fun draw(canvas: Canvas) {
            if (!attached) return; if (scaledBmp == null || scaledBmp!!.isRecycled) { buildScaledBitmap(); updateRegion(); buildBlurNode() }
            val node = blurNode ?: return; if (cropRect.isEmpty) return; if (!node.hasDisplayList()) { buildBlurNode(); if (!node.hasDisplayList()) return }; canvas.drawRenderNode(node)
        }
        override fun setAlpha(a: Int) {}; override fun setColorFilter(cf: ColorFilter?) {}
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        private fun getOrCreateEffect() = if (renderEffect == null && radius > 0f) { renderEffect = getOrCreateBlurEffect(radius); cachedRadius = radius; renderEffect } else renderEffect
        private fun buildScaledBitmap() { val s = scale.coerceIn(0.01f, 1.0f); scaledBmp = Bitmap.createScaledBitmap(source, (source.width * s).toInt().coerceAtLeast(1), (source.height * s).toInt().coerceAtLeast(1), false) }
        private fun buildBlurNode() {
            val vw = width; val vh = height; if (vw <= 0 || vh <= 0 || cropRect.isEmpty) return
            if (blurNode == null || nodeW != vw || nodeH != vh) { blurNode?.discardDisplayList(); blurNode = RenderNode("SrcBmpBlur").apply { setPosition(0, 0, vw, vh) }; nodeW = vw; nodeH = vh }
            blurNode!!.setRenderEffect(getOrCreateEffect()); val sc = scaledBmp ?: return
            val rc = blurNode!!.beginRecording(); rc.drawBitmap(sc, cropRect, Rect(0, 0, vw, vh), null); blurNode!!.endRecording(); invalidateSelf()
        }
    }

    // ================================================================
    //               WallpaperCropDrawable (WALLPAPER 路径)
    // ================================================================
    private inner class WallpaperCropDrawable(private val bitmap: Bitmap, private val blurScaleFactor: Float) : Drawable() {
        private val srcRect = Rect(); private val dstRect = Rect(); private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        fun setCropRegion(screenX: Int, screenY: Int, viewW: Int, viewH: Int) {
            val s = blurScaleFactor.coerceIn(0.01f, 1.0f)
            val sx = (screenX * s).toInt().coerceIn(0, bitmap.width - 1); val sy = (screenY * s).toInt().coerceIn(0, bitmap.height - 1)
            val sw = (viewW * s).toInt().coerceAtLeast(1).coerceAtMost(bitmap.width - sx); val sh = (viewH * s).toInt().coerceAtLeast(1).coerceAtMost(bitmap.height - sy)
            if (srcRect.left != sx || srcRect.top != sy) { srcRect.set(sx, sy, sx + sw, sy + sh); dstRect.set(0, 0, viewW, viewH); invalidateSelf() }
        }
        override fun draw(canvas: Canvas) { if (!bitmap.isRecycled && !srcRect.isEmpty) canvas.drawBitmap(bitmap, srcRect, dstRect, paint) }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }; override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    init {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val r = radii[0]; if (r > 0f && view.width > 0 && view.height > 0) outline.setRoundRect(0, 0, view.width, view.height, r) else outline.setRect(0, 0, view.width, view.height)
            }
        }
        attrs?.let { context.withStyledAttributes(it, R.styleable.FrostedAnimatedGlowView) {
            blurEnabled = getBoolean(R.styleable.FrostedAnimatedGlowView_fagv_blurEnabled, false)
            blurRadius = getInt(R.styleable.FrostedAnimatedGlowView_fagv_blurRadius, MAX_RADIUS)
            blurredDrawable = getDrawable(R.styleable.FrostedAnimatedGlowView_fagv_blurredDrawable);
            backgroundBlurOnly = getBoolean(R.styleable.FrostedAnimatedGlowView_fagv_backgroundBlurOnly, false)
        } }
    }
}
