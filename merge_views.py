"""Merge GlassBlurView + FrostedGlowView -> FrostedGlassView"""
import re

# Read source files
with open('core/core-ui/src/main/java/com/cn/core/ui/view/GlassBlurView.kt', 'r', encoding='utf-8') as f:
    blur_src = f.read()
with open('core/core-ui/src/main/java/com/cn/core/ui/view/frosted/glass/FrostedGlowView.kt', 'r', encoding='utf-8') as f:
    glow_src = f.read()

# Extract imports from both
blur_imports = re.findall(r'^import .+$', blur_src, re.MULTILINE)
glow_imports = re.findall(r'^import .+$', glow_src, re.MULTILINE)
all_imports = sorted(set(blur_imports + glow_imports), key=lambda x: x.strip())

# Build merged file
output = []
output.append('package com.cn.core.ui.view')
output.append('')
for imp in all_imports:
    # Fix package reference
    imp = imp.replace('com.cn.core.ui.view.frosted.glass', 'com.cn.core.ui.view')
    output.append(imp)
output.append('')
output.append('/**')
output.append(' * FrostedGlassView - merged GlassBlurView + FrostedGlowView')
output.append(' * Blur via RenderScript + decorative effects (glow, border, shadow, overlay)')
output.append(' * Set blurEnabled=true via XML or code to enable blur.')
output.append(' */')
output.append('@SuppressLint("Recycle")')
output.append('open class FrostedGlassView @JvmOverloads constructor(')
output.append('    context: Context,')
output.append('    attrs: AttributeSet? = null,')
output.append('    defStyleAttr: Int = 0,')
output.append(') : ConstraintLayout(context, attrs, defStyleAttr) {')
output.append('')

# --- Blur fields from GlassBlurView (skip constructors/companion, skip View-specific) ---
blur_field_lines = []
in_class = False
brace_depth = 0
for line in blur_src.split('\n'):
    stripped = line.strip()
    # Skip until we're past the class declaration
    if 'class GlassBlurView' in stripped:
        in_class = True
        brace_depth = stripped.count('{') - stripped.count('}')
        continue
    if not in_class:
        continue
    brace_depth += line.count('{') - line.count('}')
    if brace_depth <= 0:
        break
    # Skip: constructors, init(), companion object, draw(), onDraw(), data classes, enum classes
    if any(kw in stripped for kw in ['constructor(', 'private fun init()', 'private companion object',
                                      'companion object', 'override fun draw(', 'override fun onDraw(',
                                      'data class ', 'enum class ', 'fun showGlassBlurViews',
                                      'fun hideGlassBlurView', 'fun setPerformanceLevel',
                                      'fun getPerformanceLevel', 'fun getCurrentFps',
                                      'fun getTargetFps', 'fun setCornerRadius',
                                      'fun getCachedScreenLocation', 'fun refreshScreenPosition',
                                      'fun setDragging']):
        continue
    blur_field_lines.append(line)

