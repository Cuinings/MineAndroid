package com.cn.core.ui.view.loading

/**
 * @Author: CuiNing
 * @Time: 2025/8/19 10:05
 * @Description:
 */


import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.PathParser
import androidx.core.graphics.toColorInt
import com.cn.core.ui.R

@SuppressLint("CustomViewStyleable")
class LoadingView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val path = Path().apply {
        addPath(PathParser.createPathFromPathData(
            "M40,73C58.225,73 73,58.225 73,40C73,21.775 58.225,7 40,7C21.775,7 7,21.775 7,40C7,58.225 21.775,73 40,73ZM40,67C54.912,67 67,54.912 67,40C67,25.088 54.912,13 40,13C25.088,13 13,25.088 13,40C13,54.912 25.088,67 40,67Z"
        ))
        fillType = Path.FillType.EVEN_ODD
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val matrix = Matrix()
    private var centerX = 0f
    private var centerY = 0f
    private var rotationAnimator: ValueAnimator? = null
    private var currentRotation = 0f

    // 渐变颜色定义
    private val colors = intArrayOf(
        "#0FFFFFFF".toColorInt(), // 起始色（透明白）
        "#FFFFFFFF".toColorInt()  // 结束色（纯白）
    )
    private lateinit var sweepGradient: SweepGradient

    init {
        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.LoadingView).apply {
                getColor(R.styleable.LoadingView_gradientRingStartColor, Color.BLACK).let {
                    colors[0] = it
                }
                getColor(R.styleable.LoadingView_gradientRingEndColor, Color.WHITE).let {
                    colors[1] = it
                }
                recycle()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 创建缩放矩阵以适应View尺寸
        val scale = width.coerceAtMost(height) / 80f
        val matrix = Matrix().apply { setScale(scale, scale) }
        // 应用缩放变换到所有路径
        path.transform(matrix)
        centerX = w / 2f
        centerY = h / 2f

        // 初始化渐变（以中心为锚点）
        sweepGradient = SweepGradient(centerX, centerY, colors, null)

        // 调整渐变起始角度（默认从3点钟方向开始）
        matrix.setRotate(-90f, centerX, centerY) // 旋转至顶部起始
        sweepGradient.setLocalMatrix(matrix)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        when (visibility) {
            VISIBLE -> startRotation()
            GONE, INVISIBLE -> stopRotation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.rotate(currentRotation, centerX, centerY)
        paint.shader = sweepGradient
        canvas.drawPath(path, paint)
    }

    private fun startRotation() {
        rotationAnimator?.cancel()
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2000
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { anim ->
                currentRotation = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopRotation() {
        rotationAnimator?.cancel()
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)

    }

    override fun onDetachedFromWindow() {
        stopRotation()
        super.onDetachedFromWindow()
    }
}