package com.cn.core.ui.view.frosted

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RecordingCanvas
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.withClip
import com.cn.core.ui.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * @author: cn
 * @time: 2026/4/25 15:23
 * @history 2026/5/25 v5: 双后端模糊（RenderEffect HW + RS SW） + 位置跟踪
 * @description:毛玻璃发光视图 — 继承 AnimatedStatefulGlowView，添加真实背景模糊（frosted glass）效果
 *
 * ## 双后端架构
 *
 * | 后端 | API 等级 | 引擎 | 特性 |
 * |------|---------|------|------|
 * | Hardware | API 31+ | RenderNode + RenderEffect | GPU 模糊，零 Bitmap 分配，无 RS/协程 |
 * | Software | API < 31 | RenderScript | 后台协程 RS 模糊，Bitmap 池复用 |
 *
 * ## 绘制管线
 * ```
 * dispatchDraw:
 *   1. 背景模糊层（Hardware: RenderNode 离屏模糊 / Software: mDisplayBitmap 直接绘制）
 *   2. overlay / innerGlow / stroke / highlight（不模糊，顶层绘制）
 *   3. flowEffect / refreshEffect（动画效果）
 *   4. super.dispatchDraw（父类绘制）
 * ```
 *
 * ## 位置跟踪
 * 覆写 setTranslationX/Y，自动预计算 srcRect → pendingSrcRect，
 * 消除 capture 帧内 getLocationOnScreen 的开销。
 */
@SuppressLint("Recycle", "ClickableViewAccessibility")
open class FrostedAnimatedGlowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AnimatedStatefulGlowView(context, attrs, defStyleAttr), Choreographer.FrameCallback {

    companion object {
        private const val TAG = "FrostedGlow"
        private const val DEBUG = false

        private const val TARGET_FRAME_TIME_NS = 16_666_667L  // 60fps
        private const val FRAME_HISTORY_SIZE = 8
        private const val FRAME_HISTORY_MASK = FRAME_HISTORY_SIZE - 1
    }

    // ==================== 公开属性 ====================

    var blurEnabled: Boolean? = null
        set(value) { if (value != field) {
            field = value;
            invalidate()
        } }

    var blurredDrawable: Drawable? = null
        private set

    var surfaceView: SurfaceView? = null
        set(value) { if (value != field) {
            field = value
            if (value != null && isAttachedToWindow) startRealtimeBlur()
            else if (sourceBitmap == null) stopRealtimeBlur()
        } }