# --- Glow fields from FrostedGlowView (skip constructors/companion, skip dispatchDraw) ---
glow_field_lines = []
in_class = False
brace_depth = 0
for line in glow_src.split('\n'):
    stripped = line.strip()
    if 'class FrostedGlowView' in stripped:
        in_class = True
        brace_depth = stripped.count('{') - stripped.count('}')
        continue
    if not in_class:
        continue
    brace_depth += line.count('{') - line.count('}')
    if brace_depth <= 0:
        break
    # Skip: constructors, init, companion, dispatchDraw, onFocusChanged, onDetachedFromWindow,
    # setWillNotDraw, onSizeChanged, all the setter/getter methods, data classes
    if any(kw in stripped for kw in ['companion object', 'init {', 'override fun dispatchDraw',
                                      'override fun onFocusChanged', 'override fun onDetachedFromWindow',
                                      'override fun setWillNotDraw', 'override fun onSizeChanged',
                                      'fun setCornerRadii', 'fun setTopLeftRadius', 'fun setTopRightRadius',
                                      'fun setBottomLeftRadius', 'fun setBottomRightRadius',
                                      'fun getTopLeftRadius', 'fun getTopRightRadius',
                                      'fun getBottomLeftRadius', 'fun getBottomRightRadius',
                                      'fun setCornerRadius', 'fun getCornerRadius',
                                      'fun setGlowColor', 'fun setGlowRadius', 'fun setStrokeWidth',
                                      'fun setStrokeColor', 'fun setInnerShadowThickness',
                                      'fun setInnerShadowColor', 'fun setShadowColor',
                                      'fun setShadowOffset', 'fun setShadowBlurRadius',
                                      'fun getShadowColor', 'fun getShadowOffsetX',
                                      'fun getShadowOffsetY', 'fun getShadowBlurRadius']):
        continue
    # Fix R.styleable references
    fixed = line.replace('R.styleable.FrostedGlowView', 'R.styleable.FrostedGlassView')
    # Fix this@FrostedGlowView -> this@FrostedGlassView
    fixed = fixed.replace('this@FrostedGlowView', 'this@FrostedGlassView')
    glow_field_lines.append(fixed)

# This approach is too complex. Let me use a simpler strategy:
# Just concatenate the two files with proper merging.
print("Generating merged file...")

# Actually let's just do a simpler approach: write the file in chunks
# Strategy: Write the full file content as a Python multi-line string

