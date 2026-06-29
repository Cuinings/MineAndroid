package com.cn.core.ui.view.frosted

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withClip
import androidx.core.graphics.withSave
import com.cn.core.ui.R

/**
 * 毛玻璃发光视图 — Compositor Blur + Bitmap Fallback 双后端。
 *
 * ## 主路径：Compositor Blur (Android 12+, GPU 零开销)
 * ```
 * blurBehindRadius + FLAG_BLUR_BEHIND → SurfaceFlinger GPU 模糊壁纸
 * WallpaperMaskDrawable → decorView 背景
 *   ├── 清晰壁纸覆盖全窗口
 *   └── clipPath 镂空 View 区域 → 透出模糊壁纸
 * ```
 *
 * ## 降级：Bitmap + RenderEffect GPU 模糊
 * ```
 * WallpaperManager.getDrawable().toBitmap()
 *   → RenderNode 全屏录制 + RenderEffect.createBlurEffect()
 *   → 移动时 canvas.translate 裁剪（零重建、零延时）
 * ```
 *
 * @author cn
 * @time 2026/6/25
 */
@SuppressLint("Recycle", "ClickableViewAccessibility", "NewApi")
open class FrostedAnimatedGlowView1 @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : AnimatedStatefulGlowView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "FrostedGlow"
        private val masks = java.util.WeakHashMap<Activity, MaskEntry>()
        private val maskLock = Any()
        class MaskEntry(val drawable: Drawable, var refCount: Int = 0, var originalBg: Drawable? = null)
    }

    var blurEnabled: Boolean = false
        set(v) { if (v == field) return; field = v; if (v && isAttachedToWindow) activateBlur() else deactivateBlur() }

    var blurRadius: Float = 25f
        set(v) { field = v.coerceIn(1f, 25f); if (blurEnabled) onRadiusChanged() }

    /** 当前使用的模糊路径 */
    enum class BlurMode { NONE, COMPOSITOR, BITMAP }
    var blurMode: BlurMode = BlurMode.NONE
        private set

    // ── Compositor 路径状态 ──
    private var maskRegistered = false
    private var trackingInstalled = false

    // ── Bitmap 降级路径状态 ──
    private var mBlurNode: RenderNode? = null
    private var mBlurNodeDirty = true
    private var mWallpaperBitmap: Bitmap? = null
    private var mScreenW = 0; private var mScreenH = 0

    // ==================== 位置 ====================
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) { super.onLayout(changed, l, t, r, b); onPositionChanged() }
    override fun setTranslationX(tx: Float) { super.setTranslationX(tx); onPositionChanged() }
    override fun setTranslationY(ty: Float) { super.setTranslationY(ty); onPositionChanged() }
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { super.onSizeChanged(w, h, ow, oh); onPositionChanged() }
    private fun onPositionChanged() {
        if (blurMode == BlurMode.COMPOSITOR) updateMaskHole()
        else invalidate()
    }

    // ==================== 绘制 ====================
    override fun dispatchDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        buildDispatchPath(w, h)
        canvas.withClip(dispatchClipPath) {
            // 1. 模糊背景
            if (blurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (blurMode) {
                    BlurMode.COMPOSITOR -> { /* SurfaceFlinger 处理 */ }
                    BlurMode.BITMAP -> drawBitmapBlur(this)
                    BlurMode.NONE -> {}
                }
            }
            // 2. 装饰层
            drawOverlay(this, w, h); drawInnerGlow(this, w, h)
            drawStroke(this, w, h); drawTopHighlight(this, w, h)
            if (flowEnabled && isFocused && flowAlpha >= 0.01f) drawFlowEffect(this)
            if (refreshEffectEnabled && isFocused) drawRefreshEffect(this, w, h)
        }
        super.dispatchDraw(canvas)
    }

    // ==================== Compositor Blur ====================
    private fun activateCompositor(act: Activity) {
        val radiusPx = blurRadius.toInt().coerceIn(0, 25)
        try {
            val p = act.window.attributes
            p.blurBehindRadius = radiusPx
            p.flags = p.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            act.window.attributes = p
        } catch (e: Exception) { Log.w(TAG, "Window blur: ${e.message}") }

        synchronized(maskLock) {
            val entry = masks[act]
            if (entry != null) { entry.refCount++ } else {
                val mask = WallpaperMaskDrawable(act)
                val decor = act.window.decorView
                masks[act] = MaskEntry(mask, 1, decor.background)
                decor.background = mask
            }
        }
        maskRegistered = true; updateMaskHole()
        installTracking()
        setLayerType(LAYER_TYPE_NONE, null)
        blurMode = BlurMode.COMPOSITOR
        Log.d(TAG, "Blur: COMPOSITOR (radius=$radiusPx)")
    }

    private fun updateMaskHole() {
        if (!maskRegistered || width <= 0 || height <= 0 || visibility != VISIBLE) return
        val act = resolveActivity() ?: return
        val dLoc = IntArray(2); act.window.decorView.getLocationOnScreen(dLoc)
        val pos = IntArray(2); getLocationOnScreen(pos)
        val hole = RectF((pos[0] - dLoc[0]).toFloat(), (pos[1] - dLoc[1]).toFloat(),
            (pos[0] - dLoc[0] + width).toFloat(), (pos[1] - dLoc[1] + height).toFloat())
        synchronized(maskLock) {
            (masks[act]?.drawable as? WallpaperMaskDrawable)?.updateHole(this, hole, radii)
        }
        act.window.decorView.invalidate()
    }

    // ==================== Bitmap Fallback ====================
    private fun activateBitmap() {
        val dm = context.resources.displayMetrics
        mScreenW = dm.widthPixels; mScreenH = dm.heightPixels
        mBlurNodeDirty = true
        ensureBitmap()
        blurMode = BlurMode.BITMAP
        Log.d(TAG, "Blur: BITMAP fallback")
    }

    private fun ensureBitmap() {
        if (mWallpaperBitmap != null && !mWallpaperBitmap!!.isRecycled) return
        mWallpaperBitmap = try { WallpaperManager.getInstance(context).drawable?.toBitmap(mScreenW, mScreenH, Bitmap.Config.ARGB_8888) } catch (_: Exception) { null }
        mBlurNodeDirty = true
        Log.d(TAG, "Wallpaper: ${mWallpaperBitmap?.width}x${mWallpaperBitmap?.height}")
    }

    private fun drawBitmapBlur(canvas: Canvas) {
        val bmp = mWallpaperBitmap ?: run { ensureBitmap(); return }
        if (bmp.isRecycled) { mWallpaperBitmap = null; mBlurNodeDirty = true; return }
        if (width <= 0 || height <= 0) return

        if (mBlurNodeDirty || mBlurNode == null || !mBlurNode!!.hasDisplayList()) {
            mBlurNode?.discardDisplayList()
            mBlurNode = RenderNode("blur-bmp").apply {
                setPosition(0, 0, mScreenW, mScreenH)
                setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP))
            }
            val rc = mBlurNode!!.beginRecording(); rc.drawBitmap(bmp, 0f, 0f, null); mBlurNode!!.endRecording()
            mBlurNodeDirty = false
        }
        val pos = IntArray(2); getLocationOnScreen(pos)
        canvas.withSave { canvas.translate(-pos[0].toFloat(), -pos[1].toFloat()); canvas.drawRenderNode(mBlurNode!!) }
    }

    // ==================== 激活/停用 ====================
    private fun activateBlur() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) { Log.w(TAG, "API < 31"); return }
        val act = resolveActivity() ?: run { Log.w(TAG, "no Activity"); return }
        val wm = act.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (wm.isCrossWindowBlurEnabled) { activateCompositor(act); return }
        Log.w(TAG, "crossWindowBlur disabled OEM → bitmap fallback")
        activateBitmap()
    }

    private fun deactivateBlur() {
        when (blurMode) {
            BlurMode.COMPOSITOR -> {
                maskRegistered = false; uninstallTracking()
                val act = resolveActivity()
                if (act != null) synchronized(maskLock) {
                    val e = masks[act] ?: return@synchronized
                    e.refCount--
                    if (e.refCount <= 0) {
                        act.window.decorView.background = e.originalBg; masks.remove(act)
                        try { act.window.attributes = act.window.attributes.apply { blurBehindRadius = 0; flags = flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv() } } catch (_: Exception) { }
                    }
                }
            }
            BlurMode.BITMAP -> {
                mWallpaperBitmap?.recycle(); mWallpaperBitmap = null
                mBlurNode?.discardDisplayList(); mBlurNode = null; mBlurNodeDirty = true
            }
            BlurMode.NONE -> {}
        }
        blurMode = BlurMode.NONE
    }

    private fun onRadiusChanged() {
        when (blurMode) {
            BlurMode.COMPOSITOR -> { val act = resolveActivity(); if (act != null) try { act.window.attributes = act.window.attributes.apply { blurBehindRadius = blurRadius.toInt() } } catch (_: Exception) { } }
            BlurMode.BITMAP -> { mBlurNodeDirty = true; invalidate() }
            BlurMode.NONE -> {}
        }
    }

    // ==================== 工具 ====================
    private fun resolveActivity(): Activity? { var c: Context = context; while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }; return null }

    private val scrollCb = ViewTreeObserver.OnScrollChangedListener { if (blurMode == BlurMode.COMPOSITOR) updateMaskHole() }
    private val layoutCb = ViewTreeObserver.OnGlobalLayoutListener { if (blurMode == BlurMode.COMPOSITOR) updateMaskHole() }
    private fun installTracking() { if (!trackingInstalled) { val vto = viewTreeObserver; if (vto.isAlive) { vto.addOnScrollChangedListener(scrollCb); vto.addOnGlobalLayoutListener(layoutCb); trackingInstalled = true } } }
    private fun uninstallTracking() { if (trackingInstalled) { val vto = viewTreeObserver; if (vto.isAlive) { vto.removeOnScrollChangedListener(scrollCb); vto.removeOnGlobalLayoutListener(layoutCb) }; trackingInstalled = false } }

    // ==================== 蒙版 Drawable ====================
    class WallpaperMaskDrawable(private val context: Context) : Drawable() {
        private val holes = java.util.WeakHashMap<View, HoleInfo>()
        private val path = Path()
        private var wp: Drawable? = null
        data class HoleInfo(val rect: RectF, val radii: FloatArray)
        fun updateHole(view: View, rect: RectF, radii: FloatArray) { holes[view] = HoleInfo(rect, radii); invalidateSelf() }
        @SuppressLint("MissingPermission")
        override fun draw(canvas: Canvas) {
            if (wp == null) wp = try { WallpaperManager.getInstance(context).drawable?.mutate() } catch (_: Exception) { null }
            val w = wp ?: return
            if (w.bounds.isEmpty) w.setBounds(0, 0, canvas.width, canvas.height)
            if (holes.isEmpty()) { w.draw(canvas); return }
            path.reset(); path.fillType = Path.FillType.EVEN_ODD
            path.addRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), Path.Direction.CW)
            for ((_, h) in holes) path.addRoundRect(h.rect, h.radii, Path.Direction.CW)
            canvas.withClip(path) { w.draw(this) }
        }
        override fun setAlpha(a: Int) { wp?.alpha = a }
        override fun setColorFilter(cf: ColorFilter?) { wp?.colorFilter = cf }
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); if (blurEnabled) activateBlur() }
    override fun onDetachedFromWindow() { deactivateBlur(); super.onDetachedFromWindow() }

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.FrostedAnimatedGlowView) {
                blurEnabled = getBoolean(R.styleable.FrostedAnimatedGlowView_fagv_useBackgroundBlurMode, false)
                blurRadius = getInt(R.styleable.FrostedAnimatedGlowView_fagv_backgroundBlurRadius, 25).toFloat()
            }
        }
    }
}
