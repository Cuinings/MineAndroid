package com.cn.board.meet.home.blur

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import java.util.function.Consumer

/**
 * @author: cn
 * @time: 2026/4/8 10:09
 * @history
 * @description:跨窗口模糊背景的View， 仅对这个View的透明背景区域应用跨窗口模糊
 */
class CrossWindowBlurBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnBlurStateChangedListener {
        fun onBlurEnabled(isEnabled: Boolean)
        fun onBlurRadiusChanged(radius: Int)
    }

    // 配置参数
    data class Config(
        var blurRadius: Int = 20,
        var cornerRadius: Float = 0f,
        var overlayColor: Int = Color.WHITE,
        var overlayAlpha: Float = 0.6f
    ) {
        companion object {
            val LIGHT = Config(blurRadius = 10, overlayAlpha = 0.3f)
            val MEDIUM = Config(blurRadius = 20, overlayAlpha = 0.6f)
            val HEAVY = Config(blurRadius = 30, overlayAlpha = 0.8f)
        }
    }

    private var config = Config()
    private var isBlurEnabled = false

    // Android 12+ 监听器
    @RequiresApi(Build.VERSION_CODES.S)
    private val blurStateListener: Consumer<Boolean?> =  Consumer { isEnabled ->
        onSystemBlurStateChanged(isEnabled == true)
    }

    // 自定义监听器
    private var onBlurStateChangedListener: OnBlurStateChangedListener? = null

    // 绘制相关
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blurPath = Path()
    private var currentCornerRadius = 0f

    init {
        setupView()
        setupPaint()
    }

    private fun setupView() {
        // 确保View是透明的
        setBackgroundColor(Color.TRANSPARENT)

        // 禁用硬件加速以获得更好的模糊效果（如果需要）
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            setLayerType(LAYER_TYPE_SOFTWARE, paint)
        }

        // 初始状态
        isBlurEnabled = false
    }

    private fun setupPaint() {
        paint.apply {
            isAntiAlias = true
            isFilterBitmap = true
            style = Paint.Style.FILL
        }
    }

    /**
     * 启用跨窗口模糊
     */
    fun enableCrossWindowBlur(config: Config = Config()) {
        this.config = config
        isBlurEnabled = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            enableNativeCrossWindowBlur()
            enableFallbackBlur()
        } else {
            enableFallbackBlur()
        }

        invalidate()
        onBlurStateChangedListener?.onBlurEnabled(true)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun enableNativeCrossWindowBlur() {
        val activity = context as? android.app.Activity ?: return
        val window = activity.window

        try {
            // 1. 设置窗口背景模糊半径
            window.setBackgroundBlurRadius(config.blurRadius)

            // 2. 设置窗口模糊属性
            val layoutParams = window.attributes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 可能需要额外的设置
                layoutParams.blurBehindRadius = config.blurRadius
            }
            window.attributes = layoutParams

            // 3. 注册模糊状态监听器
            activity.windowManager.addCrossWindowBlurEnabledListener(
                activity.mainExecutor,
                blurStateListener
            )

        } catch (e: Exception) {
            e.printStackTrace()
            // 如果原生API失败，使用降级方案
            enableFallbackBlur()
        }
    }

    private fun enableFallbackBlur() {
        // 设置半透明背景模拟模糊效果
        val overlayColor = Color.argb(
            (config.overlayAlpha * 255).toInt(),
            Color.red(config.overlayColor),
            Color.green(config.overlayColor),
            Color.blue(config.overlayColor)
        )

        paint.color = overlayColor

        // 更新模糊路径
        updateBlurPath()

        invalidate()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun onSystemBlurStateChanged(isEnabled: Boolean) {
        if (isEnabled) {
            // 系统重新启用了模糊
            val activity = context as? android.app.Activity
            activity?.window?.setBackgroundBlurRadius(config.blurRadius)
        } else {
            // 系统禁用了模糊，显示降级效果
            showSystemBlurDisabledEffect()
        }

        onBlurStateChangedListener?.onBlurEnabled(isEnabled)
    }

    private fun showSystemBlurDisabledEffect() {
        // 系统禁用模糊时的降级效果
        val fallbackAlpha = config.overlayAlpha + 0.2f.coerceAtMost(1f)
        val fallbackColor = Color.argb(
            (fallbackAlpha * 255).toInt(),
            Color.red(config.overlayColor),
            Color.green(config.overlayColor),
            Color.blue(config.overlayColor)
        )

        paint.color = fallbackColor
        invalidate()
    }

    /**
     * 禁用跨窗口模糊
     */
    fun disableCrossWindowBlur() {
        isBlurEnabled = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val activity = context as? android.app.Activity
            activity?.let {
                // 移除监听器
                it.windowManager.removeCrossWindowBlurEnabledListener(blurStateListener)

                // 清除窗口模糊
                it.window.setBackgroundBlurRadius(0)
            }
        }

        // 清除背景
        setBackgroundColor(Color.TRANSPARENT)
        paint.color = Color.TRANSPARENT

        invalidate()
        onBlurStateChangedListener?.onBlurEnabled(false)
    }

    /**
     * 更新模糊半径
     */
    fun updateBlurRadius(radius: Int) {
        config = config.copy(blurRadius = radius)

        if (isBlurEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val activity = context as? android.app.Activity
                activity?.window?.setBackgroundBlurRadius(radius)
            } else {
                // 更新降级效果的透明度
                val alpha = when (radius) {
                    in 0..10 -> 0.3f
                    in 11..20 -> 0.6f
                    else -> 0.8f
                }
                config = config.copy(overlayAlpha = alpha)
                enableFallbackBlur()
            }

            onBlurStateChangedListener?.onBlurRadiusChanged(radius)
        }
    }

    /**
     * 设置圆角半径
     */
    fun setCornerRadius(radius: Float) {
        currentCornerRadius = radius
        config = config.copy(cornerRadius = radius)

        updateBlurPath()
        invalidate()
    }

    private fun updateBlurPath() {
        blurPath.reset()

        if (currentCornerRadius > 0) {
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            blurPath.addRoundRect(
                rect,
                currentCornerRadius,
                currentCornerRadius,
                Path.Direction.CW
            )
        } else {
            blurPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateBlurPath()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isBlurEnabled) {
            // 绘制模糊背景
            if (currentCornerRadius > 0) {
                canvas.drawPath(blurPath, paint)
            } else {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }
    }

    /**
     * 设置模糊状态变化监听器
     */
    fun setOnBlurStateChangedListener(listener: OnBlurStateChangedListener) {
        this.onBlurStateChangedListener = listener
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disableCrossWindowBlur()
    }

    companion object {
        /**
         * 检查是否支持跨窗口模糊
         */
        fun isCrossWindowBlurSupported(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return false
            }

            val activity = context as? android.app.Activity
            return activity?.windowManager?.isCrossWindowBlurEnabled ?: false
        }
    }
}