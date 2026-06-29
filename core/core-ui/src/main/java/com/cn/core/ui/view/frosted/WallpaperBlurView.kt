package com.cn.core.ui.view.frosted

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.toBitmap
import com.cn.core.ui.R

@SuppressLint("Recycle", "ClickableViewAccessibility", "NewApi", "MissingPermission")
open class WallpaperBlurView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : AnimatedStatefulGlowView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "WallpaperBlur"
        private const val DEFAULT_BLUR_RADIUS = 25f
    }

    var blurEnabled: Boolean = true
        set(v) { if (v != field) { field = v; restartBlurIfNeeded() } }

    var blurRadius: Float = DEFAULT_BLUR_RADIUS
        set(v) {
            field = v.coerceIn(0f, 25f)
            if (compositorBlurActive) resolveActivity()?.let { BlurMaskManager.setWindowBlur(it, v.toInt()) }
        }

    private var compositorBlurActive = false
    private var maskRegistered = false
    private var useWallpaperBitmapBlur = false
    private val screenPosArray = IntArray(2)

    // bitmap 模糊
    private var wallpaperBlurNode: RenderNode? = null
    private var wallpaperBlurNodeW = 0; private var wallpaperBlurNodeH = 0
    private var wallpaperBitmapRaw: Bitmap? = null
    private var wallpaperBlurDirty = true
    private var wallpaperBlurFailCount = 0
    private var wallpaperBlurRadius = -1f

    private val scrollCb = ViewTreeObserver.OnScrollChangedListener { onPositionChanged() }
    private val layoutCb = ViewTreeObserver.OnGlobalLayoutListener { onPositionChanged() }
    private var trackingInstalled = false

    private val wpColorListener = WallpaperManager.OnColorsChangedListener { _, _ -> onWallpaperChanged() }
    private var wpListenerInstalled = false
    private val handler = Handler(Looper.getMainLooper())

    // ==================== 激活 ====================
    private fun activateCompositorBlur() {
        val act = resolveActivity() ?: run { activateWallpaperBitmapBlur(); return }
        if (!BlurMaskManager.isCrossWindowBlurEnabled(act)) { activateWallpaperBitmapBlur(); return }
        val rp = blurRadius.toInt().coerceIn(0, 25)
        if (BlurMaskManager.isLiveWallpaper(context)) { activateWallpaperBitmapBlur(); return }

        BlurMaskManager.setWindowBlur(act, rp)
        BlurMaskManager.register(act, rp)
        maskRegistered = true; compositorBlurActive = true; useWallpaperBitmapBlur = false
        setLayerType(LAYER_TYPE_NONE, null)
        installTracking()

        // OnPreDrawListener 确保镂空在蒙版首帧绘制前已注册
        act.window.decorView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                act.window.decorView.viewTreeObserver.removeOnPreDrawListener(this)
                if (isAttachedToWindow && maskRegistered) updateMaskHole()
                return true
            }
        })
        // 延迟装 listener，避免 WallpaperManager 回调在镂空前触发蒙版重绘
        post { installWpListener() }
    }

    private fun activateWallpaperBitmapBlur() {
        useWallpaperBitmapBlur = true; compositorBlurActive = false; maskRegistered = false
        setLayerType(LAYER_TYPE_HARDWARE, null)
        installTracking(); installWpListener()
        wallpaperBlurDirty = true; invalidate()
    }

    private fun deactivateBlur() {
        if (maskRegistered) { val a = resolveActivity(); if (a != null) { BlurMaskManager.removeHole(this, a); BlurMaskManager.unregister(a) } }
        compositorBlurActive = false; maskRegistered = false; useWallpaperBitmapBlur = false
        uninstallTracking(); uninstallWpListener()
        wallpaperBitmapRaw?.recycle(); wallpaperBitmapRaw = null
        wallpaperBlurNode?.discardDisplayList(); wallpaperBlurNode = null
    }

    private fun restartBlurIfNeeded() {
        if (!blurEnabled || !isAttachedToWindow) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (compositorBlurActive) {
            // 已激活，只更新 blur radius
            resolveActivity()?.let { BlurMaskManager.setWindowBlur(it, blurRadius.toInt()) }
        } else {
            deactivateBlur(); activateCompositorBlur()
        }
    }

    // ==================== Bitmap 模糊 ====================
    @RequiresApi(Build.VERSION_CODES.S)
    private fun updateWallpaperBlurNode() {
        if (!blurEnabled || width <= 0 || height <= 0) return
        if (!wallpaperBlurDirty && wallpaperBlurNode != null) return
        wallpaperBlurDirty = false
        val vw = width; val vh = height
        if (wallpaperBitmapRaw == null || wallpaperBitmapRaw!!.isRecycled) {
            wallpaperBitmapRaw = WallpaperManager.getInstance(context).drawable?.toBitmap(vw, vh, Bitmap.Config.ARGB_8888)
        }
        val bmp = wallpaperBitmapRaw
        if (bmp == null || bmp.isRecycled) {
            // 加载失败：指数退避，防止每帧无效重试
            wallpaperBlurFailCount++
            if (wallpaperBlurFailCount > 5) return  // 连续失败5次后停止重试
            wallpaperBlurDirty = true  // 允许下次 dispatchDraw 时重试
            return
        }
        wallpaperBlurFailCount = 0
        // 裁剪 View 对应区域（全屏 bitmap 按比例映射）
        getLocationOnScreen(screenPosArray)
        val dm = context.resources.displayMetrics
        val sx = (screenPosArray[0].toFloat() / dm.widthPixels * bmp.width).toInt().coerceIn(0, bmp.width - 1)
        val sy = (screenPosArray[1].toFloat() / dm.heightPixels * bmp.height).toInt().coerceIn(0, bmp.height - 1)
        val sw = (vw.toFloat() / dm.widthPixels * bmp.width).toInt().coerceAtLeast(1)
        val sh = (vh.toFloat() / dm.heightPixels * bmp.height).toInt().coerceAtLeast(1)
        if (wallpaperBlurNode == null || wallpaperBlurNodeW != vw || wallpaperBlurNodeH != vh) {
            wallpaperBlurNode?.discardDisplayList()
            wallpaperBlurNode = RenderNode("WpBlur").apply { setPosition(0, 0, vw, vh) }
            wallpaperBlurNodeW = vw; wallpaperBlurNodeH = vh; wallpaperBlurRadius = -1f
        }
        if (wallpaperBlurRadius != blurRadius) {
            wallpaperBlurNode!!.setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP))
            wallpaperBlurRadius = blurRadius
        }
        val rc = wallpaperBlurNode!!.beginRecording()
        rc.drawBitmap(bmp, android.graphics.Rect(sx, sy, sx + sw, sy + sh), android.graphics.Rect(0, 0, vw, vh), null)
        wallpaperBlurNode!!.endRecording()
    }

    // ==================== 蒙版 ====================
    private fun updateMaskHole() {
        if (!maskRegistered || visibility != VISIBLE) return
        if (width <= 0 || height <= 0) {
            val vto = viewTreeObserver
            if (vto.isAlive) vto.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    vto.removeOnPreDrawListener(this)
                    if (isAttachedToWindow && maskRegistered && width > 0 && height > 0) doUpdateMaskHole()
                    return true
                }
            })
            return
        }
        doUpdateMaskHole()
    }

    private fun doUpdateMaskHole() {
        val act = resolveActivity() ?: return
        if (BlurMaskManager.isLiveWallpaper(context)) return
        getLocationOnScreen(screenPosArray)
        val dl = IntArray(2); act.window.decorView.getLocationOnScreen(dl)
        val r = RectF(
            (screenPosArray[0] - dl[0]).toFloat(), (screenPosArray[1] - dl[1]).toFloat(),
            (screenPosArray[0] - dl[0] + width).toFloat(), (screenPosArray[1] - dl[1] + height).toFloat()
        )
        BlurMaskManager.updateHole(this, act, r, radii)
        act.window.decorView.invalidate()
    }

    // ==================== 位置 ====================
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { super.onSizeChanged(w, h, ow, oh); onPositionChanged() }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) { super.onLayout(changed, l, t, r, b); if (changed) onPositionChanged() }
    override fun setTranslationX(tx: Float) { super.setTranslationX(tx); onPositionChanged() }
    override fun setTranslationY(ty: Float) { super.setTranslationY(ty); onPositionChanged() }

    private fun onPositionChanged() {
        if (!isAttachedToWindow) return
        if (compositorBlurActive) updateMaskHole()
        else if (useWallpaperBitmapBlur) { wallpaperBlurDirty = true; invalidate() }
    }

    private fun installTracking() { if (!trackingInstalled) { val v = viewTreeObserver; if (v.isAlive) { v.addOnScrollChangedListener(scrollCb); v.addOnGlobalLayoutListener(layoutCb); trackingInstalled = true } } }
    private fun uninstallTracking() { if (trackingInstalled) { val v = viewTreeObserver; if (v.isAlive) { v.removeOnScrollChangedListener(scrollCb); v.removeOnGlobalLayoutListener(layoutCb) }; trackingInstalled = false } }
    private fun installWpListener() { if (!wpListenerInstalled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { try { WallpaperManager.getInstance(context).addOnColorsChangedListener(wpColorListener, handler); wpListenerInstalled = true } catch (_: Exception) { } } }
    private fun uninstallWpListener() { if (wpListenerInstalled) { try { WallpaperManager.getInstance(context).removeOnColorsChangedListener(wpColorListener) } catch (_: Exception) { }; wpListenerInstalled = false } }

    private fun onWallpaperChanged() {
        wallpaperBitmapRaw?.recycle(); wallpaperBitmapRaw = null; wallpaperBlurDirty = true; wallpaperBlurFailCount = 0
        if (compositorBlurActive) {
            val a = resolveActivity() ?: return
            BlurMaskManager.markWallpaperDirty(a)
            a.window.decorView.invalidate()
        } else invalidate()
    }

    // ==================== 绘制 ====================
    override fun dispatchDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        if (!compositorBlurActive && useWallpaperBitmapBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updateWallpaperBlurNode()
        }
        if (blurEnabled && !compositorBlurActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && wallpaperBlurNode != null) {
            canvas.drawRenderNode(wallpaperBlurNode!!)
        }
        super.dispatchDraw(canvas)
    }

    override fun setBackground(background: Drawable?) { }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); if (blurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) activateCompositorBlur() }
    override fun onDetachedFromWindow() { deactivateBlur(); super.onDetachedFromWindow() }
    override fun onWindowVisibilityChanged(v: Int) { super.onWindowVisibilityChanged(v); if (v == VISIBLE) onPositionChanged() }

    private fun resolveActivity(): Activity? { var c: Context = context; while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }; return null }
    fun isCompositorBlurActive(): Boolean = compositorBlurActive

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.WallpaperBlurView) {
                blurEnabled = getBoolean(R.styleable.WallpaperBlurView_wbv_blurEnabled, true)
                blurRadius = getFloat(R.styleable.WallpaperBlurView_wbv_blurRadius, DEFAULT_BLUR_RADIUS)
            }
        }
    }
}
