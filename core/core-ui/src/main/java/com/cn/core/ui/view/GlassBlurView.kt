package com.cn.core.ui.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.Window
import android.renderscript.*
import android.graphics.Path
import android.os.Looper
import android.view.ViewGroup
import android.os.Process
import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import androidx.core.graphics.scale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.iterator
import androidx.core.graphics.createBitmap
import kotlin.collections.filter
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.toSet
import kotlin.collections.toTypedArray
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.java
import kotlin.ranges.coerceAtLeast
import kotlin.ranges.until

/**
 * 毛玻璃模糊视图，可覆盖在 [SurfaceView]（如视频播放器）上方，
 * 实时截取底层 SurfaceView 内容并应用 RenderScript 高斯模糊效果。
 *
 * ### 核心工作流程
 * 1. 调用 [showGlassBlurViews] 后，自动检测与自身重叠的 [SurfaceView]
 * 2. 为每个重叠的 SurfaceView 启动独立的 [BlurThread] 后台线程
 * 3. [BlurThread] 循环执行：读取缓存位置 → [PixelCopy] 截取 → 主线程 RenderScript 模糊 → 绘制
 *
 * ### 拖动跟随
 * 支持拖动场景下模糊效果流畅跟随，需配合以下 API 使用：
 * - [setDragging]：拖动开始/结束时调用，拖动时自动降低降采样率加快计算
 * - [refreshScreenPosition]：在 ACTION_MOVE 中调用，实时更新屏幕位置缓存
 * - 位置缓存使用 [Volatile] [IntArray] 引用，保证主线程写入与 [BlurThread] 读取的原子性
 *
 * ### 性能优化
 * - 根据设备内存自动选择性能档位（低/中/高），调整降采样率和目标帧率
 * - Bitmap 对象池复用，减少 GC 压力
 * - 内存压力感知，低内存时自动降级
 *
 * ### 使用示例
 * ```kotlin
 * val blurView = findViewById<GlassBlurView>(R.id.blur_view)
 * blurView.showGlassBlurViews(activity)
 *
 * // 拖动支持
 * blurView.setOnTouchListener { v, event ->
 *     when (event.action) {
 *         MotionEvent.ACTION_DOWN -> blurView.setDragging(true)
 *         MotionEvent.ACTION_MOVE -> {
 *             v.translationX += deltaX
 *             v.translationY += deltaY
 *             blurView.refreshScreenPosition()
 *         }
 *         MotionEvent.ACTION_UP -> blurView.setDragging(false)
 *     }
 *     true
 * }
 * ```
 *
 * @property mCornerRadius 模糊区域的圆角半径，默认 5f，可通过 [setCornerRadius] 修改
 */
class GlassBlurView : View {
    private var mRenderScript: RenderScript? = null
    private var mBlurScript: ScriptIntrinsicBlur? = null
    private var mBlurInput: Allocation? = null
    private var mBlurOutput: Allocation? = null
    private var mBitmapToBlur: Bitmap? = null
    private var mBlurredBitmap: Bitmap? = null
    private val mOverlayColor = 0xee000000.toInt()
    private var mBlurThreads: MutableList<BlurThread> = CopyOnWriteArrayList()
    private var stopThreads = false
    private var mPaint: Paint? = null
    private val mRectSrc = Rect()
    private val mRectDst = Rect()
    private val MSG_MAP_BITMAP = 1
    private val TAG = GlassBlurView::class.java.simpleName

    private var mRoundedCornerPath: Path? = null
    private var mCornerRadius = 5f

    private var mBlurredBitmapMap: MutableMap<Int, BlurredBitmapInfo> = ConcurrentHashMap()
    
    private var mBlurredBitmapPool: MutableMap<String, Bitmap> = ConcurrentHashMap()

    private var lastActivity: Activity? = null

    private var mViewVisible = true
    
    private val DETECT_SURFACE_VIEWS = 2
    private var mDetectHandler: DetectHandler? = null
    
    private var mLastAllocationWidth = 0
    private var mLastAllocationHeight = 0
    
    private var mLastBlurTime = 0L
    private val MIN_BLUR_INTERVAL = 0L
    
    private var mLastDetectTime = 0L
    private val MIN_DETECT_INTERVAL = 0L
    
    private val mBitmapPool = ConcurrentHashMap<String, Bitmap>()
    private val MAX_POOL_SIZE = 5
    
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
    
    // 拖动模式：主线程实时更新屏幕位置缓存，BlurThread 直接读取（无阻塞）
    // 使用 @Volatile IntArray 引用，保证 X/Y 原子性读取，避免竞态
    @Volatile
    private var mCachedScreenLocation = intArrayOf(0, 0)
    @Volatile
    private var mIsDragging = false
    
    // 屏幕截图模式：当没有重叠的 SurfaceView 时，直接从 Window 截取屏幕内容（含壁纸层）
    private var mScreenBlurThread: ScreenBlurThread? = null
    private val SCREEN_BLUR_ID = -1 // 屏幕截图模式的唯一标识
    
    fun getCachedScreenLocation(): IntArray = mCachedScreenLocation
    
    /**
     * 刷新屏幕位置缓存。
     *
     * 使用 [getLocationOnScreen] 获取当前绝对屏幕坐标，以 [Volatile] [IntArray] 引用
     * 原子性写入，确保 [BlurThread] 读取到的 X/Y 坐标来自同一次更新，避免竞态条件。
     *
     * **调用时机**：主线程的 `ACTION_MOVE` 事件中调用。
     */
    fun refreshScreenPosition() {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        mCachedScreenLocation = loc
    }
    
    /**
     * 设置拖动状态。
     *
     * - `dragging = true`：刷新位置缓存，恢复所有 [BlurThread]，拖动期间降采样率降至 0.15f 加快模糊计算
     * - `dragging = false`：重置帧率节流时间戳，确保下一帧立即更新到最终位置的模糊效果
     *
     * @param dragging 是否处于拖动状态
     */
    fun setDragging(dragging: Boolean) {
        mIsDragging = dragging
        if (dragging) {
            refreshScreenPosition()
            resumeAllThreads()
        } else {
            // 拖动结束，重置帧率控制让下一帧立即更新
            mLastBlurTime = 0L
            mLastFrameTime = 0L
        }
    }
    
