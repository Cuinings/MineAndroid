package com.cn.board.meet.home.blur

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.view.Window
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toDrawable
import java.util.function.Consumer

/**
 * @author: cn
 * @time: 2026/4/8 10:00
 * @history
 * @description:
 */
class CrossWindowBlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "CrossWindowBlurView"
    }

    // 模糊配置
    data class BlurConfig(
        var blurRadius: Int = 20,
        var cornerRadius: Float = 0f,
        var overlayColor: Int = Color.TRANSPARENT,
        var overlayAlpha: Float = 0.7f,
        var blurBehind: Boolean = true,
    )

    private var blurConfig = BlurConfig()
    private var blurEnabled = false

    // 窗口模糊监听器
    private var blurStateListener: Consumer<Boolean?>? = null

    // 用于捕捉模糊的Bitmap
    private var blurBitmap: Bitmap? = null
    private var blurCanvas: Canvas? = null

    // 形状路径
    private val blurPath = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 背景Drawable
    private var backgroundDrawable: Drawable? = null

    init {
        setupView()
        setupPaint()
    }

    private fun setupView() {
        // 设置View为透明
        setBackgroundColor(Color.TRANSPARENT)

        // 禁用硬件加速以获得更好的模糊效果
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }

        // 设置裁剪边界
        clipToOutline = true
    }

    private fun setupPaint() {
        paint.apply {
            isAntiAlias = true
            isFilterBitmap = true
            style = Paint.Style.FILL
        }
    }

    /**
     * 应用模糊效果
     */
    fun applyBlur(config: BlurConfig = BlurConfig()) {
        this.blurConfig = config

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyNativeBlur()
        } else {
            applyFallbackBlur()
        }

        blurEnabled = true
        invalidate()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyNativeBlur() {
        // Android 12+ 使用原生窗口模糊
        val window = (context as? Activity)?.window
        window?.let { w ->
            // 1. 启用窗口模糊
            w.setBackgroundBlurRadius(blurConfig.blurRadius)

            // 2. 设置窗口模糊区域
            w.attributes = w.attributes.apply {
                this.blurBehindRadius = blurConfig.blurRadius

                // 设置模糊掩码
                if (blurConfig.blurBehind) {
//                    this.blurBehindMask = createBlurMask(w)
                }
            }

            // 3. 监听模糊状态
            setupBlurStateListener(w)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createBlurMask(window: Window): Bitmap? {
        val view = this
        val width = view.width
        val height = view.height

        if (width <= 0 || height <= 0) return null

        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 绘制View的形状作为掩码
            val path = Path().apply {
                if (blurConfig.cornerRadius > 0) {
                    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                    addRoundRect(rect,
                        blurConfig.cornerRadius,
                        blurConfig.cornerRadius,
                        Path.Direction.CW
                    )
                } else {
                    addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
                }
            }

            val paint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }

            canvas.drawPath(path, paint)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupBlurStateListener(window: Window) {
        blurStateListener = Consumer { isEnabled: Boolean? ->
            if (isEnabled == false) {
                // 系统禁用了模糊，显示降级效果
                showFallbackEffect()
            } else {
                // 重新启用模糊
                window.setBackgroundBlurRadius(blurConfig.blurRadius)
            }
        }

        window.windowManager.addCrossWindowBlurEnabledListener(blurStateListener!!)
    }

    private fun applyFallbackBlur() {
        // Android 12以下版本的降级实现
        // 使用半透明遮罩模拟模糊效果
        backgroundDrawable = createFallbackBackground()
        background = backgroundDrawable

        // 设置圆角
        if (blurConfig.cornerRadius > 0) {
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(
                        0, 0,
                        view.width,
                        view.height,
                        blurConfig.cornerRadius
                    )
                }
            }
        }
    }

    private fun createFallbackBackground(): Drawable {
        val color = Color.argb(
            (blurConfig.overlayAlpha * 255).toInt(),
            Color.red(blurConfig.overlayColor),
            Color.green(blurConfig.overlayColor),
            Color.blue(blurConfig.overlayColor)
        )

        return if (blurConfig.cornerRadius > 0) {
            // 圆角Drawable
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = blurConfig.cornerRadius
                setColor(color)
            }
        } else {
            // 矩形Drawable
            color.toDrawable()
        }
    }

    private fun showFallbackEffect() {
        // 显示降级效果
        val overlayColor = when (blurConfig.blurRadius) {
            in 0..10 -> Color.argb(51, 255, 255, 255)  // 轻度模糊
            in 11..20 -> Color.argb(102, 255, 255, 255) // 中度模糊
            else -> Color.argb(153, 255, 255, 255)      // 重度模糊
        }

        background = overlayColor.toDrawable()
    }

    /**
     * 更新模糊半径
     */
    fun updateBlurRadius(radius: Int) {
        blurConfig = blurConfig.copy(blurRadius = radius)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context as? Activity)?.window?.setBackgroundBlurRadius(radius)
        } else {
            // 更新降级效果的透明度
            val alpha = when (radius) {
                in 0..10 -> 0.2f
                in 11..20 -> 0.4f
                else -> 0.6f
            }
            blurConfig = blurConfig.copy(overlayAlpha = alpha)
            applyFallbackBlur()
        }

        invalidate()
    }

    /**
     * 设置圆角半径
     */
    fun setCornerRadius(radius: Float) {
        blurConfig = blurConfig.copy(cornerRadius = radius)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            applyFallbackBlur()
        }

        invalidate()
    }

    /**
     * 设置叠加颜色
     */
    fun setOverlayColor(color: Int, alpha: Float = 0.7f) {
        blurConfig = blurConfig.copy(
            overlayColor = color,
            overlayAlpha = alpha
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            applyFallbackBlur()
        }

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 更新模糊路径
        updateBlurPath(w, h)

        // 重新应用模糊
        if (blurEnabled) {
            applyBlur(blurConfig)
        }
    }

    private fun updateBlurPath(width: Int, height: Int) {
        blurPath.reset()

        if (blurConfig.cornerRadius > 0) {
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            blurPath.addRoundRect(
                rect,
                blurConfig.cornerRadius,
                blurConfig.cornerRadius,
                Path.Direction.CW
            )
        } else {
            blurPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (blurEnabled && blurConfig.cornerRadius > 0) {
            // 裁剪圆角区域
            canvas.clipPath(blurPath)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        // 保存画布状态
        val saveCount = canvas.save()

        try {
            // 应用裁剪
            if (blurEnabled && blurConfig.cornerRadius > 0) {
                canvas.clipPath(blurPath)
            }

            // 绘制子View
            super.dispatchDraw(canvas)

            // 绘制模糊叠加层
            if (blurEnabled) {
                drawBlurOverlay(canvas)
            }
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    private fun drawBlurOverlay(canvas: Canvas) {
        if (blurConfig.overlayColor != Color.TRANSPARENT && blurConfig.overlayAlpha > 0) {
            val overlayColor = Color.argb(
                (blurConfig.overlayAlpha * 255).toInt(),
                Color.red(blurConfig.overlayColor),
                Color.green(blurConfig.overlayColor),
                Color.blue(blurConfig.overlayColor)
            )

            paint.color = overlayColor

            if (blurConfig.cornerRadius > 0) {
                canvas.drawPath(blurPath, paint)
            } else {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }
    }

    /**
     * 禁用模糊
     */
    fun disableBlur() {
        blurEnabled = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurStateListener?.let {
                (context as? Activity)?.window?.windowManager?.removeCrossWindowBlurEnabledListener(it)
            }

            // 清除窗口模糊
            (context as? Activity)?.window?.setBackgroundBlurRadius(0)
        }

        // 清除背景
        background = null
        invalidate()
    }

    /**
     * 启用模糊
     */
    fun enableBlur() {
        if (!blurEnabled) {
            applyBlur(blurConfig)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disableBlur()
        cleanupResources()
    }

    private fun cleanupResources() {
        blurBitmap?.recycle()
        blurBitmap = null
        blurCanvas = null
    }
}