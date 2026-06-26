package com.cn.core.ui.view.frosted

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
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
class WallpaperBlurView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : AnimatedStatefulGlowView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "WallpaperBlur"
        private const val DEFAULT_BLUR_RADIUS = 25f
    }

    enum class WallpaperMode { AUTO, STATIC, DYNAMIC }
    var wallpaperMode: WallpaperMode = WallpaperMode.AUTO
        set(v) { if (v != field) { field = v; restartBlurIfNeeded() } }

    var blurEnabled: Boolean = true
        set(v) { if (v != field) { field = v; restartBlurIfNeeded() } }

    var blurRadius: Float = DEFAULT_BLUR_RADIUS
        set(v) { field = v.coerceIn(0f, 25f); if (compositorBlurActive) BlurMaskManager.setWindowBlur(resolveActivity()!!, v.toInt()) }

    private var compositorBlurActive = false
    private var maskRegistered = false
    private var useWallpaperBitmapBlur = false
    private val screenPosArray = IntArray(2)

    private var wallpaperBlurNode: RenderNode? = null
    private var wallpaperBlurNodeW = 0; private var wallpaperBlurNodeH = 0
    private var wallpaperBitmapRaw: Bitmap? = null
    private var wallpaperBlurDirty = true
    private val wallpaperSrcRect = Rect(); private val wallpaperDstRect = Rect()
    private var wallpaperBlurRadius = -1f

    private var fullScreenCapture: Bitmap? = null
    private val captureRefreshInterval = 2000L
    private var fullScreenCaptureTime = 0L
    private var isCapturingScreen = false
    private var screenCapturePending = false
    private val handler = Handler(Looper.getMainLooper())

    private val scrollCb = ViewTreeObserver.OnScrollChangedListener { onPositionChanged() }
    private val layoutCb = ViewTreeObserver.OnGlobalLayoutListener { onPositionChanged() }
    private var trackingInstalled = false

    private val wpColorListener = WallpaperManager.OnColorsChangedListener { _, _ -> onWallpaperChanged() }
    private var wpListenerInstalled = false

    // ==================== 激活 ====================
    private fun activateCompositorBlur() {
        val act = resolveActivity() ?: run { activateWallpaperBitmapBlur(); return }
        if (!BlurMaskManager.isCrossWindowBlurEnabled(act)) { activateWallpaperBitmapBlur(); return }
        val rp = blurRadius.toInt().coerceIn(0, 25)
        if (BlurMaskManager.isLiveWallpaper(context)) { activateLiveWallpaperCapture(act); return }
        BlurMaskManager.setWindowBlur(act, rp)
        BlurMaskManager.register(act, rp)
        maskRegistered = true; compositorBlurActive = true; useWallpaperBitmapBlur = false
        setLayerType(LAYER_TYPE_NONE, null)
        installTracking(); installWpListener()
        post { updateMaskHole() }
        // 延迟重试：WallpaperManager.getDrawable() 可能在首帧返回 null，100ms 后重试
        postDelayed({ updateMaskHole() }, 100)
        Log.d(TAG, "Compositor blur activated, radius=$rp")
    }

    private fun activateWallpaperBitmapBlur() {
        useWallpaperBitmapBlur = true; compositorBlurActive = false
        setLayerType(LAYER_TYPE_HARDWARE, null)
        installTracking(); installWpListener()
        wallpaperBlurDirty = true; invalidate()
    }

    private fun deactivateBlur() {
        if (maskRegistered) { val act = resolveActivity(); if (act != null) { BlurMaskManager.removeHole(this, act); BlurMaskManager.unregister(act) } }
        compositorBlurActive = false; maskRegistered = false; useWallpaperBitmapBlur = false
        uninstallTracking(); uninstallWpListener()
        wallpaperBitmapRaw?.recycle(); wallpaperBitmapRaw = null
        wallpaperBlurNode?.discardDisplayList(); wallpaperBlurNode = null
        fullScreenCapture?.recycle(); fullScreenCapture = null
    }

    private fun restartBlurIfNeeded() {
        if (!blurEnabled || !isAttachedToWindow) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { deactivateBlur(); activateCompositorBlur() }
    }

    // ==================== 动态壁纸 ====================
    @RequiresApi(Build.VERSION_CODES.S)
    private fun activateLiveWallpaperCapture(activity: Activity) {
        useWallpaperBitmapBlur = true; compositorBlurActive = false
        installTracking(); installWpListener()
        scheduleFullScreenRefresh(activity); wallpaperBlurDirty = true; invalidate()
    }

    private fun scheduleFullScreenRefresh(activity: Activity) {
        val now = System.currentTimeMillis()
        if (now - fullScreenCaptureTime < captureRefreshInterval) return
        fullScreenCaptureTime = now
        if (isCapturingScreen) { screenCapturePending = true; return }
        isCapturingScreen = true
        activity.window.decorView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                activity.window.decorView.viewTreeObserver.removeOnPreDrawListener(this)
                Thread {
                    try {
                        val d = context.resources.displayMetrics
                        val bmp = captureFullScreenDirect(d.widthPixels, d.heightPixels)
                            ?: captureFullScreenReflective(d.widthPixels, d.heightPixels)
                        handler.post {
                            fullScreenCapture?.recycle(); fullScreenCapture = bmp
                            wallpaperBlurDirty = true; invalidate()
                            isCapturingScreen = false
                            if (screenCapturePending) { screenCapturePending = false; scheduleFullScreenRefresh(activity) }
                        }
                    } catch (_: Exception) { handler.post { isCapturingScreen = false } }
                }.start()
                return true
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun captureFullScreenDirect(w: Int, h: Int): Bitmap? = try {
        val cls = Class.forName("android.view.SurfaceControl")
        val m = cls.getDeclaredMethod("screenshot", Rect::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        m.isAccessible = true; m.invoke(null, Rect(0, 0, w, h), w, h, 0) as? Bitmap
    } catch (_: Exception) { null }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun captureFullScreenReflective(w: Int, h: Int): Bitmap? {
        try {
            val cls = Class.forName("android.view.SurfaceControl")
            for (mName in arrayOf("getInternalDisplayToken", "getPrimaryDisplayToken", "getBuiltInDisplay")) {
                try {
                    val gt = cls.getDeclaredMethod(mName, Int::class.javaPrimitiveType); gt.isAccessible = true
                    val tk = gt.invoke(null, 0) as? android.os.IBinder ?: continue
                    for (sig in arrayOf(arrayOf(android.os.IBinder::class.java, Rect::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType), arrayOf(Rect::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType))) {
                        try { val sm = cls.getDeclaredMethod("screenshot", *sig); sm.isAccessible = true; val a = if (sig.size == 5) arrayOf(tk, Rect(0, 0, w, h), w, h, 0) else arrayOf(Rect(0, 0, w, h), w, h, 0); val r = sm.invoke(null, *a) as? Bitmap; if (r != null) return r } catch (_: Exception) { }
                    }
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
        return null
    }

    // ==================== Bitmap 模糊 ====================
    @RequiresApi(Build.VERSION_CODES.S)
    private fun updateWallpaperBlurNode() {
        if (!blurEnabled || width <= 0 || height <= 0) return
        if (!wallpaperBlurDirty && wallpaperBlurNode != null) return
        wallpaperBlurDirty = false
        val vw = width; val vh = height
        if (wallpaperBitmapRaw == null || wallpaperBitmapRaw!!.isRecycled) {
            wallpaperBitmapRaw = if (BlurMaskManager.isLiveWallpaper(context)) fullScreenCapture ?: WallpaperManager.getInstance(context).drawable?.toBitmap()
            else WallpaperManager.getInstance(context).drawable?.toBitmap(vw, vh, Bitmap.Config.ARGB_8888)
        }
        val bmp = wallpaperBitmapRaw ?: return
        if (bmp.isRecycled) { wallpaperBitmapRaw = null; return }
        getLocationOnScreen(screenPosArray)
        val dm = context.resources.displayMetrics
        val sx = (screenPosArray[0].toFloat() / dm.widthPixels * bmp.width).toInt().coerceIn(0, bmp.width)
        val sy = (screenPosArray[1].toFloat() / dm.heightPixels * bmp.height).toInt().coerceIn(0, bmp.height)
        val sw = (vw.toFloat() / dm.widthPixels * bmp.width).toInt().coerceIn(0, bmp.width - sx)
        val sh = (vh.toFloat() / dm.heightPixels * bmp.height).toInt().coerceIn(0, bmp.height - sy)
        if (sw <= 0 || sh <= 0) return
        wallpaperSrcRect.set(sx, sy, sx + sw, sy + sh); wallpaperDstRect.set(0, 0, vw, vh)
        if (wallpaperBlurNode == null || wallpaperBlurNodeW != vw || wallpaperBlurNodeH != vh) {
            wallpaperBlurNode?.discardDisplayList()
            wallpaperBlurNode = RenderNode("WpBlur").apply { setPosition(0, 0, vw, vh) }
            wallpaperBlurNodeW = vw; wallpaperBlurNodeH = vh; wallpaperBlurRadius = -1f
        }
        if (wallpaperBlurRadius != blurRadius) {
            wallpaperBlurNode!!.setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP))
            wallpaperBlurRadius = blurRadius
        }
        val rc = wallpaperBlurNode!!.beginRecording(); rc.drawBitmap(bmp, wallpaperSrcRect, wallpaperDstRect, null); wallpaperBlurNode!!.endRecording()
    }

    // ==================== 蒙版 ====================
    private fun updateMaskHole() {
        if (!maskRegistered || visibility != VISIBLE || width <= 0 || height <= 0) return
        val act = resolveActivity() ?: return
        if (BlurMaskManager.isLiveWallpaper(context)) return
        getLocationOnScreen(screenPosArray)
        val dl = IntArray(2); act.window.decorView.getLocationOnScreen(dl)
        val r = RectF((screenPosArray[0] - dl[0]).toFloat(), (screenPosArray[1] - dl[1]).toFloat(), (screenPosArray[0] - dl[0] + width).toFloat(), (screenPosArray[1] - dl[1] + height).toFloat())
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
        wallpaperBitmapRaw?.recycle(); wallpaperBitmapRaw = null; wallpaperBlurDirty = true
        if (compositorBlurActive) { val a = resolveActivity(); if (a != null) { BlurMaskManager.updateHole(this, a, RectF(), radii); a.window.decorView.invalidate() } }
        else invalidate()
    }

    // ==================== 绘制 ====================
//    private var radii = FloatArray(8) { 0f }
//    fun setCornerRadius(radius: Float) { radii.fill(radius); invalidate() }
//    fun setCornerRadius(tl: Float, tr: Float, br: Float, bl: Float) { radii[0] = tl; radii[1] = tl; radii[2] = tr; radii[3] = tr; radii[4] = br; radii[5] = br; radii[6] = bl; radii[7] = bl; invalidate() }

    override fun dispatchDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        if (!compositorBlurActive && useWallpaperBitmapBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updateWallpaperBlurNode()
            if (BlurMaskManager.isLiveWallpaper(context)) { val act = resolveActivity(); if (act != null) scheduleFullScreenRefresh(act) }
        }
        if (blurEnabled && !isCapturingScreen && !compositorBlurActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && wallpaperBlurNode != null) {
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
//                setCornerRadius(getDimension(R.styleable.WallpaperBlurView_wbv_cornerRadius, 0f))
//                setCornerRadius(getDimension(R.styleable.WallpaperBlurView_wbv_cornerRadiusTL, 0f), getDimension(R.styleable.WallpaperBlurView_wbv_cornerRadiusTR, 0f), getDimension(R.styleable.WallpaperBlurView_wbv_cornerRadiusBR, 0f), getDimension(R.styleable.WallpaperBlurView_wbv_cornerRadiusBL, 0f))
            }
        }
    }
}