    private companion object {
        const val PERFORMANCE_LEVEL_LOW = 0
        const val PERFORMANCE_LEVEL_MEDIUM = 1
        const val PERFORMANCE_LEVEL_HIGH = 2
        
        const val MEMORY_PRESSURE_LOW = 0
        const val MEMORY_PRESSURE_MEDIUM = 1
        const val MEMORY_PRESSURE_HIGH = 2
        const val MEMORY_PRESSURE_CRITICAL = 3
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    /**
     * 初始化画笔、圆角路径、硬件加速层、禁用点击反馈，
     * 并检测设备性能和注册内存压力回调。
     */
    @SuppressLint("NewApi")
    private fun init() {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        mRoundedCornerPath = Path()
        setLayerType(LAYER_TYPE_HARDWARE, null)
        
        // 禁用点击反馈效果
        isClickable = false
        isFocusable = false
        foreground = null
        
        mDetectHandler = DetectHandler(this)
        
        detectDevicePerformance()
        registerMemoryCallbacks()
    }
    
    /**
     * 检测设备性能等级。
     *
     * 根据 [ActivityManager.isLowRamDevice]、应用内存类别（memoryClass）和设备总内存
     * 判定为 [PERFORMANCE_LEVEL_LOW]、[PERFORMANCE_LEVEL_MEDIUM] 或 [PERFORMANCE_LEVEL_HIGH]。
     */
    private fun detectDevicePerformance() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        activityManager?.let { am ->
            val isLowRamDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.isLowRamDevice
            } else {
                false
            }
            
            val memoryClass = am.memoryClass
            val totalMem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                val memInfo = android.app.ActivityManager.MemoryInfo()
                am.getMemoryInfo(memInfo)
                memInfo.totalMem
            } else {
                (memoryClass * 1024 * 1024).toLong()
            }
            
            mPerformanceLevel = when {
                isLowRamDevice || memoryClass <= 64 -> PERFORMANCE_LEVEL_LOW
                memoryClass >= 256 && totalMem >= 4L * 1024 * 1024 * 1024 -> PERFORMANCE_LEVEL_HIGH
                else -> PERFORMANCE_LEVEL_MEDIUM
            }
            
