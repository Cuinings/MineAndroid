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
import android.renderscript.*
import android.graphics.Path
import android.view.ViewGroup
import androidx.core.graphics.scale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import kotlin.collections.iterator
import androidx.core.graphics.createBitmap
import kotlin.collections.filter
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.toSet
import kotlin.collections.toTypedArray
import kotlin.jvm.java
import kotlin.ranges.coerceAtLeast
import kotlin.ranges.until

/**
 * GlassBlurView 支持多个SurfaceView的毛玻璃效果视图
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

    // 添加圆角路径和画笔
    private var mRoundedCornerPath: Path? = null
    private var mCornerRadius = 20f // 默认圆角半径

    // 用于拼接的位图映射，键为区域ID，值为对应的模糊位图及位置信息
    private var mBlurredBitmapMap: MutableMap<Int, BlurredBitmapInfo> = mutableMapOf()

    // 保存最近一次调用showGlassBlurViews的参数
    private var lastActivity: Activity? = null

    // 添加标志位跟踪视图可见性
    private var mViewVisible = true
    
    // 实时检测相关的Handler
    private val DETECT_SURFACE_VIEWS = 2
    private var mDetectHandler: Handler? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        mPaint = Paint()
        // 初始化圆角相关绘制对象
        mRoundedCornerPath = Path()
        // 关闭硬件加速以确保 PorterDuffXfermode 正常工作
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        
        // 初始化检测Handler
        mDetectHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    DETECT_SURFACE_VIEWS -> {
                        detectSurfaceViews()
                        // 每100毫秒检测一次
                        sendEmptyMessageDelayed(DETECT_SURFACE_VIEWS, 100)
                    }
                }
            }
        }
    }
    
    fun showGlassBlurViews(activity: Activity) {
        lastActivity = activity
        // 启动实时检测
        mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
        mDetectHandler?.sendEmptyMessage(DETECT_SURFACE_VIEWS)
    }
    
    /**
     * 实时检测SurfaceViews
     */
    private fun detectSurfaceViews() {
        val activity = lastActivity ?: return
        val overlappingSurfaceViews = mutableListOf<SurfaceView>()
        // 遍历Activity中的所有SurfaceView
        activity.window.decorView.findAllSurfaceViews(overlappingSurfaceViews, this)
        // 调用原始的showGlassBlurViews方法处理找到的SurfaceView
        if (overlappingSurfaceViews.isNotEmpty()) {
            showGlassBlurViewsInternal(overlappingSurfaceViews.toTypedArray(), activity)
        } else {
            // 如果没有重叠的SurfaceView，清空模糊效果
            clearAllBlurEffects()
        }
    }
    
    /**
     * 递归查找与当前GlassBlurView有重叠的所有SurfaceView
     */
    private fun View.findAllSurfaceViews(list: MutableList<SurfaceView>, glassBlurView: GlassBlurView) {
        if (this is SurfaceView) {
            // 检查此SurfaceView是否与GlassBlurView有重叠
            val glassBlurLocation = IntArray(2)
            val surfaceLocation = IntArray(2)
            glassBlurView.getLocationOnScreen(glassBlurLocation)
            this.getLocationOnScreen(surfaceLocation)
            
            val glassBlurRight = glassBlurLocation[0] + glassBlurView.width
            val glassBlurBottom = glassBlurLocation[1] + glassBlurView.height
            val surfaceRight = surfaceLocation[0] + this.width
            val surfaceBottom = surfaceLocation[1] + this.height
            
            // 检查是否有重叠
            if (glassBlurLocation[0] < surfaceRight && glassBlurRight > surfaceLocation[0] &&
                glassBlurLocation[1] < surfaceBottom && glassBlurBottom > surfaceLocation[1]) {
                list.add(this)
            }
        }
        
        // 递归检查所有子视图
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                getChildAt(i).findAllSurfaceViews(list, glassBlurView)
            }
        }
    }
    
    /**
     * 内部显示玻璃模糊视图方法，避免重复触发检测
     */
    private fun showGlassBlurViewsInternal(
        surfaceViews: Array<SurfaceView>,
        context: Context
    ) {
        stopThreads = false
        
        // 获取当前已有的线程ID列表
        val existingThreadIds = mBlurThreads.map { it.surfaceId }.toSet()
        
        // 创建新的SurfaceView ID集合
        val currentSurfaceIds = surfaceViews.map { it.hashCode() }.toSet()
        
        // 移除不再相交的线程
        val threadsToRemove = mBlurThreads.filter { !currentSurfaceIds.contains(it.surfaceId) }
        for (thread in threadsToRemove) {
            thread.stopThread()
            thread.interrupt()
            mBlurThreads.remove(thread)
        }
        
        // 为每个新发现的SurfaceView创建模糊线程
        for (surfaceView in surfaceViews) {
            val surfaceId = surfaceView.hashCode()
            // 检查是否已经有处理该SurfaceView的线程
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
     * 清除所有模糊效果
     */
    private fun clearAllBlurEffects() {
        // 停止所有线程
        for (thread in mBlurThreads) {
            thread.stopThread()
            thread.interrupt()
        }
        mBlurThreads.clear()
        
        // 清空模糊位图映射
        for ((_, blurredBitmapInfo) in mBlurredBitmapMap) {
            blurredBitmapInfo.bitmap.recycle()
        }
        mBlurredBitmapMap.clear()
        postInvalidate()
    }

    /**
     * 转换位图为玻璃模糊效果
     *
     * @param context 上下文
     * @param originBitmap 原始位图
     * @param radius 模糊半径 (0 < radius <= 25)
     * @param id 区域ID
     * @param left 重叠区域左边距
     * @param top 重叠区域上边距
     * @param right 重叠区域右边距
     * @param bottom 重叠区域下边距
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
        if (mRenderScript == null) {
            try {
                mRenderScript = RenderScript.create(context)
                mBlurScript = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript))
            } catch (e: RSRuntimeException) {
                release()
                return
            }
        }

        // 缩放位图以提高性能
        val width = Math.max(1, (originBitmap.width * 0.4).toInt())
        val height = Math.max(1, (originBitmap.height * 0.4).toInt())

        var bitmapToBlur: Bitmap? = null
        var blurredBitmap: Bitmap? = null

        // 为每个区域维护独立的位图
        if (mBitmapToBlur == null || mBitmapToBlur?.width != width || mBitmapToBlur?.height != height) {
            bitmapToBlur = originBitmap.scale(width, height, false)
            blurredBitmap = Bitmap.createBitmap(bitmapToBlur)
        } else {
            bitmapToBlur = originBitmap.scale(width, height, false)
            blurredBitmap = Bitmap.createBitmap(bitmapToBlur)
        }

        try {
            mBlurScript?.setRadius(Math.min(radius, 25f))
            mBlurInput = Allocation.createFromBitmap(
                mRenderScript, bitmapToBlur,
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT
            )
            mBlurOutput = Allocation.createTyped(mRenderScript!!, mBlurInput!!.type)
            mBlurInput!!.copyFrom(bitmapToBlur)
            mBlurScript?.setInput(mBlurInput!!)
            mBlurScript?.forEach(mBlurOutput!!)
            mBlurOutput!!.copyTo(blurredBitmap)

            // 将处理后的位图及位置信息存入映射中
            val rect = Rect(left, top, right, bottom)
            mBlurredBitmapMap[id] = BlurredBitmapInfo(blurredBitmap, rect)

            // 触发重绘
            postInvalidate()
        } catch (e: Exception) {
        } finally {
            bitmapToBlur?.recycle()
        }
    }

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

        fun stopThread() {
            mStopThread = true
        }

        // 添加暂停和恢复方法
        fun pauseThread() {
            mStopThread = true
        }

        fun resumeThread() {
            mStopThread = false
        }

        private fun sendMessage(what: Int, obj: Any?) {
            val msg = Message.obtain()
            msg.obj = obj
            msg.what = what
            mHandler.sendMessage(msg)
        }

        @SuppressLint("NewApi")
        override fun run() {
            // 子线程中实时检测SurfaceView与GlassBlurView的相交情况
            while (!mStopThread && !stopThreads && !isInterrupted) {
                try {
                    // 检查视图是否可见，如果不可见则跳过处理
                    if (!mViewVisible) {
                        try {
                            sleep(100) // 短暂休眠后继续检查
                        } catch (e: InterruptedException) {
                            currentThread().interrupt()
                            break
                        }
                        continue
                    }

                    // 在子线程中获取SurfaceView和GlassBlurView在屏幕上的位置和尺寸
                    val surfaceViewLocation = IntArray(2)
                    val glassBlurViewLocation = IntArray(2)
                    
                    // 使用同步机制确保位置信息获取的准确性
                    val syncLatch = CountDownLatch(1)
                    mHandler.post {
                        try {
                            if (view.isAttachedToWindow) {
                                view.getLocationOnScreen(surfaceViewLocation)
                                glassBlurView.getLocationOnScreen(glassBlurViewLocation)
                            }
                        } finally {
                            syncLatch.countDown()
                        }
                    }
                    syncLatch.await()

                    val surfaceWidth = view.width
                    val surfaceHeight = view.height
                    
                    // 如果SurfaceView尺寸无效，则等待下次循环
                    if (surfaceWidth <= 0 || surfaceHeight <= 0) {
                        try {
                            sleep(100)
                        } catch (e: InterruptedException) {
                            currentThread().interrupt()
                            break
                        }
                        continue
                    }

                    // 获取GlassBlurView在屏幕上的位置和尺寸
                    val glassBlurWidth = glassBlurView.width
                    val glassBlurHeight = glassBlurView.height

                    // 如果GlassBlurView尺寸无效，则等待下次循环
                    if (glassBlurWidth <= 0 || glassBlurHeight <= 0) {
                        try {
                            sleep(100)
                        } catch (e: InterruptedException) {
                            currentThread().interrupt()
                            break
                        }
                        continue
                    }

                    // 计算重叠区域
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

                    // 检查是否有重叠区域，如果没有重叠则清除该区域的模糊效果
                    if (right <= left || bottom <= top) {
                        // 移除无重叠区域的模糊效果
                        mHandler.post {
                            mBlurredBitmapMap.remove(surfaceId)
                            glassBlurView.postInvalidate()
                        }
                        
                        try {
                            sleep(150)
                        } catch (e: InterruptedException) {
                            currentThread().interrupt()
                            break
                        }
                        continue
                    }

                    // 计算相对于SurfaceView的坐标
                    val relativeLeft = left - surfaceViewLocation[0]
                    val relativeTop = top - surfaceViewLocation[1]
                    val relativeRight = right - surfaceViewLocation[0]
                    val relativeBottom = bottom - surfaceViewLocation[1]

                    // 确保相对坐标不超过SurfaceView边界
                    val clampedLeft = Math.max(0, relativeLeft)
                    val clampedTop = Math.max(0, relativeTop)
                    val clampedRight = Math.min(surfaceWidth, relativeRight)
                    val clampedBottom = Math.min(surfaceHeight, relativeBottom)

                    // 检查裁剪后的区域是否有效
                    if (clampedRight <= clampedLeft || clampedBottom <= clampedTop) {
                        try {
                            sleep(150)
                        } catch (e: InterruptedException) {
                            currentThread().interrupt()
                            break
                        }
                        continue
                    }

                    val width = clampedRight - clampedLeft
                    val height = clampedBottom - clampedTop

                    // 创建或重用位图
                    if (mScreenBitmap == null || mScreenBitmap?.width != width || mScreenBitmap?.height != height) {
                        mScreenBitmap?.recycle()
                        mScreenBitmap = createBitmap(
                            1.coerceAtLeast(width),
                            1.coerceAtLeast(height)
                        )
                    }

                    val tempRect = Rect(clampedLeft, clampedTop, clampedRight, clampedBottom)

                    // 从SurfaceView获取像素数据
                    PixelCopy.request(
                        view, tempRect, mScreenBitmap!!,
                        PixelCopy.OnPixelCopyFinishedListener { copyResult ->

                            if (PixelCopy.SUCCESS == copyResult) {
                                // 获取SurfaceView内容后发送消息处理

                                // 转换为相对于GlassBlurView的坐标
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
                                // 如果复制失败，移除对应的模糊效果
                                mHandler.post {
                                    mBlurredBitmapMap.remove(surfaceId)
                                    glassBlurView.postInvalidate()
                                }
                            }
                        },
                        mHandler
                    )
                } catch (e: IllegalArgumentException) {
                    // Surface isn't valid
                    // 移除无效SurfaceView的模糊效果
                    mHandler.post {
                        mBlurredBitmapMap.remove(surfaceId)
                        glassBlurView.postInvalidate()
                    }
                } catch (e: Exception) {
                    // 其他异常处理
                }

                try {
                    // 控制获取SurfaceView内容的时间间隔
                    sleep(150)
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
            // 可见时重新启动检测
            val activity = lastActivity
            if (activity != null) {
                mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
                mDetectHandler?.sendEmptyMessage(DETECT_SURFACE_VIEWS)
            }
        } else {
            // 不可见时停止检测
            mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            // 可见时重新启动检测
            val activity = lastActivity
            if (activity != null) {
                mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
                mDetectHandler?.sendEmptyMessage(DETECT_SURFACE_VIEWS)
            }
        } else {
            // 不可见时停止检测
            mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 重新附加到窗口时尝试恢复
        val activity = lastActivity
        if (activity != null) {
            mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
            mDetectHandler?.sendEmptyMessage(DETECT_SURFACE_VIEWS)
        }
    }

    override fun onDetachedFromWindow() {
        // 停止检测
        mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
        hideGlassBlurView()
        super.onDetachedFromWindow()
    }

    override fun draw(canvas: Canvas) {
        drawBlurredBitmap(canvas, mBlurredBitmapMap, mOverlayColor)
        super.draw(canvas)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }

    private fun drawBlurredBitmap(
        canvas: Canvas,
        blurredBitmapMap: Map<Int, BlurredBitmapInfo>,
        overlayColor: Int
    ) {
        // 绘制所有区域的模糊位图
        for ((key, blurredBitmapInfo) in blurredBitmapMap) {
            val blurredBitmap = blurredBitmapInfo.bitmap
            val blurRect = blurredBitmapInfo.rect

            if (null != blurredBitmap) {
                mRectSrc.left = 0
                mRectSrc.top = 0
                mRectSrc.right = blurredBitmap.width
                mRectSrc.bottom = blurredBitmap.height

                // 使用实际的重叠区域坐标
                mRectDst.left = blurRect.left
                mRectDst.top = blurRect.top
                mRectDst.right = blurRect.right
                mRectDst.bottom = blurRect.bottom

                // 更新圆角路径
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

                // 保存画布状态
                val saveCount = canvas.save()

                // 先绘制圆角路径作为剪裁区域
                if (mRoundedCornerPath != null) {
                    canvas.clipPath(mRoundedCornerPath!!)
                }

                // 绘制模糊位图
                canvas.drawBitmap(blurredBitmap, mRectSrc, mRectDst, null)

                // 恢复画布状态
                canvas.restoreToCount(saveCount)
            }
        }
    }

    /**
     * 设置圆角半径
     *
     * @param radius 圆角半径
     */
    fun setCornerRadius(radius: Float) {
        mCornerRadius = radius
        postInvalidate()
    }

    private fun release() {
        mBitmapToBlur?.recycle()
        mBitmapToBlur = null

        mBlurredBitmap?.recycle()
        mBlurredBitmap = null

        // 回收所有区域的位图
        for ((_, blurredBitmapInfo) in mBlurredBitmapMap) {
            blurredBitmapInfo.bitmap.recycle()
        }
        mBlurredBitmapMap.clear()

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
    }

    /**
     * 隐藏玻璃模糊视图
     */
    fun hideGlassBlurView() {
        stopThreads = true
        for (thread in mBlurThreads) {
            thread.stopThread()
            thread.interrupt()
        }
        mBlurThreads.clear()
        mDetectHandler?.removeMessages(DETECT_SURFACE_VIEWS)
        release()
    }


    /**
     * 模糊位图及位置信息数据类
     */
    data class BlurredBitmapInfo(
        val bitmap: Bitmap,
        val rect: Rect
    )

    /**
     * 传递给处理线程的数据类
     */
    data class BlurData(
        val bitmap: Bitmap,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )
}