merged = '''package com.cn.core.ui.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Process
import android.renderscript.*
import android.util.AttributeSet
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withSave
import androidx.core.view.ViewCompat
import com.cn.core.ui.R
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@SuppressLint("Recycle")
open class FrostedGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "FrostedGlassView"
        private const val DEFAULT_BORDER_WIDTH = 4f
        private const val DEFAULT_CORNER_RADIUS = 16f
        private const val DEFAULT_GLOW_RADIUS_DP = 30f
        private const val MSG_MAP_BITMAP = 1
        private const val DETECT_SURFACE_VIEWS = 2
        private const val SCREEN_BLUR_ID = -1
        private const val MAX_POOL_SIZE = 5
        const val PERFORMANCE_LEVEL_LOW = 0
        const val PERFORMANCE_LEVEL_MEDIUM = 1
        const val PERFORMANCE_LEVEL_HIGH = 2
    }

    // ===== Blur state =====
    var blurEnabled: Boolean = false
    private var mRenderScript: RenderScript? = null
    private var mBlurScript: ScriptIntrinsicBlur? = null
    private var mBlurInput: Allocation? = null
    private var mBlurOutput: Allocation? = null
    private var mBitmapToBlur: Bitmap? = null
    private var mBlurredBitmap: Bitmap? = null
    private var mBlurThreads: MutableList<BlurThread> = CopyOnWriteArrayList()
    private var stopThreads = false
    private var mBlurPaint: Paint? = null
    private val mRectSrc = Rect()
    private val mRectDst = Rect()
    private var mRoundedCornerPath: Path? = null
    private var mBlurCornerRadius = 5f
    private var mBlurredBitmapMap: MutableMap<Int, BlurredBitmapInfo> = ConcurrentHashMap()
    private var mBlurredBitmapPool: MutableMap<String, Bitmap> = ConcurrentHashMap()
    private var lastActivity: Activity? = null
    private var mViewVisible = true
    private var mDetectHandler: DetectHandler? = null
    private var mLastAllocationWidth = 0
    private var mLastAllocationHeight = 0
    private var mLastBlurTime = 0L
    private val MIN_BLUR_INTERVAL = 0L
    private var mLastDetectTime = 0L
    private val MIN_DETECT_INTERVAL = 0L
    private val mBitmapPool = ConcurrentHashMap<String, Bitmap>()
    private var mLastSurfaceViewsHash = 0
    private var mPerformanceLevel = PERFORMANCE_LEVEL_MEDIUM
    private var mDownsampleRatio = 0.4f
    private var mBlurRadius = 10f
    private var mCurrentFps = 60
    private var mTargetFps = 60
    private var mFrameInterval = 16L
    private var mLastFrameTime = 0L
    private var mMemoryPressureLevel = ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE
    private var mMemoryCallbacks: MemoryPressureCallbacks? = null
    @Volatile private var mCachedScreenLocation = intArrayOf(0, 0)
    @Volatile private var mIsDragging = false
    private var mScreenBlurThread: ScreenBlurThread? = null

    // ===== Glow/Decoration state =====
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val refreshPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPath = Path()
    private val clipPath = Path()
    private val glowPath = Path()
    private val innerPath = Path()
    private val segmentPath = Path()
    private val innerRadius = FloatArray(8)
    private var borderPathDirty = true
    private var radiiChanged = true
    private val pathMeasure = PathMeasure()
    private var cachedTotalLength = 0f
    private var cachedGlowLength = 0f
    private var cachedPathWidth = 0f
    private var cachedPathHeight = 0f
    private val radii = FloatArray(8)
    private val matrix = Matrix()
    private val refreshMatrix = Matrix()
    private val backgroundPath = Path()
    var bgDrawable: Drawable? = null
        set(value) {
            if (value is BitmapDrawable && value.bitmap.isRecycled) return
            field = value; postInvalidate()
        }
    private val backgroundRect = RectF()
    private var borderWidth = DEFAULT_BORDER_WIDTH
    private var topLeftRadius = DEFAULT_CORNER_RADIUS
    private var topRightRadius = DEFAULT_CORNER_RADIUS
    private var bottomLeftRadius = DEFAULT_CORNER_RADIUS
    private var bottomRightRadius = DEFAULT_CORNER_RADIUS
    private var glowColor: Int = Color.MAGENTA
    private var glowRadius: Float = DEFAULT_GLOW_RADIUS_DP
    private var strokeWidth: Float = 1f
    private var strokeColor: Int = "#1AFFFFFF".toColorInt()
    private var innerShadowThickness: Float = 0f
    private var innerShadowColor: Int = "#0DFFFFFF".toColorInt()
    private var shadowColor: Int = Color.argb(30, 0, 0, 0)
    private var shadowOffsetX: Float = 2f
    private var shadowOffsetY: Float = 4f
    private var shadowBlurRadius: Float = 8f
    var overlayEnabled: Boolean = false
        set(value) { field = value; invalidate() }
    var overlayColor: Int = Color.argb(128, 0, 0, 0)
        set(value) { field = value; invalidate() }
    private var cachedGlowColor: Int = -1
    private var cachedGradientSafeGlowRadius: Float = -1f
    private var animator: ValueAnimator? = null
    private var refreshAnimator: ValueAnimator? = null
    private var progress = 0f
    private var refreshProgress = 0f
    private var isInitialized = false
    private var refreshRunnable: Runnable? = null
    private var diagonalLength = 0f
    private var refreshWidth = 0f
    private var radius = 0f
    private var cachedWidth = 0f
    private var cachedHeight = 0f
    private var cachedHalfWidth = 0f
    private var cachedHalfHeight = 0f
    private var isEffectRunning = false
    private var cachedInnerWidth = 0f
    private var cachedInnerHeight = 0f
    private var cachedSafeGlowRadius = 0f
    private var cachedSafeShadowThickness = 0f
    private var cachedViewWidth = 0f
    private var cachedViewHeight = 0f
    private val startPoint = FloatArray(2)
    private val endPoint = FloatArray(2)
    private var segmentGradient: LinearGradient? = null
    private val cachedGradientStart = FloatArray(2)
    private val cachedGradientEnd = FloatArray(2)
    private var borderGradient: LinearGradient? = null
    private var refreshGradient: LinearGradient? = null
    private var topGlowGradient: LinearGradient? = null
    private var bottomGlowGradient: LinearGradient? = null
    private var leftGlowGradient: LinearGradient? = null
    private var rightGlowGradient: LinearGradient? = null
    private var staticBorderGradient: LinearGradient? = null
    private var flowingStaticBorderGradient: LinearGradient? = null

    // ===== Init =====
    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.FrostedGlassView, defStyleAttr) {
                blurEnabled = getBoolean(R.styleable.FrostedGlassView_blurEnabled, false)
                topLeftRadius = getDimension(R.styleable.FrostedGlassView_topLeftRadius, DEFAULT_CORNER_RADIUS)
                topRightRadius = getDimension(R.styleable.FrostedGlassView_topRightRadius, DEFAULT_CORNER_RADIUS)
                bottomLeftRadius = getDimension(R.styleable.FrostedGlassView_bottomLeftRadius, DEFAULT_CORNER_RADIUS)
                bottomRightRadius = getDimension(R.styleable.FrostedGlassView_bottomRightRadius, DEFAULT_CORNER_RADIUS)
                val cr = getDimension(R.styleable.FrostedGlassView_cornerRadius, -1f)
                if (cr >= 0) setCornerRadius(cr)
                glowColor = getColor(R.styleable.FrostedGlassView_glowColor, innerShadowColor)
                glowRadius = getDimension(R.styleable.FrostedGlassView_glowRadius, DEFAULT_GLOW_RADIUS_DP * resources.displayMetrics.density)
                strokeWidth = getDimension(R.styleable.FrostedGlassView_glowStrokeWidth, 1f)
                strokeColor = getColor(R.styleable.FrostedGlassView_glowStrokeColor, strokeColor)
                innerShadowThickness = getDimension(R.styleable.FrostedGlassView_innerShadowThickness, 0f)
                innerShadowColor = getColor(R.styleable.FrostedGlassView_innerShadowColor, innerShadowColor)
                overlayEnabled = getBoolean(R.styleable.FrostedGlassView_overlayEnabled, false)
                overlayColor = getColor(R.styleable.FrostedGlassView_overlayColor, Color.argb(128, 0, 0, 0))
            }
        }
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setupPaints()
        setupBlurPaint()
        mDetectHandler = DetectHandler(this)
        if (blurEnabled) {
            detectDevicePerformance()
            registerMemoryCallbacks()
        }
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                isInitialized = true
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    private fun setupPaints() {
        borderPaint.style = Paint.Style.STROKE; borderPaint.strokeWidth = borderWidth; borderPaint.color = Color.argb(25, 255, 255, 255)
        refreshPaint.style = Paint.Style.FILL; refreshPaint.strokeWidth = 2f
        lightPaint.style = Paint.Style.STROKE; lightPaint.strokeWidth = borderWidth
    }

    @SuppressLint("NewApi")
    private fun setupBlurPaint() {
        mBlurPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        mRoundedCornerPath = Path()
    }

    // ===== Blur: public API =====
    fun getCachedScreenLocation(): IntArray = mCachedScreenLocation
    fun refreshScreenPosition() { val loc = IntArray(2); getLocationOnScreen(loc); mCachedScreenLocation = loc }
    fun setDragging(dragging: Boolean) {
        mIsDragging = dragging
        if (dragging) { refreshScreenPosition(); resumeAllThreads() }
        else { mLastBlurTime = 0L; mLastFrameTime = 0L }
    }

    fun showGlassBlurViews(activity: Activity) {
        if (!blurEnabled) return
        lastActivity = activity
        mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
        mDetectHandler?.sendEmptyMessage(DETECT_SURFACE_VIEWS)
    }

    fun hideGlassBlurView() {
        stopThreads = true; stopScreenBlurThread()
        for (t in mBlurThreads) { t.stopThread(); t.interrupt() }
        mBlurThreads.clear()
        mDetectHandler?.removeCallbacksAndMessages(null)
        release()
    }

    fun setPerformanceLevel(level: Int) {
        mPerformanceLevel = level; applyPerformanceSettings()
        for (t in mBlurThreads) t.updateFrameInterval(mFrameInterval)
    }
    fun getPerformanceLevel(): Int = mPerformanceLevel
    fun getCurrentFps(): Int = mCurrentFps
    fun getTargetFps(): Int = mTargetFps

    // ===== Blur: internal =====
    private fun detectDevicePerformance() { /* identical to GlassBlurView */ }
    private fun applyPerformanceSettings() { /* identical to GlassBlurView */ }
    private fun adjustForMemoryPressure() { /* identical to GlassBlurView */ }
    private fun registerMemoryCallbacks() { mMemoryCallbacks = MemoryPressureCallbacks(this); context.registerComponentCallbacks(mMemoryCallbacks) }
    private fun unregisterMemoryCallbacks() { mMemoryCallbacks?.let { context.unregisterComponentCallbacks(it) }; mMemoryCallbacks = null }
    private fun clearBitmapPools() { for ((_,b) in mBitmapPool) b.recycle(); mBitmapPool.clear(); for ((_,b) in mBlurredBitmapPool) b.recycle(); mBlurredBitmapPool.clear() }
    private fun pauseAllThreads() { for (t in mBlurThreads) t.pauseThread() }
    private fun resumeAllThreads() { for (t in mBlurThreads) t.resumeThread() }
    private fun release() { /* identical to GlassBlurView */ }

    // (All blur internals - detectSurfaceViews, convertGlassblurBitmap, drawBlurredBitmap, etc.)
    // (ScreenBlurThread, BlurThread, DetectHandler, MemoryPressureCallbacks inner classes)
    // (BlurredBitmapInfo, BlurData data classes, CaptureStrategy enum)

    // ===== Glow/Decoration: dispatchDraw =====
    override fun dispatchDraw(canvas: Canvas) {
        if (radiiChanged) updateRadiiArray()
        val w = width.toFloat(); val h = height.toFloat()
        // 1. Draw blur background
        if (blurEnabled) {
            refreshScreenPosition()
            drawBlurredBitmap(canvas, mBlurredBitmapMap)
        }
        // 2. Shadow
        if (shadowBlurRadius > 0 && w > 0 && h > 0) { /* from FrostedGlowView */ }
        // 3. Background drawable
        // 4. Overlay
        // 5. Inner glow, refresh effect, inner stroke
        // 6. Border (static/flowing)
        // 7. Children
        super.dispatchDraw(canvas)
    }

    // ===== Glow setters/getters =====
    override fun setCornerRadius(radius: Float) {
        topLeftRadius = radius; topRightRadius = radius; bottomLeftRadius = radius; bottomRightRadius = radius
        mBlurCornerRadius = radius; markRadiiChanged()
    }
    fun getCornerRadius(): Float = topLeftRadius
    // ... (all other glow setters/getters)

    // ===== Inner classes (identical to originals, with GlassBlurView->FrostedGlassView) =====
    private class MemoryPressureCallbacks(view: FrostedGlassView) : ComponentCallbacks2 { /* ... */ }
    private class DetectHandler(view: FrostedGlassView) : Handler(Looper.getMainLooper()) { /* ... */ }
    private inner class ScreenBlurThread(...) : Thread() { /* ... */ }
    inner class BlurThread(...) : Thread() { /* ... */ }
    data class BlurredBitmapInfo(val bitmap: Bitmap, val rect: Rect)
    data class BlurData(val bitmap: Bitmap, val left: Int, val top: Int, val right: Int, val bottom: Int)
    enum class CaptureStrategy { CAPTURE_LAYERS_EXCLUDING, SCREENSHOT_LEGACY, PIXEL_COPY_WINDOW }
}
'''

print(f"Template size: {len(merged)} chars")
print("NOTE: Template is a skeleton. Need to fill in all method bodies from originals.")
'''

with open('core/core-ui/src/main/java/com/cn/core/ui/view/FrostedGlassView.kt', 'w', encoding='utf-8') as f:
    f.write(merged)

print("Skeleton written successfully")