            applyPerformanceSettings()
        }
    }
    
    /**
     * 根据 [mPerformanceLevel] 设置降采样率、模糊半径、目标帧率和帧间隔，
     * 随后调用 [adjustForMemoryPressure] 叠加内存压力降级。
     */
    private fun applyPerformanceSettings() {
        when (mPerformanceLevel) {
            PERFORMANCE_LEVEL_LOW -> {
                mDownsampleRatio = 0.25f
                mBlurRadius = 8f
                mTargetFps = 30
                mFrameInterval = 33L
            }
            PERFORMANCE_LEVEL_MEDIUM -> {
                mDownsampleRatio = 0.4f
                mBlurRadius = 10f
                mTargetFps = 60
                mFrameInterval = 16L
            }
            PERFORMANCE_LEVEL_HIGH -> {
                mDownsampleRatio = 0.5f
                mBlurRadius = 12f
                mTargetFps = 60
                mFrameInterval = 16L
            }
        }
        
        adjustForMemoryPressure()
    }
    
    /**
     * 根据当前内存压力级别进一步降低性能参数。
     *
     * - [ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW]：降采样率 ×0.8，目标帧率 ×0.75
     * - [ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL]：降采样率 ×0.6，目标帧率 ×0.5，清空 Bitmap 池
     */
    private fun adjustForMemoryPressure() {
        when (mMemoryPressureLevel) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                mDownsampleRatio *= 0.8f
                mTargetFps = (mTargetFps * 0.75).toInt()
                mFrameInterval = (1000L / mTargetFps)
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                mDownsampleRatio *= 0.6f
                mTargetFps = (mTargetFps * 0.5).toInt()
                mFrameInterval = (1000L / mTargetFps)
                clearBitmapPools()
            }
        }
    }
    
    /** 注册全局 [ComponentCallbacks2]，监听内存压力和配置变化。 */
    private fun registerMemoryCallbacks() {
        mMemoryCallbacks = MemoryPressureCallbacks(this)
        context.registerComponentCallbacks(mMemoryCallbacks)
    }
    
    /** 注销全局 [ComponentCallbacks2]，防止内存泄漏。 */
    private fun unregisterMemoryCallbacks() {
        mMemoryCallbacks?.let {
            context.unregisterComponentCallbacks(it)
        }
        mMemoryCallbacks = null
    }
    
    /**
     * 内存压力回调，通过 [WeakReference] 持有 [GlassBlurView] 引用避免泄漏。
     *
     * - [onTrimMemory]：转发到 [handleMemoryPressure]
     * - [onLowMemory]：以 [ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL] 级别处理
     */
    private class MemoryPressureCallbacks(view: GlassBlurView) : ComponentCallbacks2 {
        private val mViewRef: WeakReference<GlassBlurView> = WeakReference(view)
        
        override fun onTrimMemory(level: Int) {
            val view = mViewRef.get() ?: return
            view.handleMemoryPressure(level)
        }
        
        override fun onConfigurationChanged(newConfig: Configuration) {}
        
        override fun onLowMemory() {
            val view = mViewRef.get() ?: return
            view.handleMemoryPressure(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
        }
    }
    
    /**
     * 处理系统内存压力回调。
     *
     * 更新 [mMemoryPressureLevel]，根据级别降低降采样率/帧率或暂停模糊线程。
     * 与 [adjustForMemoryPressure] 配合使用，先处理本次压力再叠加已有压力。
     *
     * @param level 内存压力级别，来自 [ComponentCallbacks2.onTrimMemory]
     */
    private fun handleMemoryPressure(level: Int) {
        mMemoryPressureLevel = level
        
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                mDownsampleRatio *= 0.8f
                mTargetFps = (mTargetFps * 0.75).toInt()
                mFrameInterval = (1000L / mTargetFps)
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                mDownsampleRatio *= 0.6f
                mTargetFps = (mTargetFps * 0.5).toInt()
                mFrameInterval = (1000L / mTargetFps)
                clearBitmapPools()
            }
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                pauseAllThreads()
            }
        }
        
        adjustForMemoryPressure()
    }
    
    /** 回收并清空 [mBitmapPool] 和 [mBlurredBitmapPool] 中所有缓存的 Bitmap。 */
    private fun clearBitmapPools() {
        for ((_, bitmap) in mBitmapPool) {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        mBitmapPool.clear()
        
        for ((_, bitmap) in mBlurredBitmapPool) {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        mBlurredBitmapPool.clear()
    }
    
    /** 暂停所有 [BlurThread]，用于 UI 不可见或内存压力时节省资源。 */
    private fun pauseAllThreads() {
        for (thread in mBlurThreads) {
            thread.pauseThread()
        }
    }
    
    /** 恢复所有 [BlurThread]，用于 UI 重新可见或拖动开始时恢复模糊更新。 */
    private fun resumeAllThreads() {
        for (thread in mBlurThreads) {
            thread.resumeThread()
        }
    }
    
    /**
     * 主线程 Handler，定时触发 [detectSurfaceViews] 以检测重叠的 SurfaceView 变化。
     * 使用 [WeakReference] 避免持有 GlassBlurView 导致泄漏。
     */
    private class DetectHandler(view: GlassBlurView) : Handler(Looper.getMainLooper()) {
        private val mViewRef: WeakReference<GlassBlurView> = WeakReference(view)
        
        override fun handleMessage(msg: Message) {
            val view = mViewRef.get() ?: return
            when (msg.what) {
                view.DETECT_SURFACE_VIEWS -> {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - view.mLastDetectTime >= view.MIN_DETECT_INTERVAL) {
                        view.mLastDetectTime = currentTime
                        view.detectSurfaceViews()
                    }
                    sendEmptyMessageDelayed(view.DETECT_SURFACE_VIEWS, view.MIN_DETECT_INTERVAL)
                }
            }
        }
    }
    
    /**
     * 启动模糊效果。检测 Activity 中与自身重叠的 [SurfaceView]，
     * 并为每个 SurfaceView 启动独立的 [BlurThread] 进行实时模糊。
     *
     * @param activity 所属的 Activity，用于遍历 View 树查找 SurfaceView
     */
    fun showGlassBlurViews(activity: Activity) {
        lastActivity = activity
        mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
        mDetectHandler?.sendEmptyMessage(DETECT_SURFACE_VIEWS)
    }
    
    /**
     * 遍历 Activity 的 DecorView，查找与自身屏幕区域重叠的 [SurfaceView]。
     * 通过 hashCode 去重，仅在 SurfaceView 集合变化时触发线程的创建/销毁。
     */
    private fun detectSurfaceViews() {
        val activity = lastActivity ?: return
        val overlappingSurfaceViews = mutableListOf<SurfaceView>()
        activity.window.decorView.findAllSurfaceViews(overlappingSurfaceViews, this)
        
        val currentHash = overlappingSurfaceViews.hashCode()
        if (currentHash == mLastSurfaceViewsHash && overlappingSurfaceViews.isNotEmpty()) {
            return
        }
        mLastSurfaceViewsHash = currentHash
        
        if (overlappingSurfaceViews.isNotEmpty()) {
            // 有 SurfaceView，使用原有的 BlurThread 模式
            stopScreenBlurThread()
            Log.d(TAG, "[Detect] Found ${overlappingSurfaceViews.size} overlapping SurfaceView(s)")
            showGlassBlurViewsInternal(overlappingSurfaceViews.toTypedArray(), activity)
        } else {
            // 无 SurfaceView，启动屏幕截图模式（透明 Activity 下截取包含壁纸层的屏幕内容）
            Log.d(TAG, "[Detect] No overlapping SurfaceView, starting screen capture mode")
            clearAllBlurEffects()
            startScreenBlurThread(activity)
        }
    }
    
    /**
     * 递归遍历 View 树，收集与 [glassBlurView] 屏幕区域重叠的 [SurfaceView]。
     *
     * 使用矩形碰撞检测判断两个 View 的屏幕坐标是否相交。
     *
     * @param list 收集结果
     * @param glassBlurView 模糊视图，用于计算屏幕区域
     */
    private fun View.findAllSurfaceViews(list: MutableList<SurfaceView>, glassBlurView: GlassBlurView) {
        if (this is SurfaceView) {
            val glassBlurLocation = IntArray(2)
            val surfaceLocation = IntArray(2)
            glassBlurView.getLocationOnScreen(glassBlurLocation)
            this.getLocationOnScreen(surfaceLocation)
            
            val glassBlurRight = glassBlurLocation[0] + glassBlurView.width
            val glassBlurBottom = glassBlurLocation[1] + glassBlurView.height
            val surfaceRight = surfaceLocation[0] + this.width
            val surfaceBottom = surfaceLocation[1] + this.height
            
            if (glassBlurLocation[0] < surfaceRight && glassBlurRight > surfaceLocation[0] &&
                glassBlurLocation[1] < surfaceBottom && glassBlurBottom > surfaceLocation[1]) {
                list.add(this)
            }
        }
        
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                getChildAt(i).findAllSurfaceViews(list, glassBlurView)
            }
        }
    }
    
    /**
     * 管理 [BlurThread] 的生命周期：为新增的 SurfaceView 创建线程，移除不再重叠的线程。
     *
     * @param surfaceViews 当前与 GlassBlurView 重叠的 SurfaceView 数组
     * @param context 上下文，用于创建 BlurThread
     */
    private fun showGlassBlurViewsInternal(
        surfaceViews: Array<SurfaceView>,
        context: Context
    ) {
        stopThreads = false
        
        val existingThreadIds = mBlurThreads.map { it.surfaceId }.toSet()
        
        val currentSurfaceIds = surfaceViews.map { it.hashCode() }.toSet()
        
        val threadsToRemove = mBlurThreads.filter { !currentSurfaceIds.contains(it.surfaceId) }
        for (thread in threadsToRemove) {
            thread.stopThread()
            thread.interrupt()
            mBlurThreads.remove(thread)
        }
        
        for (surfaceView in surfaceViews) {
            val surfaceId = surfaceView.hashCode()
            if (!existingThreadIds.contains(surfaceId)) {
                val blurThread = BlurThread(
                    context, surfaceView, surfaceId, this
                )
                blurThread.name = "GlassBlurViewThread-$surfaceId"
                mBlurThreads.add(blurThread)
                blurThread.start()
            }
        }
    }
    
    /**
     * 停止所有 [BlurThread]，回收所有缓存的 Bitmap（模糊结果、源图池、模糊图池），触发重绘。
     * 用于 SurfaceView 不再与 GlassBlurView 重叠时清理资源。
     */
    private fun clearAllBlurEffects() {
        stopScreenBlurThread()
        
        for (thread in mBlurThreads) {
            thread.stopThread()
            thread.interrupt()
        }
        mBlurThreads.clear()
        
        for ((_, blurredBitmapInfo) in mBlurredBitmapMap) {
            if (!blurredBitmapInfo.bitmap.isRecycled) blurredBitmapInfo.bitmap.recycle()
        }
        mBlurredBitmapMap.clear()
        
        for ((_, bitmap) in mBitmapPool) {
            bitmap.recycle()
        }
        mBitmapPool.clear()
        
        for ((_, bitmap) in mBlurredBitmapPool) {
            bitmap.recycle()
        }
        mBlurredBitmapPool.clear()
        
        mLastSurfaceViewsHash = 0
        
        postInvalidate()
    }

    /**
     * 在主线程中执行 RenderScript 高斯模糊，结果存入 [mBlurredBitmapMap]。
     *
     * 处理流程：
     * 1. 帧率节流检查（[mFrameInterval] + [MIN_BLUR_INTERVAL]）
     * 2. 初始化 RenderScript 环境（惰性创建，线程安全）
     * 3. 降采样：拖动时 0.15f，正常时按性能档位
     * 4. 从 [mBitmapPool] 复用源 Bitmap，从 [mBlurredBitmapPool] 复用输出 Bitmap
     * 5. RenderScript 执行模糊，结果存入 [mBlurredBitmapMap]，旧结果回收到池中
     * 6. 局部重绘 [postInvalidate]
     *
     * @param context 上下文
     * @param originBitmap 从 SurfaceView 截取的原始像素
     * @param radius 模糊半径（px）
     * @param id SurfaceView 的标识（hashCode）
     * @param left 绘制区域左边界（相对于 GlassBlurView）
     * @param top 绘制区域上边界
     * @param right 绘制区域右边界
     * @param bottom 绘制区域下边界
     */
    private fun convertGlassblurBitmap(
        context: Context,
        originBitmap: Bitmap,
        radius: Float,
        id: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        if (originBitmap.isRecycled || !isAttachedToWindow) return

        val currentTime = System.currentTimeMillis()
        
        if (currentTime - mLastFrameTime < mFrameInterval) {
            return
        }
        mLastFrameTime = currentTime
        
        if (currentTime - mLastBlurTime < MIN_BLUR_INTERVAL) {
            return
        }
        mLastBlurTime = currentTime
        
        if (mRenderScript == null) {
            try {
                mRenderScript = RenderScript.create(context)
                mBlurScript = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript))
            } catch (e: RSRuntimeException) {
                release()
                return
            }
        }

        val width = Math.max(1, (originBitmap.width * if (mIsDragging) 0.15f else mDownsampleRatio).toInt())
        val height = Math.max(1, (originBitmap.height * if (mIsDragging) 0.15f else mDownsampleRatio).toInt())

        var bitmapToBlur: Bitmap? = null
        var blurredBitmap: Bitmap? = null
        var srcFromPool = false

        try {
            val poolKey = "${width}x${height}"
            
            bitmapToBlur = mBitmapPool[poolKey]
            
            if (bitmapToBlur != null && !bitmapToBlur.isRecycled && 
                bitmapToBlur.width == width && bitmapToBlur.height == height) {
                mBitmapPool.remove(poolKey)
                srcFromPool = true
                val canvas = Canvas(bitmapToBlur)
                canvas.drawBitmap(originBitmap, null, Rect(0, 0, width, height), null)
            } else {
                mBitmapPool.remove(poolKey)
                bitmapToBlur?.let { if (!it.isRecycled) it.recycle() }
                bitmapToBlur = try { originBitmap.scale(width, height, false) } catch (_: Exception) { return }
                srcFromPool = false
            }

            if (bitmapToBlur == null || bitmapToBlur.isRecycled) return
            
            blurredBitmap = mBlurredBitmapPool[poolKey]
            if (blurredBitmap == null || blurredBitmap.isRecycled || 
                blurredBitmap.width != width || blurredBitmap.height != height) {
                mBlurredBitmapPool.remove(poolKey)
                blurredBitmap?.let { if (!it.isRecycled) it.recycle() }
                blurredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            } else {
                mBlurredBitmapPool.remove(poolKey)
            }

            if (mBlurInput == null || mLastAllocationWidth != width || mLastAllocationHeight != height) {
                mBlurInput?.destroy()
                mBlurOutput?.destroy()
                
                mBlurInput = Allocation.createFromBitmap(
                    mRenderScript, bitmapToBlur,
                    Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT
                )
                mBlurOutput = Allocation.createTyped(mRenderScript!!, mBlurInput!!.type)
                mLastAllocationWidth = width
                mLastAllocationHeight = height
            } else {
                mBlurInput!!.copyFrom(bitmapToBlur)
            }

            mBlurScript?.setRadius(Math.min(mBlurRadius, 25f))
            mBlurScript?.setInput(mBlurInput!!)
            mBlurScript?.forEach(mBlurOutput!!)
            mBlurOutput!!.copyTo(blurredBitmap)

            val rect = Rect(left, top, right, bottom)
            
            val oldInfo = mBlurredBitmapMap[id]
            if (oldInfo != null && oldInfo.bitmap != blurredBitmap) {
                val oldPoolKey = "${oldInfo.bitmap.width}x${oldInfo.bitmap.height}"
                if (mBlurredBitmapPool.size < MAX_POOL_SIZE) {
                    mBlurredBitmapPool[oldPoolKey] = oldInfo.bitmap
                } else {
                    if (!oldInfo.bitmap.isRecycled) oldInfo.bitmap.recycle()
                }
            }

            blurredBitmap?.let { mBlurredBitmapMap[id] = BlurredBitmapInfo(it, rect) }
            
            if (!srcFromPool && mBitmapPool.size < MAX_POOL_SIZE) {
                val newSrcBmp = try { originBitmap.scale(width, height, false) } catch (_: Exception) { null }
                if (newSrcBmp != null && !newSrcBmp.isRecycled) {
                    mBitmapPool[poolKey] = newSrcBmp
                }
            } else if (!srcFromPool) {
                bitmapToBlur?.recycle()
            }

            postInvalidate(left, top, right, bottom)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (!srcFromPool) bitmapToBlur?.let { if (!it.isRecycled) it.recycle() }
            blurredBitmap?.let { if (!it.isRecycled) it.recycle() }
        }
    }

    /**
     * 启动屏幕截图模糊线程。
     *
     * 当没有与 GlassBlurView 重叠的 SurfaceView 时，使用 [PixelCopy] 从 Activity 的 Window
     * 截取屏幕内容。对于透明背景的 Activity，截取的内容会包含系统壁纸层。
     *
     * @param activity 所属的 Activity
     */
    @SuppressLint("NewApi")
    private fun startScreenBlurThread(activity: Activity) {
        if (mScreenBlurThread != null && mScreenBlurThread?.isAlive == true) {
            Log.d(TAG, "[ScreenBlur] Thread already running, skip")
            return
        }
        val window = activity.window
        val decorView = window.decorView
        if (decorView.width <= 0 || decorView.height <= 0) {
            Log.w(TAG, "[ScreenBlur] DecorView not ready: ${decorView.width}x${decorView.height}")
            return
        }

        Log.d(TAG, "[ScreenBlur] Starting screen blur thread, decorView=${decorView.width}x${decorView.height}, " +
            "view=${width}x${height}, screenLocation=${mCachedScreenLocation.contentToString()}")
        mScreenBlurThread = ScreenBlurThread(window, this)
        mScreenBlurThread?.name = "GlassBlurView-ScreenThread"
        mScreenBlurThread?.start()
    }

    /**
     * 停止并清理屏幕截图模糊线程。
     */
    private fun stopScreenBlurThread() {
        mScreenBlurThread?.stopThread()
        mScreenBlurThread?.interrupt()
        mBlurredBitmapMap.remove(SCREEN_BLUR_ID)
        mScreenBlurThread = null
    }

    /**
     * 屏幕截图模糊线程。
     *
     * 根据 Android 版本选择不同截图策略：
     * - Android 12+ (API 31)：使用 `SurfaceControl.captureLayersExcluding()` 反射，
     *   返回 `ScreenshotHardwareBuffer`，通过 `getHardwareBuffer()` + `Bitmap.wrapHardwareBuffer()` 转为 Bitmap
     * - Android 11 及以下：使用 `SurfaceControl.screenshot(Rect, int, int, int, int)` 反射，直接返回 Bitmap
     * - 最终兜底：使用公开 API `PixelCopy.request(Window, Rect, Bitmap, listener)`
     */
    @SuppressLint("NewApi", "PrivateApi", "DiscouragedPrivateApi")
    private inner class ScreenBlurThread(
        private val window: Window,
        private val glassBlurView: GlassBlurView
    ) : Thread() {

        private val mCallback = Handler.Callback { msg ->
            if (msg != null && msg.what == MSG_MAP_BITMAP) {
                val data = msg.obj as BlurData
                convertGlassblurBitmap(
                    glassBlurView.context,
                    data.bitmap,
                    10f,
                    SCREEN_BLUR_ID,
                    data.left,
                    data.top,
                    data.right,
                    data.bottom
                )
            }
            true
        }

        private val mHandler = Handler(mCallback)
        private var mStopThread = false
        private var mThreadFrameInterval = 16L

        private var mStrategy = CaptureStrategy.PIXEL_COPY_WINDOW
        // 反射缓存
        private var mCaptureMethod: java.lang.reflect.Method? = null
        private var mGetHardwareBufferMethod: java.lang.reflect.Method? = null
        private var mInitialized = false

        fun stopThread() {
            mStopThread = true
        }

        /**
         * 初始化截图策略和反射方法。
         */
        private fun init() {
            if (mInitialized) return
            mInitialized = true
            Log.d(TAG, "[ScreenBlur] SDK=${Build.VERSION.SDK_INT}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+：尝试 captureLayersExcluding
                if (initCaptureLayersExcluding()) return
                // 尝试 screenshot（某些厂商可能保留）
                if (initScreenshot()) return
            } else {
                // Android 11 及以下：使用 screenshot
                if (initScreenshot()) return
            }

            // 兜底：PixelCopy.request(Window, ...) 公开 API
            mStrategy = CaptureStrategy.PIXEL_COPY_WINDOW
            Log.d(TAG, "[ScreenBlur] Fallback to PixelCopy(Window)")
        }

        /**
         * Android 12+: 反射 `SurfaceControl.captureLayersExcluding(SurfaceControl, Rect)`
         * 返回 `ScreenshotHardwareBuffer`，通过 `getHardwareBuffer()` 获取 `HardwareBuffer`，
         * 再用 `Bitmap.wrapHardwareBuffer()` 转为 Bitmap。
         */
        @SuppressLint("BlockedPrivateApi")
        private fun initCaptureLayersExcluding(): Boolean {
            try {
                val scClass = Class.forName("android.view.SurfaceControl")
                val rectClass = Rect::class.java
                val shbClass = Class.forName("android.view.SurfaceControl\$ScreenshotHardwareBuffer")

                // 枚举所有含 "capture" 的方法
                for (m in scClass.declaredMethods) {
                    if (m.name.contains("capture", ignoreCase = true)) {
                        Log.d(TAG, "[ScreenBlur] Candidate: ${m.name}(${m.parameterTypes.joinToString { it.simpleName }}) -> ${m.returnType.simpleName}")
                    }
                }

                // 尝试 captureLayersExcluding(SurfaceControl, Rect) -> ScreenshotHardwareBuffer
                val scInstance = scClass.getDeclaredConstructor().newInstance()
                try {
                    val method = scClass.getDeclaredMethod("captureLayersExcluding", scClass, rectClass)
                    method.isAccessible = true
                    mCaptureMethod = method

                    // 缓存 ScreenshotHardwareBuffer.getHardwareBuffer()
                    mGetHardwareBufferMethod = shbClass.getDeclaredMethod("getHardwareBuffer")

                    mStrategy = CaptureStrategy.CAPTURE_LAYERS_EXCLUDING
                    Log.d(TAG, "[ScreenBlur] Using captureLayersExcluding strategy")
                    return true
                } catch (e: NoSuchMethodException) {
                    Log.d(TAG, "[ScreenBlur] captureLayersExcluding not found: ${e.message}")
                }

                // 尝试其他 capture 方法名
                for (m in scClass.declaredMethods) {
                    if (m.name.startsWith("capture") && m.parameterCount == 2 &&
                        m.parameterTypes[0] == scClass && m.parameterTypes[1] == rectClass) {
                        m.isAccessible = true
                        mCaptureMethod = m
                        try {
                            mGetHardwareBufferMethod = m.returnType.getDeclaredMethod("getHardwareBuffer")
                        } catch (_: Exception) {}
                        mStrategy = CaptureStrategy.CAPTURE_LAYERS_EXCLUDING
                        Log.d(TAG, "[ScreenBlur] Using ${m.name} strategy")
                        return true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[ScreenBlur] captureLayersExcluding init failed: ${e.message}")
            }
            return false
        }

        /**
         * Android 11 及以下：反射 `SurfaceControl.screenshot(Rect, int, int, int, int)` -> Bitmap
         */
        private fun initScreenshot(): Boolean {
            return try {
                val scClass = Class.forName("android.view.SurfaceControl") ?: return false
                val rectClass = Rect::class.java
                val intClass = Int::class.javaPrimitiveType ?: return false

                val candidates = listOf(
                    arrayOf(rectClass, intClass, intClass, intClass, intClass),
                    arrayOf(rectClass, intClass, intClass, intClass),
                    arrayOf(rectClass),
                )

                for (params in candidates) {
                    val foundMethod = findDeclaredMethod(scClass, "screenshot", *params)
                    if (foundMethod != null && foundMethod.returnType == Bitmap::class.java) {
                        foundMethod.isAccessible = true
                        mCaptureMethod = foundMethod
                        mStrategy = CaptureStrategy.SCREENSHOT_LEGACY
                        Log.d(TAG, "[ScreenBlur] Using screenshot(${params.size} params) strategy")
                        return true
                    }
                }
                false
            } catch (e: Exception) {
                Log.e(TAG, "[ScreenBlur] screenshot init failed: ${e.message}")
                false
            }
        }
        
        private fun findDeclaredMethod(clazz: Class<*>, name: String, vararg paramTypes: Class<*>?): java.lang.reflect.Method? {
            return try { clazz.getDeclaredMethod(name, *paramTypes.filterNotNull().toTypedArray()) }
            catch (_: NoSuchMethodException) { null }
        }

        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            init()

            Log.d(TAG, "[ScreenBlur] Thread started, strategy=$mStrategy")

            while (!mStopThread && !stopThreads && !isInterrupted) {
                try {
                    mThreadFrameInterval = glassBlurView.mFrameInterval

                    if (!mViewVisible) {
                        try { sleep(mThreadFrameInterval * 2) }
                        catch (e: InterruptedException) { currentThread().interrupt(); break }
                        continue
                    }

                    val glassBlurLocation = glassBlurView.getCachedScreenLocation().copyOf()
                    val viewWidth = glassBlurView.width
                    val viewHeight = glassBlurView.height

                    if (viewWidth <= 0 || viewHeight <= 0) {
                        try { sleep(mThreadFrameInterval * 2) }
                        catch (e: InterruptedException) { currentThread().interrupt(); break }
                        continue
                    }

                    val captureRect = Rect(
                        glassBlurLocation[0],
                        glassBlurLocation[1],
                        glassBlurLocation[0] + viewWidth,
                        glassBlurLocation[1] + viewHeight
                    )

                    val screenshot: Bitmap? = when (mStrategy) {
                        CaptureStrategy.CAPTURE_LAYERS_EXCLUDING -> captureViaLayers(captureRect, viewWidth, viewHeight)
                        CaptureStrategy.SCREENSHOT_LEGACY -> captureViaScreenshot(captureRect, viewWidth, viewHeight)
                        CaptureStrategy.PIXEL_COPY_WINDOW -> captureViaPixelCopy(captureRect, viewWidth, viewHeight)
                    }

                    if (screenshot != null && !screenshot.isRecycled) {
                        val data = BlurData(screenshot, 0, 0, viewWidth, viewHeight)
                        val msg = Message.obtain()
                        msg.obj = data
                        msg.what = MSG_MAP_BITMAP
                        mHandler.sendMessage(msg)
                    } else {
                        mHandler.post {
                            mBlurredBitmapMap.remove(SCREEN_BLUR_ID)
                            glassBlurView.postInvalidate()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "[ScreenBlur] Loop error: ${e.message}")
                    mHandler.post {
                        mBlurredBitmapMap.remove(SCREEN_BLUR_ID)
                        glassBlurView.postInvalidate()
                    }
                }

                try { sleep(mThreadFrameInterval) }
                catch (e: InterruptedException) { currentThread().interrupt(); break }
            }

            mHandler.removeCallbacksAndMessages(null)
        }

        /**
         * Android 12+: 通过 captureLayersExcluding 截取屏幕。
         */
        private fun captureViaLayers(rect: Rect, w: Int, h: Int): Bitmap? {
            val method = mCaptureMethod ?: return null
            return try {
                val scClass = Class.forName("android.view.SurfaceControl")
                val scInstance = scClass.getDeclaredConstructor().newInstance()

                val result = when (method.parameterCount) {
                    2 -> method.invoke(null, scInstance, rect)
                    else -> {
                        Log.w(TAG, "[ScreenBlur] captureLayers paramCount=${method.parameterCount}")
                        return null
                    }
                } ?: return null

                // result 是 ScreenshotHardwareBuffer
                val hbMethod = mGetHardwareBufferMethod
                if (hbMethod != null) {
                    val hardwareBuffer = hbMethod.invoke(result) as? android.hardware.HardwareBuffer ?: return null
                    // Bitmap.wrapHardwareBuffer(HardwareBuffer, ColorSpace) - API 28+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                        if (bitmap != null) {
                            Log.d(TAG, "[ScreenBlur] captureLayers success: ${bitmap.width}x${bitmap.height}")
                            return bitmap
                        }
                    }
                }
                Log.w(TAG, "[ScreenBlur] captureLayers result type: ${result.javaClass.simpleName}")
                null
            } catch (e: Exception) {
                Log.e(TAG, "[ScreenBlur] captureLayers failed: ${e.javaClass.simpleName}: ${e.message}")
                null
            }
        }

        /**
         * Android <= 11: 通过 SurfaceControl.screenshot 截取屏幕。
         */
        private fun captureViaScreenshot(rect: Rect, w: Int, h: Int): Bitmap? {
            val method = mCaptureMethod ?: return null
            return try {
                val args = when (method.parameterCount) {
                    5 -> arrayOf(rect, w, h, -1, -1)
                    3 -> arrayOf(rect, w, h)
                    1 -> arrayOf(rect)
                    else -> return null
                }
                val result = method.invoke(null, *args) as? Bitmap
                if (result != null) {
                    Log.d(TAG, "[ScreenBlur] screenshot success: ${result.width}x${result.height}")
                }
                result
            } catch (e: Exception) {
                Log.e(TAG, "[ScreenBlur] screenshot failed: ${e.message}")
                null
            }
        }

        /**
         * 兜底：使用公开 API PixelCopy.request(Window, Rect, Bitmap, listener) 截取。
         * 对于透明 Activity，截取内容可能包含壁纸层（取决于系统实现）。
         */
        @SuppressLint("NewApi")
        private fun captureViaPixelCopy(rect: Rect, w: Int, h: Int): Bitmap? {
            val bitmap = createBitmap(1.coerceAtLeast(w), 1.coerceAtLeast(h))
            val latch = java.util.concurrent.CountDownLatch(1)
            var resultBitmap: Bitmap? = null

            try {
                PixelCopy.request(
                    window, rect, bitmap,
                    PixelCopy.OnPixelCopyFinishedListener { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            resultBitmap = bitmap
                            Log.d(TAG, "[ScreenBlur] PixelCopy(Window) success: ${bitmap.width}x${bitmap.height}")
                        } else {
                            Log.w(TAG, "[ScreenBlur] PixelCopy(Window) failed: result=$copyResult")
                            // 不要 recycle，PixelCopy 内部可能已释放 native 内存，再 recycle 会 SIGSEGV
                        }
                        latch.countDown()
                    },
                    mHandler
                )
                // 等待异步结果（最多 1 秒）
                latch.await(1, java.util.concurrent.TimeUnit.SECONDS)
                // 如果失败，让 bitmap 被 GC 回收（不能 recycle，避免 native crash）
            } catch (e: Exception) {
                Log.e(TAG, "[ScreenBlur] PixelCopy(Window) error: ${e.message}")
            }
            return resultBitmap
        }
    }

    /**
     * 后台模糊线程，为单个 [SurfaceView] 执行截图→模糊→绘制循环。
     *
     * 工作流程：
     * 1. 通过 [getCachedScreenLocation] 无阻塞读取 GlassBlurView 屏幕位置
     * 2. 计算与 SurfaceView 的交集区域
     * 3. [PixelCopy] 从 SurfaceView 截取交集区域的像素
     * 4. 回调主线程 [convertGlassblurBitmap] 进行 RenderScript 高斯模糊
     * 5. 按 [mThreadFrameInterval] 间隔循环
     */
    inner class BlurThread(
        private val context: Context,
        private val view: SurfaceView,
        val surfaceId: Int,
        private val glassBlurView: GlassBlurView
    ) : Thread() {

        private val mCallback = Handler.Callback { msg ->
            if (msg != null) {
                when (msg.what) {
                    MSG_MAP_BITMAP -> {
                        val data = msg.obj as BlurData
                        convertGlassblurBitmap(
                            context,
                            data.bitmap,
                            10f,
                            surfaceId,
                            data.left,
                            data.top,
                            data.right,
                            data.bottom
                        )
                    }
                }
            }
            true
        }

        private val mHandler = Handler(mCallback)
        private var mScreenBitmap: Bitmap? = null
        private var mStopThread = false
        private var mLastScreenWidth = 0
        private var mLastScreenHeight = 0
        private var mThreadFrameInterval = 16L
        private var mCachedSurfaceLocation: IntArray? = null

        fun stopThread() {
            mStopThread = true
        }

        fun pauseThread() {
            mStopThread = true
        }

        fun resumeThread() {
            mStopThread = false
        }
        
        fun updateFrameInterval(interval: Long) {
            mThreadFrameInterval = interval
        }

        private fun sendMessage(what: Int, obj: Any?) {
            val msg = Message.obtain()
            msg.obj = obj
            msg.what = what
            mHandler.sendMessage(msg)
        }

        @SuppressLint("NewApi")
        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            
            while (!mStopThread && !stopThreads && !isInterrupted) {
                try {
                    mThreadFrameInterval = glassBlurView.mFrameInterval
                    
                    if (!mViewVisible) {
                        try {
                            sleep(mThreadFrameInterval * 2)
                        } catch (e: InterruptedException) {
                            currentThread().interrupt()
                            break
                        }
                        continue
                    }

                    // 直接读取主线程缓存的位置，无阻塞（@Volatile IntArray 原子引用）
                    val glassBlurViewLocation = glassBlurView.getCachedScreenLocation().copyOf()
                    
                    // SurfaceView 位置固定（全屏），只需缓存一次
                    val surfaceViewLocation = IntArray(2)
                    if (mCachedSurfaceLocation == null) {
                        val loc = IntArray(2)
                        mHandler.post {
                            if (view.isAttachedToWindow) {
                                view.getLocationOnScreen(loc)
                                mCachedSurfaceLocation = IntArray(2)
                                mCachedSurfaceLocation!![0] = loc[0]
                                mCachedSurfaceLocation!![1] = loc[1]
                            }
                        }
                        try {
                            sleep(32)
                        } catch (e: InterruptedException) {
                            currentThread().interrupt()
                            break
                        }
                        continue
                    } else {
                        surfaceViewLocation[0] = mCachedSurfaceLocation!![0]
                        surfaceViewLocation[1] = mCachedSurfaceLocation!![1]
                    }

                    val surfaceWidth = view.width
                    val surfaceHeight = view.height
                    
                    if (surfaceWidth <= 0 || surfaceHeight <= 0) {
                        try {
                            sleep(mThreadFrameInterval * 2)
                        } catch (e: InterruptedException) {
                            currentThread().interrupt()
                            break
                        }
                        continue
                    }

                    val glassBlurWidth = glassBlurView.width
                    val glassBlurHeight = glassBlurView.height

                    if (glassBlurWidth <= 0 || glassBlurHeight <= 0) {
                        try {
                            sleep(mThreadFrameInterval * 2)
                        } catch (e: InterruptedException) {
                            currentThread().interrupt()
                            break
                        }
                        continue
                    }

                    val left = Math.max(glassBlurViewLocation[0], surfaceViewLocation[0])
                    val top = Math.max(glassBlurViewLocation[1], surfaceViewLocation[1])
                    val right = Math.min(
                        glassBlurViewLocation[0] + glassBlurWidth,
                        surfaceViewLocation[0] + surfaceWidth
                    )
                    val bottom = Math.min(
                        glassBlurViewLocation[1] + glassBlurHeight,
                        surfaceViewLocation[1] + surfaceHeight
                    )

                    if (right <= left || bottom <= top) {
                        mHandler.post {
                            mBlurredBitmapMap.remove(surfaceId)
                            glassBlurView.postInvalidate()
                        }
                        
                        try {
                            sleep(mThreadFrameInterval * 2)
                        } catch (e: InterruptedException) {
                            currentThread().interrupt()
                            break
                        }
                        continue
                    }

                    val relativeLeft = left - surfaceViewLocation[0]
                    val relativeTop = top - surfaceViewLocation[1]
                    val relativeRight = right - surfaceViewLocation[0]
                    val relativeBottom = bottom - surfaceViewLocation[1]

                    val clampedLeft = Math.max(0, relativeLeft)
                    val clampedTop = Math.max(0, relativeTop)
                    val clampedRight = Math.min(surfaceWidth, relativeRight)
                    val clampedBottom = Math.min(surfaceHeight, relativeBottom)

                    if (clampedRight <= clampedLeft || clampedBottom <= clampedTop) {
                        try {
                            sleep(mThreadFrameInterval * 2)
                        } catch (e: InterruptedException) {
                            currentThread().interrupt()
                            break
                        }
                        continue
                    }

                    val width = clampedRight - clampedLeft
                    val height = clampedBottom - clampedTop

                    if (mScreenBitmap == null || mLastScreenWidth != width || mLastScreenHeight != height) {
                        mScreenBitmap?.recycle()
                        mScreenBitmap = createBitmap(
                            1.coerceAtLeast(width),
                            1.coerceAtLeast(height)
                        )
                        mLastScreenWidth = width
                        mLastScreenHeight = height
                    }

                    val tempRect = Rect(clampedLeft, clampedTop, clampedRight, clampedBottom)

                    PixelCopy.request(
                        view, tempRect, mScreenBitmap!!,
                        PixelCopy.OnPixelCopyFinishedListener { copyResult ->

                            if (PixelCopy.SUCCESS == copyResult) {

                                val relativeLeft = left - glassBlurViewLocation[0]
                                val relativeTop = top - glassBlurViewLocation[1]
                                val relativeRight = right - glassBlurViewLocation[0]
                                val relativeBottom = bottom - glassBlurViewLocation[1]

                                val data = BlurData(
                                    mScreenBitmap!!,
                                    relativeLeft,
                                    relativeTop,
                                    relativeRight,
                                    relativeBottom
                                )
                                sendMessage(MSG_MAP_BITMAP, data)
                            } else {
                                mHandler.post {
                                    mBlurredBitmapMap.remove(surfaceId)
                                    glassBlurView.postInvalidate()
                                }
                            }
                        },
                        mHandler
                    )
                } catch (e: IllegalArgumentException) {
                    mHandler.post {
                        mBlurredBitmapMap.remove(surfaceId)
                        glassBlurView.postInvalidate()
                    }
                } catch (e: Exception) {
                }

                try {
                    sleep(mThreadFrameInterval)
                } catch (e: InterruptedException) {
                    currentThread().interrupt()
                    break
                }
            }

            mScreenBitmap?.recycle()
            mHandler.removeCallbacksAndMessages(null)
            mScreenBitmap = null
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)

        mViewVisible = isVisible
        if (isVisible) {
            resumeAllThreads()
            val activity = lastActivity
            if (activity != null) {
                mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
                mDetectHandler?.sendEmptyMessage(DETECT_SURFACE_VIEWS)
            }
        } else {
            pauseAllThreads()
            mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            resumeAllThreads()
            val activity = lastActivity
            if (activity != null) {
                mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
                mDetectHandler?.sendEmptyMessage(DETECT_SURFACE_VIEWS)
            }
        } else {
            pauseAllThreads()
            mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerMemoryCallbacks()
        val activity = lastActivity
        if (activity != null) {
            mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
            mDetectHandler?.sendEmptyMessage(DETECT_SURFACE_VIEWS)
        }
    }

    override fun onDetachedFromWindow() {
        mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
        unregisterMemoryCallbacks()
        hideGlassBlurView()
        super.onDetachedFromWindow()
    }

    override fun draw(canvas: Canvas) {
        // 每帧绘制时更新屏幕位置缓存，确保 BlurThread 读到最新值
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        mCachedScreenLocation = loc
        drawBlurredBitmap(canvas, mBlurredBitmapMap, mOverlayColor)
        super.draw(canvas)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }

    /**
     * 将模糊结果绘制到 Canvas 上。
     * 遍历 [blurredBitmapMap]，对每个模糊 Bitmap 使用圆角裁剪路径绘制到对应区域。
     *
     * @param canvas 画布
     * @param blurredBitmapMap 模糊结果映射（SurfaceView id → [BlurredBitmapInfo]）
     * @param overlayColor 叠加颜色（当前未使用，保留扩展）
     */
    private fun drawBlurredBitmap(
        canvas: Canvas,
        blurredBitmapMap: Map<Int, BlurredBitmapInfo>,
        overlayColor: Int
    ) {
        for ((key, blurredBitmapInfo) in blurredBitmapMap) {
            val blurredBitmap = blurredBitmapInfo.bitmap
            val blurRect = blurredBitmapInfo.rect

            if (null != blurredBitmap) {
                mRectSrc.left = 0
                mRectSrc.top = 0
                mRectSrc.right = blurredBitmap.width
                mRectSrc.bottom = blurredBitmap.height

                mRectDst.left = blurRect.left
                mRectDst.top = blurRect.top
                mRectDst.right = blurRect.right
                mRectDst.bottom = blurRect.bottom

                if (mRoundedCornerPath == null) {
                    mRoundedCornerPath = Path()
                }
                mRoundedCornerPath?.rewind()
                mRoundedCornerPath?.addRoundRect(
                    mRectDst.left.toFloat(),
                    mRectDst.top.toFloat(),
                    mRectDst.right.toFloat(),
                    mRectDst.bottom.toFloat(),
                    mCornerRadius,
                    mCornerRadius,
                    Path.Direction.CW
                )

                val saveCount = canvas.save()

                if (mRoundedCornerPath != null) {
                    canvas.clipPath(mRoundedCornerPath!!)
                }

                canvas.drawBitmap(blurredBitmap, mRectSrc, mRectDst, mPaint)

                canvas.restoreToCount(saveCount)
            }
        }
    }

    /**
     * 设置模糊区域的圆角半径。
     *
     * @param radius 圆角半径（px）
     */
    fun setCornerRadius(radius: Float) {
        mCornerRadius = radius
        postInvalidate()
    }
    
    /**
     * 设置性能级别，影响降采样率、模糊半径和目标帧率。
     *
     * - [PERFORMANCE_LEVEL_LOW]（0）：降采样 0.25，半径 8，目标 30fps
     * - [PERFORMANCE_LEVEL_MEDIUM]（1）：降采样 0.4，半径 10，目标 60fps（默认）
     * - [PERFORMANCE_LEVEL_HIGH]（2）：降采样 0.5，半径 12，目标 60fps
     *
     * @param level 性能级别，取值 0/1/2
     */
    fun setPerformanceLevel(level: Int) {
        mPerformanceLevel = level
        applyPerformanceSettings()
        
        for (thread in mBlurThreads) {
            thread.updateFrameInterval(mFrameInterval)
        }
    }
    
    /**
     * 获取当前性能级别。
     *
     * @return 0（低）、1（中）、2（高）
     */
    fun getPerformanceLevel(): Int = mPerformanceLevel
    
    /** 获取当前实际帧率。 */
    fun getCurrentFps(): Int = mCurrentFps
    
    /** 获取目标帧率。 */
    fun getTargetFps(): Int = mTargetFps

    /**
     * 释放所有资源：Bitmap、RenderScript、Allocation、画笔等，
     * 并重置所有状态变量到初始值。
     */
    private fun release() {
        mBitmapToBlur?.let { if (!it.isRecycled) it.recycle() }
        mBitmapToBlur = null

        mBlurredBitmap?.let { if (!it.isRecycled) it.recycle() }
        mBlurredBitmap = null

        for ((_, blurredBitmapInfo) in mBlurredBitmapMap) {
            if (!blurredBitmapInfo.bitmap.isRecycled) blurredBitmapInfo.bitmap.recycle()
        }
        mBlurredBitmapMap.clear()

        for ((_, bitmap) in mBitmapPool) {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        mBitmapPool.clear()
        
        for ((_, bitmap) in mBlurredBitmapPool) {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        mBlurredBitmapPool.clear()

        try {
            mBlurInput?.destroy()
        } catch (e: Exception) {
        }
        mBlurInput = null

        try {
            mBlurOutput?.destroy()
        } catch (e: Exception) {
        }
        mBlurOutput = null

        try {
            mBlurScript?.destroy()
        } catch (e: Exception) {
        }
        mBlurScript = null

        try {
            mRenderScript?.destroy()
        } catch (e: Exception) {
        }
        mRenderScript = null

        mPaint = null
        mRoundedCornerPath = null
        
        mLastAllocationWidth = 0
        mLastAllocationHeight = 0
        mLastBlurTime = 0L
        mLastDetectTime = 0L
        mLastSurfaceViewsHash = 0
        mLastFrameTime = 0L
        
        mScreenBlurThread = null
        
        mPerformanceLevel = PERFORMANCE_LEVEL_MEDIUM
        mDownsampleRatio = 0.4f
        mBlurRadius = 10f
        mCurrentFps = 60
        mTargetFps = 60
        mFrameInterval = 16L
        mMemoryPressureLevel = ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE
    }

    /**
     * 停止所有模糊效果，释放 RenderScript、Bitmap 等资源。
     * 调用后如需重新启用，需再次调用 [showGlassBlurViews]。
     */
    fun hideGlassBlurView() {
        stopThreads = true
        stopScreenBlurThread()
        for (thread in mBlurThreads) {
            thread.stopThread()
            thread.interrupt()
        }
        mBlurThreads.clear()
        mDetectHandler?.removeCallbacksAndMessages(null)
        release()
    }


    /**
     * 模糊结果缓存，存储模糊后的 Bitmap 及其在 GlassBlurView 中的绘制区域。
     *
     * @param bitmap 已模糊的 Bitmap（降采样后）
     * @param rect 在 GlassBlurView 坐标系中的绘制区域
     */
    data class BlurredBitmapInfo(
        val bitmap: Bitmap,
        val rect: Rect
    )

    /**
     * 模糊任务数据，从 BlurThread 通过 Handler 消息传递到主线程。
     *
     * @param bitmap 从 SurfaceView 截取的原始像素
     * @param left 在 GlassBlurView 坐标系中的左侧坐标
     * @param top 在 GlassBlurView 坐标系中的顶部坐标
     * @param right 在 GlassBlurView 坐标系中的右侧坐标
     * @param bottom 在 GlassBlurView 坐标系中的底部坐标
     */
    data class BlurData(
        val bitmap: Bitmap,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    // 截图策略
    enum class CaptureStrategy {
        CAPTURE_LAYERS_EXCLUDING,  // Android 12+ captureLayersExcluding
        SCREENSHOT_LEGACY,         // Android <= 11 screenshot()
        PIXEL_COPY_WINDOW           // 兜底：PixelCopy.request(Window, ...)
    }
}