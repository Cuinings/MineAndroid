package com.cn.library.common.mic.energy.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.core.graphics.PathParser
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withSave
/**
 * @Author: CuiNing
 * @Time: 2025/8/12 15:12
 * @Description:EDU
 */
class MicEnergyView1: View {

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, ): super(context, attrs, defStyleAttr)

    // 状态常量
    companion object {
        val TAG = MicEnergyView1::class.simpleName
        private const val STATE_OFF_DEFAULT = 0
        private const val STATE_OFF_FOCUSED = 1
        private const val STATE_ON_DEFAULT = 2
        private const val STATE_ON_FOCUSED = 3
    }

    // 状态变量
    private var currentState = STATE_OFF_DEFAULT    //default STATE_OFF_DEFAULT
    private var bFocused = false                    //default false
        get() = if (isDuplicateParentStateEnabled) (parent as ViewGroup).hasFocus() else field
    private var energyEnabled = true                //default false
    private var energyLevel = 0f                    //float range 0.0 - 1.0

    private val scaleValue = 32f
    private val waringPathData = "M10.866,3.5L10.866,3.5A1,1 54.217,0 1,12.232 3.866L24.732,25.517A1,1 104.197,0 1,24.366 26.883L24.366,26.883A1,1 104.197,0 1,23 26.517L10.5,4.866A1,1 54.217,0 1,10.866 3.5z"
    private val offPathData = "M11.16,8.741C11.056,9.143 11,9.565 11,10L11,16C11,18.761 13.239,21 16,21C16.71,21 17.386,20.852 17.998,20.585L16.987,18.834C16.678,18.941 16.346,19 16,19C14.343,19 13,17.657 13,16L13,11.928L11.16,8.741ZM18.562,17.562C18.84,17.107 19,16.572 19,16L19,10C19,8.343 17.657,7 16,7C14.864,7 13.875,7.631 13.366,8.562L12.272,6.668C13.188,5.644 14.519,5 16,5C18.761,5 21,7.239 21,10L21,16C21,17.327 20.483,18.533 19.64,19.428L18.562,17.562ZM19.003,22.325C18.093,22.758 17.075,23 16,23C12.134,23 9,19.866 9,16C9,15.448 8.552,15 8,15C7.448,15 7,15.448 7,16C7,20.633 10.501,24.448 15.002,24.945C15,24.963 15,24.982 15,25L15,27C15,27.552 15.448,28 16,28C16.552,28 17,27.552 17,27L17,25C17,24.982 17,24.963 16.999,24.945C18.069,24.827 19.082,24.521 20.006,24.062L19.003,22.325ZM21.688,22.975L20.671,21.214C22.101,19.932 23,18.071 23,16C23,15.448 23.448,15 24,15C24.552,15 25,15.448 25,16C25,18.813 23.709,21.325 21.688,22.975Z"
    private val onPathData = "M9,16C9,19.866 12.134,23 16,23C19.866,23 23,19.866 23,16C23,15.448 23.448,15 24,15C24.552,15 25,15.448 25,16C25,20.633 21.499,24.448 16.999,24.945C17,24.963 17,24.982 17,25L17,27C17,27.552 16.552,28 16,28C15.448,28 15,27.552 15,27L15,25C15,24.982 15,24.963 15.002,24.945C10.501,24.448 7,20.633 7,16C7,15.448 7.448,15 8,15C8.552,15 9,15.448 9,16Z"
    private val onPathDataLevel = "M16,5L16,5A5,5 0,0 1,21 10L21,16A5,5 0,0 1,16 21L16,21A5,5 0,0 1,11 16L11,10A5,5 0,0 1,16 5z"
    // 路径对象
    private val warningPath = Path().apply {
        addPath(PathParser.createPathFromPathData(waringPathData).apply { this.fillType = Path.FillType.EVEN_ODD })
    }
    private val micOffDefaultPath = Path().apply {
        addPath(PathParser.createPathFromPathData(offPathData).apply { this.fillType = Path.FillType.EVEN_ODD })
    }
    private val micOffFocusedPath = Path().apply {
        addPath(PathParser.createPathFromPathData(offPathData))
    }
    private val micOnDefaultPath = Path().apply {
        addPath(PathParser.createPathFromPathData(onPathDataLevel).apply { this.fillType = Path.FillType.EVEN_ODD })
        addPath(PathParser.createPathFromPathData(onPathData))
    }
    private val micOnFocusedPath = Path().apply {
        addPath(PathParser.createPathFromPathData(onPathDataLevel).apply { this.fillType = Path.FillType.EVEN_ODD })
        addPath(PathParser.createPathFromPathData(onPathData))
    }
    private val energyBarPath = Path().apply {
        addPath(PathParser.createPathFromPathData(onPathDataLevel))
    }

    // 能量条边界
    private val energyBarBounds = RectF(17f, 7f, 31f, 31f)

    // 颜色定义
    private val warningColor = "#B32424".toColorInt()
    private val colorNormal = "#3D3D3D".toColorInt()
    private val colorFocused = "#FFFFFF".toColorInt()
    private val colorEnergy = "#1A9AEB".toColorInt()

    // 画笔
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val energyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorEnergy
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 创建缩放矩阵以适应View尺寸
        val scale = width.coerceAtMost(height) / scaleValue
        val matrix = Matrix().apply { setScale(scale, scale) }
        // 应用缩放变换到所有路径
        micOffDefaultPath.transform(matrix)
        warningPath.transform(matrix)
        micOffFocusedPath.transform(matrix)
        micOnDefaultPath.transform(matrix)
        micOnFocusedPath.transform(matrix)
        energyBarPath.transform(matrix)
        // 更新能量条边界
        energyBarPath.computeBounds(energyBarBounds, true)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 根据状态选择绘制内容
        when (currentState) {
            STATE_OFF_DEFAULT -> {
                // 关闭默认状态
                drawMicPath(canvas, micOffDefaultPath, colorNormal)
                drawWarning(canvas)
            }
            STATE_OFF_FOCUSED -> {
                // 关闭焦点状态
                drawMicPath(canvas, micOffFocusedPath, colorFocused)
                drawWarning(canvas)
            }
            STATE_ON_DEFAULT -> {
                // 开启默认状态
                drawMicPath(canvas, micOnDefaultPath, colorNormal)
                if (energyEnabled) drawEnergyBar(canvas)
            }
            STATE_ON_FOCUSED -> {
                // 开启焦点状态
                drawMicPath(canvas, micOnFocusedPath, colorFocused)
                if (energyEnabled) drawEnergyBar(canvas)
            }
        }
    }

    private fun drawMicPath(canvas: Canvas, path: Path, color: Int) {
        paint.color = color
        canvas.drawPath(path, paint)
    }

    private fun drawWarning(canvas: Canvas) {
        paint.color = warningColor
        canvas.drawPath(warningPath, paint)
    }

    private fun drawEnergyBar(canvas: Canvas) {
        // 保存当前画布状态
        canvas.withSave {
            // 计算能量条裁剪区域
            val clipTop = energyBarBounds.top + energyBarBounds.height() * (1 - energyLevel)
            clipRect(
                energyBarBounds.left,
                clipTop,
                energyBarBounds.right,
                energyBarBounds.bottom
            )
            // 绘制能量条
            drawPath(energyBarPath, energyPaint)
            // 恢复画布状态
        }
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        bFocused = gainFocus
        updateState()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun toggleActiveState() {
        currentState = when (currentState) {
            STATE_OFF_DEFAULT, STATE_OFF_FOCUSED -> STATE_ON_DEFAULT
            else -> STATE_OFF_DEFAULT
        }
    }

    /**
     * 是否开启能量波动
     * @param energy true 开启能量、 false 不开启能量
     */
    fun toggleEnergy(energy: Boolean) {
        post {
            energyEnabled = energy
            invalidate()
        }
    }

    private fun updateState() {
        currentState = when {
            currentState == STATE_OFF_DEFAULT && bFocused -> STATE_OFF_FOCUSED
            currentState == STATE_OFF_FOCUSED && !bFocused -> STATE_OFF_DEFAULT
            currentState == STATE_ON_DEFAULT && bFocused -> STATE_ON_FOCUSED
            currentState == STATE_ON_FOCUSED && !bFocused -> STATE_ON_DEFAULT
            else -> currentState
        }
        Log.d(TAG, "updateState: $currentState")
        invalidate()
    }

    /**
     * 设置麦克风激活状态
     * @param active true表示开启，false表示关闭
     */
    fun toggleActive(active: Boolean) {
        post {
            energyLevel = if (!active) 0f else energyLevel
            currentState = when (active) {
                true -> if (bFocused) STATE_ON_FOCUSED else STATE_ON_DEFAULT
                false -> if (bFocused) STATE_OFF_FOCUSED else STATE_OFF_DEFAULT
            }
            invalidate()
        }
    }

    /**
     * 设置能量级别
     * @param level 能量级别（0.0 - 1.0）
     */
    fun setEnergyLevel(@FloatRange(from = 0.00, to = 1.00)  level: Float) {
        post {
            energyLevel = level.coerceIn(0f, 1f)
            if (currentState == STATE_ON_DEFAULT || currentState == STATE_ON_FOCUSED) {
                invalidate()
            }
        }
    }
}