    var sourceBitmap: Bitmap? = null
        set(value) { if (value != field) {
            field = value
            // 重置截取状态
            mSourceBitmapCaptured = false
            mLastCaptureX = -1
            mLastCaptureY = -1
            mLastCaptureW = -1
            mLastCaptureH = -1
            Log.d(TAG, "sourceBitmap: $value, blurEnabled:$blurEnabled")
            if (blurEnabled == true) {
                if (value != null && isAttachedToWindow) startRealtimeBlur()
                else if (surfaceView == null) stopRealtimeBlur()
            } else {
                captureAndBlurFromSourceBitmap()
            }
        } }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged: w:$w, h:$h")
        // 尺寸变化：丢弃 HW 节点，下次 dispatchDraw 重新构建
        discardHwNode()
        if (blurEnabled != true) {
            captureAndBlurFromSourceBitmap()
        }
    }

    // sourceBitmap 在屏幕上的偏移位置（用于计算截取区域）
    var sourceBitmapOffsetX: Int = 0
        set(value) { if (value != field) { field = value; mSourceBitmapCaptured = false } }

    var sourceBitmapOffsetY: Int = 0
        set(value) { if (value != field) { field = value; mSourceBitmapCaptured = false } }

    // sourceBitmap 相对于原始屏幕的缩放比例（例如 0.25 表示缩小到 1/4）
    var sourceBitmapScale: Float = 0.1f
        set(value) { if (value != field) { field = value.coerceIn(0.01f, 1f); mSourceBitmapCaptured = false } }

    var blurRadius: Float = 15f
        set(value) {
            field = value.coerceIn(0f, 25f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) updateHwBlurRadius()
        }

    var blurScaleFactor: Float = 0.1f
        set(value) { field = value.coerceIn(0.1f, 1f) }

    var autoUpdate: Boolean = false
    var frameSkipCount: Int = 1
    var adaptiveFrameRate: Boolean = true
    var debugMode: Boolean = DEBUG

    // ==================== 预分配对象池 ====================

    private val mTempViewLoc = intArrayOf(0, 0)
    private val mTempSvLoc = intArrayOf(0, 0)
    private val mCaptureRect = Rect()
    private val mSrcRect = Rect()
    private val mDstRect = Rect()
    private val mPaint: Paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { isDither = true }
    private val mScalePaint: Paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { isDither = true }
    private val mScaleMatrix = Matrix()  // 预分配缩放矩阵
    private var mCaptureCanvas: Canvas? = null  // 预分配截取 Canvas

    @Volatile private var mIsCapturing = false
    private val mIsProcessing = AtomicBoolean(false)
    private val mFrameId = AtomicLong(0)

    // sourceBitmap 截取状态（位置变化检测）
    @Volatile private var mLastCaptureX = -1
    @Volatile private var mLastCaptureY = -1
    @Volatile private var mLastCaptureW = -1
    @Volatile private var mLastCaptureH = -1
    @Volatile private var mSourceBitmapCaptured = false

    // ==================== 协程作用域 ====================

    private val mScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mBlurJob: Job? = null

    // Channel 用于帧队列（容量1，背压）
    private val mFrameChannel = Channel<BlurTask>(Channel.CONFLATED)

    data class BlurTask(
        val bitmap: Bitmap,
        val frameId: Long,
        val viewWidth: Int,
        val viewHeight: Int
    )

    // ==================== VSYNC 调度 ====================

    private val mChoreographer: Choreographer by lazy { Choreographer.getInstance() }
    private val mMainHandler = Handler(Looper.getMainLooper())
    private var mVSyncCounter = 0

    @Volatile private var mAdaptiveSkipCount = 1

    // ==================== HW 路径 (API 31+) ====================

    /** 持久化 RenderNode：存储截取内容 + RenderEffect 模糊，GPU 实时合成 */
    private var mHwBlurNode: RenderNode? = null
    private var mHwNodeW = 0
    private var mHwNodeH = 0
    private var mHwBlurRadius = 0f

    // ==================== Bitmap 池 ====================

    @Volatile private var mCaptureBitmap: Bitmap? = null
    @Volatile private var mCaptureW = 0
    @Volatile private var mCaptureH = 0

    @Volatile private var mScaledBitmap: Bitmap? = null
    @Volatile private var mScaledW = 0
    @Volatile private var mScaledH = 0

    @Volatile private var mBackBuffer: Bitmap? = null
    @Volatile private var mBackW = 0
    @Volatile private var mBackH = 0

    @Volatile private var mDisplayBitmap: Bitmap? = null

    private var mScaleCanvas: Canvas? = null

    private inline fun acquireCapture(w: Int, h: Int): Bitmap {
        val existing = mCaptureBitmap
        if (existing != null && !existing.isRecycled && w == mCaptureW && h == mCaptureH) return existing
        existing?.recycle()
        return createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            mCaptureBitmap = it; mCaptureW = w; mCaptureH = h
            mCaptureCanvas = Canvas(it)  // 预分配 Canvas
        }
    }

    private inline fun acquireScaled(w: Int, h: Int): Bitmap {
        val existing = mScaledBitmap
        if (existing != null && !existing.isRecycled && w == mScaledW && h == mScaledH) return existing
        existing?.recycle()
        return createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            mScaledBitmap = it; mScaledW = w; mScaledH = h
            mScaleCanvas = Canvas(it)
        }
    }

    private inline fun acquireBack(w: Int, h: Int): Bitmap {
        val existing = mBackBuffer
        if (existing != null && !existing.isRecycled && w == mBackW && h == mBackH) return existing
        existing?.recycle()
        return createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            mBackBuffer = it; mBackW = w; mBackH = h
        }
    }

    // ==================== RenderScript ====================

    @Volatile private var mRS: RenderScript? = null
    @Volatile private var mBlur: ScriptIntrinsicBlur? = null
    @Volatile private var mAllocIn: Allocation? = null
    @Volatile private var mAllocOut: Allocation? = null
    @Volatile private var mLastAllocW = 0
    @Volatile private var mLastAllocH = 0

    private val rsLock = ReentrantLock()

    private fun ensureRS(): Boolean {
        mRS?.let { return true }
        return try {
            rsLock.withLock {
                mRS?.let { return true }
                RenderScript.create(context).also { rs ->
                    mRS = rs
                    mBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
                }
                true
            }
        } catch (e: Exception) { Log.e(TAG, "RS init failed", e); false }
    }

    private suspend fun blur(input: Bitmap, output: Bitmap): Boolean = withContext(Dispatchers.IO) {
        if (blurRadius <= 0f) return@withContext false
        if (!ensureRS()) return@withContext false

        val rs = mRS ?: return@withContext false
        val blur = mBlur ?: return@withContext false
        val w = input.width
        val h = input.height

        try {
            rsLock.withLock {
                val allocIn: Allocation
                val allocOut: Allocation

                if (w == mLastAllocW && h == mLastAllocH && mAllocIn != null && mAllocOut != null) {
                    allocIn = mAllocIn!!
                    allocOut = mAllocOut!!
                    allocIn.copyFrom(input)
                } else {
                    mAllocIn?.destroy()
                    mAllocOut?.destroy()
                    allocIn = Allocation.createFromBitmap(rs, input,
                        Allocation.MipmapControl.MIPMAP_NONE,
                        Allocation.USAGE_SCRIPT or Allocation.USAGE_SHARED)
                    allocOut = Allocation.createTyped(rs, allocIn.type)
                    mAllocIn = allocIn
                    mAllocOut = allocOut
                    mLastAllocW = w
                    mLastAllocH = h
                }

                blur.setRadius(blurRadius.coerceIn(0.1f, 25f))
                blur.setInput(allocIn)
                blur.forEach(allocOut)
                allocOut.copyTo(output)
            }
            true
        } catch (e: Exception) { Log.e(TAG, "Blur failed", e); false }
    }

    // ==================== 帧时间监控 ====================

    @Volatile private var mFrameStart = 0L
    private val mFrameTimes = LongArray(FRAME_HISTORY_SIZE)
    @Volatile private var mFrameIdx = 0

    private inline fun frameStart() { mFrameStart = System.nanoTime() }

    private inline fun frameEnd() {
        if (!adaptiveFrameRate) return
        val elapsed = System.nanoTime() - mFrameStart
        val idx = mFrameIdx and FRAME_HISTORY_MASK
        mFrameTimes[idx] = elapsed
        mFrameIdx++

        if ((mFrameIdx and FRAME_HISTORY_MASK) == 0) {
            var sum = 0L
            for (i in 0 until FRAME_HISTORY_SIZE) sum += mFrameTimes[i]
            val avg = sum / FRAME_HISTORY_SIZE

            mAdaptiveSkipCount = when {
                avg < TARGET_FRAME_TIME_NS * 3L / 2 -> 1
                avg < TARGET_FRAME_TIME_NS * 5L / 2 -> 2
                avg < TARGET_FRAME_TIME_NS * 4L -> 3
                else -> 4
            }
        }
    }

    // ==================== 绘制 ====================

    override fun dispatchDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        buildDispatchPath(w, h)
        canvas.withClip(dispatchClipPath) {
            if (blurEnabled == true) {
                // ── HW 路径 (API 31+)：GPU 实时模糊，无协程无 Bitmap 分配 ──
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    mHwBlurNode?.let { node -> canvas.drawRenderNode(node) }
                }
                // ── SW 路径 (API < 31)：预模糊 mDisplayBitmap ──
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    mDisplayBitmap?.takeIf { !it.isRecycled }?.let { bg ->
                        mSrcRect.set(0, 0, bg.width, bg.height)
                        mDstRect.set(0, 0, width, height)
                        drawBitmap(bg, mSrcRect, mDstRect, mPaint)
                    }
                }
            } else blurredDrawable?.toBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)?.let {
                mSrcRect.set(0, 0, it.width, it.height)
                mDstRect.set(0, 0, width, height)
                drawBitmap(it, mSrcRect, mDstRect, mPaint)
            }
            drawOverlay(this, w, h)
            drawInnerGlow(this, w, h)
            drawStroke(this, w, h)
            drawTopHighlight(this, w, h)
            if (flowEnabled && isFocused && flowAlpha >= 0.01f) drawFlowEffect(this)
            if (refreshEffectEnabled && isFocused) drawRefreshEffect(this, w, h)
        }
        super.dispatchDraw(canvas)
    }

    // ==================== 截图 + 模糊流水线 ====================

    /**
     * 从 SurfaceView 截取
     */
    @SuppressLint("CheckResult")
    fun captureAndBlurFromSurfaceView() {
        val sv = surfaceView ?: return
        if (mIsCapturing || width <= 0 || height <= 0 || sv.width <= 0 || sv.height <= 0) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!isShown) return

        mIsCapturing = true
        frameStart()

        getLocationOnScreen(mTempViewLoc)
        sv.getLocationOnScreen(mTempSvLoc)

        val left = mTempViewLoc[0] - mTempSvLoc[0]
        val top = mTempViewLoc[1] - mTempSvLoc[1]
        val right = left + width
        val bottom = top + height

        mCaptureRect.set(left, top, right, bottom)
        mCaptureRect.intersect(0, 0, sv.width, sv.height)

        if (mCaptureRect.isEmpty) { mIsCapturing = false; registerNext(); return }

        // 截取 Bitmap（HW/SW 路径共享）
        val capture = acquireCapture(mCaptureRect.width(), mCaptureRect.height())
        val frameId = mFrameId.incrementAndGet()

        try {
            PixelCopy.request(sv, mCaptureRect, capture, { result ->
                if (result == PixelCopy.SUCCESS && frameId == mFrameId.get()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // ── HW 路径：截取内容 → RenderNode + RenderEffect ──
                        postToHwRenderNode(capture, width, height)
                        mIsCapturing = false
                        frameEnd()
                        registerNext()
                    } else {
                        // ── SW 路径：发送到 Channel → RS 模糊 ──
                        mScope.launch {
                            mFrameChannel.send(BlurTask(capture, frameId, width, height))
                        }
                    }
                } else {
                    mIsCapturing = false
                    registerNext()
                }
            }, mMainHandler)
        } catch (e: Exception) {
            mIsCapturing = false
            registerNext()
        }
    }

    /**
     * 从 SourceBitmap 截取
     * HW 路径 (API 31+)：裁剪 source → RenderNode + RenderEffect，零协程
     * SW 路径 (API < 31)：裁剪 source → mDisplayBitmap 直接显示（不模糊）
     * 位置不变时仅截取一次
     */
    fun captureAndBlurFromSourceBitmap() {
        Log.d(TAG, "captureAndBlurFromSourceBitmap: ")
        val src = sourceBitmap ?: return
        Log.d(TAG, "captureAndBlurFromSourceBitmap: 1, $width, $height, ${src.isRecycled}")
        if (width <= 0 || height <= 0 || src.isRecycled) return
        Log.d(TAG, "captureAndBlurFromSourceBitmap: 2")
        if (!isShown) return
        Log.d(TAG, "captureAndBlurFromSourceBitmap: 3")
        // 获取当前 view 屏幕位置
        getLocationOnScreen(mTempViewLoc)
        val curX = mTempViewLoc[0]
        val curY = mTempViewLoc[1]
        val curW = width
        val curH = height

        // 位置和尺寸未变化且已截取过，跳过
        if (mSourceBitmapCaptured &&
            curX == mLastCaptureX && curY == mLastCaptureY &&
            curW == mLastCaptureW && curH == mLastCaptureH) {
            registerNext()
            return
        }
        Log.d(TAG, "captureAndBlurFromSourceBitmap: 4")
        if (mIsCapturing) return
        Log.d(TAG, "captureAndBlurFromSourceBitmap: 5")
        mIsCapturing = true
        frameStart()

        try {
            // 计算 sourceBitmap 中的截取区域
            // view 屏幕位置 * 缩放比例 - sourceBitmap 偏移 = sourceBitmap 中的坐标
            val scale = sourceBitmapScale
            val srcLeft = ((curX - sourceBitmapOffsetX) * scale).toInt().coerceAtLeast(0)
            val srcTop = ((curY - sourceBitmapOffsetY) * scale).toInt().coerceAtLeast(0)
            val srcRight = (srcLeft + (curW * scale).toInt()).coerceAtMost(src.width)
            val srcBottom = (srcTop + (curH * scale).toInt()).coerceAtMost(src.height)

            val captureW = srcRight - srcLeft
            val captureH = srcBottom - srcTop

            if (captureW <= 0 || captureH <= 0) {
                mIsCapturing = false
                registerNext()
                return
            }

            val capture = acquireCapture(captureW, captureH)

            // 使用预分配 Canvas 进行截取
            mCaptureCanvas?.let { canvas ->
                canvas.setBitmap(capture)
                // 从 sourceBitmap 的对应区域截取
                mSrcRect.set(srcLeft, srcTop, srcRight, srcBottom)
                mDstRect.set(0, 0, captureW, captureH)
                canvas.drawBitmap(src, mSrcRect, mDstRect, mPaint)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // ── HW 路径：截取内容 → RenderNode + RenderEffect ──
                postToHwRenderNode(capture, curW, curH)
                // 同时保留 mDisplayBitmap 供非模糊场景回退
                mDisplayBitmap = capture
                blurredDrawable = capture.toDrawable(resources)
            } else {
                // ── SW 路径：直接显示，不模糊 ──
                mDisplayBitmap = capture
                blurredDrawable = capture.toDrawable(resources)
            }

            // 记录截取状态
            mLastCaptureX = curX
            mLastCaptureY = curY
            mLastCaptureW = curW
            mLastCaptureH = curH
            mSourceBitmapCaptured = true

            invalidate()
        } finally {
            mIsCapturing = false
            frameEnd()
            registerNext()
        }
    }

    /**
     * 统一截取入口
     */
    fun captureAndBlur() {
        surfaceView?.let { captureAndBlurFromSurfaceView() }
    }

    // ==================== HW 路径 (API 31+) ====================

    /**
     * 将截取的 Bitmap 提交到 HW RenderNode，应用 RenderEffect 模糊。
     *
     * 仅在 API 31+ 调用。RenderNode 内容持久化在 GPU，
     * dispatchDraw 时通过 canvas.drawRenderNode() 直接合成，
     * 无需协程、无需缩放、无需 CPU 模糊。
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun postToHwRenderNode(capture: Bitmap, viewW: Int, viewH: Int) {
        if (capture.isRecycled || viewW <= 0 || viewH <= 0) return

        // 尺寸变化时重建 RenderNode
        if (mHwBlurNode == null || mHwNodeW != viewW || mHwNodeH != viewH) {
            mHwBlurNode?.discardDisplayList()
            mHwBlurNode = RenderNode("BlurNode").apply {
                setPosition(0, 0, viewW, viewH)
            }
            mHwNodeW = viewW
            mHwNodeH = viewH
            mHwBlurRadius = 0f  // 强制刷新 effect
        }

        val node = mHwBlurNode ?: return

        // 绘制截取内容到 RenderNode
        val rCanvas: RecordingCanvas = node.beginRecording(viewW, viewH)
        mSrcRect.set(0, 0, capture.width, capture.height)
        mDstRect.set(0, 0, viewW, viewH)
        rCanvas.drawBitmap(capture, mSrcRect, mDstRect, mPaint)
        node.endRecording()

        // 应用/更新模糊效果
        val radius = blurRadius.coerceIn(0.1f, 25f)
        if (radius != mHwBlurRadius) {
            node.setRenderEffect(
                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            )
            mHwBlurRadius = radius
        }

        invalidate()
    }

    /** 更新 HW RenderNode 的模糊半径（blurRadius 属性变更时调用） */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun updateHwBlurRadius() {
        val node = mHwBlurNode ?: return
        val radius = blurRadius.coerceIn(0.1f, 25f)
        if (radius != mHwBlurRadius) {
            node.setRenderEffect(
                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            )
            mHwBlurRadius = radius
            invalidate()
        }
    }

    /** 丢弃 HW RenderNode（尺寸变化时调用，dispatchDraw 自动退回到 SW 路径） */
    private fun discardHwNode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mHwBlurNode?.discardDisplayList()
            mHwBlurNode = null
            mHwNodeW = 0
            mHwNodeH = 0
            mHwBlurRadius = 0f
        }
    }

    /**
     * 启动模糊消费者协程
     */
    private fun startBlurConsumer() {
        mBlurJob?.cancel()
        mBlurJob = mScope.launch(Dispatchers.IO) {
            mFrameChannel.consumeEach { task ->
                if (!isActive) return@consumeEach

                try {
                    // 帧丢弃检查
                    if (task.frameId != mFrameId.get()) {
                        withContext(Dispatchers.Main) {
                            mIsProcessing.set(false)
                            mIsCapturing = false
                            registerNext()
                        }
                        return@consumeEach
                    }

                    // 计算缩放尺寸
                    val sw = (task.bitmap.width * blurScaleFactor).toInt().coerceAtLeast(1)
                    val sh = (task.bitmap.height * blurScaleFactor).toInt().coerceAtLeast(1)

                    // 缩放
                    val scaled = acquireScaled(sw, sh)
                    mScaleCanvas?.let { canvas ->
                        mSrcRect.set(0, 0, task.bitmap.width, task.bitmap.height)
                        mDstRect.set(0, 0, sw, sh)
                        canvas.drawBitmap(task.bitmap, mSrcRect, mDstRect, mScalePaint)
                    }

                    // 模糊
                    val back = acquireBack(sw, sh)
                    val success = blur(scaled, back)

                    // 切回主线程
                    withContext(Dispatchers.Main) {
                        try {
                            if (task.frameId == mFrameId.get() && task.viewWidth > 0 && task.viewHeight > 0 && success) {
                                mDisplayBitmap = back
                                blurredDrawable = back.toDrawable(resources)
                                invalidate()
                            }
                        } finally {
                            mIsProcessing.set(false)
                            mIsCapturing = false
                            frameEnd()
                            registerNext()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        mIsProcessing.set(false)
                        mIsCapturing = false
                        registerNext()
                    }
                }
            }
        }
    }

    // ==================== 控制接口 ====================

    private inline fun registerNext() {
        if (autoUpdate && (surfaceView != null || sourceBitmap != null) && isAttachedToWindow && isShown) {
            mChoreographer.postFrameCallback(this)
        }
    }

    fun startRealtimeBlur() {
        if (blurEnabled == true) {
            autoUpdate = true
            mVSyncCounter = 0
            mFrameIdx = 0
            mAdaptiveSkipCount = 1
            mChoreographer.removeFrameCallback(this)
            mChoreographer.postFrameCallback(this)
            startBlurConsumer()
            if (debugMode) Log.d(TAG, "startRealtimeBlur")
        }
    }

    fun stopRealtimeBlur() {
        if (blurEnabled == true) {
            autoUpdate = false
            mChoreographer.removeFrameCallback(this)
            mBlurJob?.cancel()
            mBlurJob = null
            if (debugMode) Log.d(TAG, "stopRealtimeBlur")
        }
    }

    // ==================== 可见性 ====================

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE) registerNext()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == View.VISIBLE) registerNext()
    }

    // ==================== 内存压力 ====================

    fun onTrimMemory(level: Int) {
        when {
            level >= 40 -> { stopRealtimeBlur(); recycleAll() }
            level >= 20 -> recycleAll()
            level >= 10 -> { mCaptureBitmap?.recycle(); mCaptureBitmap = null }
        }
    }

    private fun recycleAll() {
        mCaptureBitmap?.recycle(); mCaptureBitmap = null
        mScaledBitmap?.recycle(); mScaledBitmap = null; mScaleCanvas = null
        mBackBuffer?.recycle(); mBackBuffer = null
        mDisplayBitmap?.recycle(); mDisplayBitmap = null
        discardHwNode()
    }

    // ==================== 生命周期 ====================

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopRealtimeBlur()
        mScope.cancel()
        mFrameChannel.close()
        recycleAll()
        rsLock.withLock {
            mBlur?.destroy(); mBlur = null
            mAllocIn?.destroy(); mAllocIn = null
            mAllocOut?.destroy(); mAllocOut = null
            mRS?.destroy(); mRS = null
        }
        if (debugMode) Log.d(TAG, "onDetachedFromWindow")
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!autoUpdate || (surfaceView == null && sourceBitmap == null) || !isAttachedToWindow || !isShown) {
            if (autoUpdate) mChoreographer.postFrameCallback(this)
            return
        }

        if (mIsCapturing) {
            mChoreographer.postFrameCallback(this)
            return
        }

        val skip = if (adaptiveFrameRate) mAdaptiveSkipCount else frameSkipCount
        if (++mVSyncCounter >= skip) {
            mVSyncCounter = 0
            captureAndBlur()
        } else {
            mChoreographer.postFrameCallback(this)
        }
    }

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.FrostedAnimatedGlowView) {
                blurEnabled = getBoolean(R.styleable.FrostedAnimatedGlowView_fagv_blurEnabled, false)
                blurRadius = getFloat(R.styleable.FrostedAnimatedGlowView_fagv_blurRadius, 15f)
                blurScaleFactor = getFloat(R.styleable.FrostedAnimatedGlowView_fagv_blurScaleFactor, 15f)
                blurredDrawable = getDrawable(R.styleable.FrostedAnimatedGlowView_fagv_blurredDrawable)
            }
        }
    }
}
