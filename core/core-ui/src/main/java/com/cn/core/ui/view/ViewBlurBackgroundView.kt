package com.cn.core.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlin.math.max
import kotlin.math.min

class ViewBlurBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mRenderScript: RenderScript? = null
    private var mBlurScript: ScriptIntrinsicBlur? = null
    private var mBlurInput: Allocation? = null
    private var mBlurOutput: Allocation? = null
    
    private var mBlurredBitmap: Bitmap? = null
    private var mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    
    private var mBlurRadius: Float = 15f
    private var mScaleFactor: Float = 0.25f
    private var mCornerRadius: Float = 0f
    private var mOverlayColor: Int = Color.TRANSPARENT
    
    private val mRectSrc = Rect()
    private val mRectDst = RectF()
    private var mRoundedPath: Path? = null
    
    private var mSourceView: View? = null
    private var mCaptureHandler: Handler? = null
    private var mIsCapturing = false
    private var mCaptureInterval: Long = 100L
    private var mAutoUpdate: Boolean = false
    
    private val MSG_CAPTURE = 1
    
    private var mClipRect: Rect? = null
    
    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        initHandler()
    }
    
    private fun initHandler() {
        mCaptureHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_CAPTURE -> {
                        if (mAutoUpdate && mSourceView != null) {
                            captureAndBlur()
                            sendEmptyMessageDelayed(MSG_CAPTURE, mCaptureInterval)
                        }
                    }
                }
            }
        }
    }
    
    fun setBlurRadius(radius: Float) {
        mBlurRadius = radius.coerceIn(0f, 25f)
        if (mSourceView != null) {
            captureAndBlur()
        }
    }
    
    fun setScaleFactor(factor: Float) {
        mScaleFactor = factor.coerceIn(0.1f, 1f)
        if (mSourceView != null) {
            captureAndBlur()
        }
    }
    
    fun setCornerRadius(radius: Float) {
        mCornerRadius = radius
        invalidate()
    }
    
    fun setOverlayColor(color: Int) {
        mOverlayColor = color
        invalidate()
    }
    
    fun setCaptureInterval(interval: Long) {
        mCaptureInterval = interval
    }
    
    fun setAutoUpdate(autoUpdate: Boolean) {
        mAutoUpdate = autoUpdate
        if (autoUpdate && mSourceView != null) {
            mCaptureHandler?.removeMessages(MSG_CAPTURE)
            mCaptureHandler?.sendEmptyMessage(MSG_CAPTURE)
        } else {
            mCaptureHandler?.removeMessages(MSG_CAPTURE)
        }
    }
    
    fun setSourceView(view: View?) {
        mSourceView = view
        if (view != null) {
            captureAndBlur()
        } else {
            clearBlur()
        }
    }
    
    fun setSourceViewWithClip(view: View?, clipLeft: Int, clipTop: Int, clipRight: Int, clipBottom: Int) {
        mSourceView = view
        mClipRect = Rect(clipLeft, clipTop, clipRight, clipBottom)
        if (view != null) {
            captureAndBlur()
        } else {
            clearBlur()
        }
    }
    
    fun setClipRect(clipLeft: Int, clipTop: Int, clipRight: Int, clipBottom: Int) {
        mClipRect = Rect(clipLeft, clipTop, clipRight, clipBottom)
        if (mSourceView != null) {
            captureAndBlur()
        }
    }
    
    fun clearClipRect() {
        mClipRect = null
        if (mSourceView != null) {
            captureAndBlur()
        }
    }
    
    fun captureAndBlur() {
        val sourceView = mSourceView ?: return
        
        if (mIsCapturing) return
        mIsCapturing = true
        
        if (sourceView.width <= 0 || sourceView.height <= 0) {
            mIsCapturing = false
            return
        }
        
        val targetView = findTargetView(sourceView)
        
        if (targetView is SurfaceView) {
            captureSurfaceView(targetView, sourceView)
        } else {
            captureNormalView(sourceView)
        }
    }
    
    private fun findTargetView(view: View): View {
        return view
    }
    
    @SuppressLint("NewApi")
    private fun captureSurfaceView(surfaceView: SurfaceView, sourceView: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            captureNormalView(sourceView)
            return
        }
        
        try {
            val clipRect = mClipRect
            val captureWidth: Int
            val captureHeight: Int
            
            if (clipRect != null) {
                captureWidth = max(1, clipRect.width())
                captureHeight = max(1, clipRect.height())
            } else {
                captureWidth = max(1, sourceView.width)
                captureHeight = max(1, sourceView.height)
            }
            
            val bitmap = createBitmap(captureWidth, captureHeight)
            
            val captureRect = if (clipRect != null) {
                Rect(clipRect)
            } else {
                Rect(0, 0, surfaceView.width, surfaceView.height)
            }
            
            PixelCopy.request(
                surfaceView,
                captureRect,
                bitmap,
                { copyResult ->
                    mIsCapturing = false
                    if (copyResult == PixelCopy.SUCCESS) {
                        processBitmap(bitmap, sourceView)
                    } else {
                        bitmap.recycle()
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            mIsCapturing = false
            captureNormalView(sourceView)
        }
    }
    
    private fun captureNormalView(view: View) {
        try {
            val clipRect = mClipRect
            val captureWidth: Int
            val captureHeight: Int
            
            if (clipRect != null) {
                captureWidth = max(1, clipRect.width())
                captureHeight = max(1, clipRect.height())
            } else {
                captureWidth = max(1, view.width)
                captureHeight = max(1, view.height)
            }
            
            val bitmap = createBitmap(captureWidth, captureHeight)
            val canvas = Canvas(bitmap)
            
            if (clipRect != null) {
                canvas.translate(-clipRect.left.toFloat(), -clipRect.top.toFloat())
            }
            
            view.draw(canvas)
            
            mIsCapturing = false
            processBitmap(bitmap, view)
        } catch (e: Exception) {
            mIsCapturing = false
        }
    }
    
    private fun processBitmap(sourceBitmap: Bitmap, sourceView: View) {
        if (width <= 0 || height <= 0) {
            sourceBitmap.recycle()
            return
        }
        
        val scaledWidth = max(1, (sourceBitmap.width * mScaleFactor).toInt())
        val scaledHeight = max(1, (sourceBitmap.height * mScaleFactor).toInt())
        
        val scaledBitmap = sourceBitmap.scale(scaledWidth, scaledHeight, false)
        sourceBitmap.recycle()
        
        val blurredBitmap = applyBlur(scaledBitmap)
        scaledBitmap.recycle()
        
        mBlurredBitmap?.recycle()
        mBlurredBitmap = blurredBitmap
        
        postInvalidate()
    }
    
    private fun applyBlur(bitmap: Bitmap): Bitmap {
        if (mBlurRadius <= 0f) {
            return bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }
        
        try {
            if (mRenderScript == null) {
                mRenderScript = RenderScript.create(context)
                mBlurScript = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript))
            }
            
            val input = Allocation.createFromBitmap(
                mRenderScript,
                bitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT
            )
            val output = Allocation.createTyped(mRenderScript!!, input.type)
            
            mBlurScript?.setRadius(mBlurRadius)
            mBlurScript?.setInput(input)
            mBlurScript?.forEach(output)
            
            val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
            output.copyTo(result)
            
            input.destroy()
            output.destroy()
            
            return result
        } catch (e: Exception) {
            return bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val blurredBitmap = mBlurredBitmap ?: return
        
        if (mCornerRadius > 0) {
            drawRoundedBlur(canvas, blurredBitmap)
        } else {
            drawNormalBlur(canvas, blurredBitmap)
        }
        
        if (mOverlayColor != Color.TRANSPARENT) {
            canvas.drawColor(mOverlayColor)
        }
    }
    
    private fun drawNormalBlur(canvas: Canvas, bitmap: Bitmap) {
        mRectSrc.set(0, 0, bitmap.width, bitmap.height)
        mRectDst.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(bitmap, mRectSrc, mRectDst, mPaint)
    }
    
    private fun drawRoundedBlur(canvas: Canvas, bitmap: Bitmap) {
        if (mRoundedPath == null) {
            mRoundedPath = Path()
        }
        
        mRoundedPath?.rewind()
        mRoundedPath?.addRoundRect(
            0f, 0f, width.toFloat(), height.toFloat(),
            mCornerRadius, mCornerRadius,
            Path.Direction.CW
        )
        
        val saveCount = canvas.save()
        canvas.clipPath(mRoundedPath!!)
        
        mRectSrc.set(0, 0, bitmap.width, bitmap.height)
        mRectDst.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(bitmap, mRectSrc, mRectDst, mPaint)
        
        canvas.restoreToCount(saveCount)
    }
    
    fun clearBlur() {
        mCaptureHandler?.removeMessages(MSG_CAPTURE)
        mBlurredBitmap?.recycle()
        mBlurredBitmap = null
        mSourceView = null
        mClipRect = null
        invalidate()
    }
    
    private fun release() {
        mCaptureHandler?.removeMessages(MSG_CAPTURE)
        
        mBlurredBitmap?.recycle()
        mBlurredBitmap = null
        
        try {
            mBlurInput?.destroy()
        } catch (e: Exception) {}
        mBlurInput = null
        
        try {
            mBlurOutput?.destroy()
        } catch (e: Exception) {}
        mBlurOutput = null
        
        try {
            mBlurScript?.destroy()
        } catch (e: Exception) {}
        mBlurScript = null
        
        try {
            mRenderScript?.destroy()
        } catch (e: Exception) {}
        mRenderScript = null
    }
    
    override fun onDetachedFromWindow() {
        release()
        super.onDetachedFromWindow()
    }
    
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE && mAutoUpdate && mSourceView != null) {
            mCaptureHandler?.removeMessages(MSG_CAPTURE)
            mCaptureHandler?.sendEmptyMessage(MSG_CAPTURE)
        } else {
            mCaptureHandler?.removeMessages(MSG_CAPTURE)
        }
    }
    
    companion object {
        fun captureViewArea(
            sourceView: View,
            blurView: ViewBlurBackgroundView,
            clipLeft: Int = 0,
            clipTop: Int = 0,
            clipRight: Int = sourceView.width,
            clipBottom: Int = sourceView.height
        ) {
            blurView.setSourceViewWithClip(sourceView, clipLeft, clipTop, clipRight, clipBottom)
        }
        
        fun findAndCaptureOverlappingViews(
            rootView: ViewGroup,
            blurView: ViewBlurBackgroundView,
            overlappingView: View
        ) {
            val blurLocation = IntArray(2)
            val targetLocation = IntArray(2)
            
            blurView.getLocationOnScreen(blurLocation)
            overlappingView.getLocationOnScreen(targetLocation)
            
            val blurLeft = blurLocation[0]
            val blurTop = blurLocation[1]
            val blurRight = blurLeft + blurView.width
            val blurBottom = blurTop + blurView.height
            
            val targetLeft = targetLocation[0]
            val targetTop = targetLocation[1]
            val targetRight = targetLeft + overlappingView.width
            val targetBottom = targetTop + overlappingView.height
            
            val overlapLeft = max(blurLeft, targetLeft)
            val overlapTop = max(blurTop, targetTop)
            val overlapRight = min(blurRight, targetRight)
            val overlapBottom = min(blurBottom, targetBottom)
            
            if (overlapRight > overlapLeft && overlapBottom > overlapTop) {
                val clipLeft = overlapLeft - targetLeft
                val clipTop = overlapTop - targetTop
                val clipRight = overlapRight - targetLeft
                val clipBottom = overlapBottom - targetTop
                
                blurView.setSourceViewWithClip(
                    overlappingView,
                    clipLeft, clipTop, clipRight, clipBottom
                )
            }
        }
    }
}
