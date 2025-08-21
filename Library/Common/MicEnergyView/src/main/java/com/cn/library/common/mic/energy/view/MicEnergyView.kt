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
 * @Description:
 */
class MicEnergyView : View {

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, ): super(context, attrs, defStyleAttr)

    // 状态常量
    companion object {
        val TAG = MicEnergyView::class.simpleName
        private const val STATE_OFF_DEFAULT = 0
        private const val STATE_OFF_FOCUSED = 1
        private const val STATE_ON_DEFAULT = 2
        private const val STATE_ON_FOCUSED = 3
    }

    // 状态变量
    private var currentState = STATE_OFF_DEFAULT    //default STATE_OFF_DEFAULT
    private var bFocused = false                   //default false
        get() = if (isDuplicateParentStateEnabled) (parent as ViewGroup).hasFocus() else field
    private var energyEnabled = true                //default false
    private var energyLevel = 0f                    //float range 0.0 - 1.0

    private val waringPathData = "M37.0909 38.8782L17.4545 4.86703C17.4187 4.805 17.3772 4.74705 17.3299 4.6932C17.2827 4.63934 17.2307 4.59061 17.1738 4.54701C17.117 4.5034 17.0565 4.46576 16.9922 4.43408C16.928 4.4024 16.8613 4.37729 16.7921 4.35875C16.7229 4.34021 16.6526 4.3286 16.5811 4.32391C16.5096 4.31923 16.4384 4.32156 16.3673 4.33091C16.2963 4.34026 16.2269 4.35645 16.1591 4.37947C16.0913 4.40249 16.0263 4.43191 15.9643 4.46773C15.9023 4.50354 15.8443 4.54507 15.7905 4.5923C15.7366 4.63952 15.6879 4.69155 15.6443 4.74838C15.6007 4.80521 15.563 4.86574 15.5313 4.92999C15.4997 4.99423 15.4745 5.06095 15.456 5.13014C15.4375 5.19933 15.4259 5.26966 15.4212 5.34114C15.4165 5.41261 15.4188 5.48386 15.4282 5.55488C15.4375 5.62589 15.4537 5.69532 15.4767 5.76315C15.4998 5.83097 15.5292 5.89591 15.565 5.95794L35.2013 39.9691C35.2372 40.0312 35.2787 40.0891 35.3259 40.143C35.3732 40.1968 35.4252 40.2455 35.482 40.2892C35.5388 40.3328 35.5994 40.3704 35.6636 40.4021C35.7279 40.4338 35.7946 40.4589 35.8638 40.4774C35.9329 40.4959 36.0033 40.5075 36.0748 40.5123C36.1462 40.5169 36.2175 40.5146 36.2885 40.5053C36.3595 40.4959 36.4289 40.4797 36.4968 40.4567C36.5646 40.4337 36.6295 40.4042 36.6916 40.3684C36.7536 40.3326 36.8115 40.2911 36.8654 40.2439C36.9193 40.1966 36.968 40.1446 37.0116 40.0878C37.0552 40.0309 37.0928 39.9704 37.1245 39.9062C37.1562 39.8419 37.1813 39.7752 37.1998 39.706C37.2184 39.6368 37.23 39.5665 37.2347 39.495C37.2394 39.4236 37.237 39.3523 37.2277 39.2813C37.2183 39.2103 37.2021 39.1408 37.1791 39.073C37.1561 39.0052 37.1267 38.9402 37.0909 38.8782Z"
    private val offPathData = "M17.2371 12.1919C17.0804 12.7788 17.0005 13.3805 17.0005 14L17.0005 24C17.0005 24.93 17.1805 25.82 17.5305 26.68C17.8905 27.54 18.4005 28.3 19.0605 28.95C19.7105 29.61 20.4605 30.11 21.3205 30.47C22.1805 30.82 23.0705 31 24.0005 31C24.9305 31 25.8205 30.82 26.6805 30.47C26.9798 30.3447 27.2669 30.2013 27.5416 30.0397L26.5422 28.3087C26.3418 28.4268 26.1312 28.531 25.9105 28.62C25.3005 28.87 24.6605 29 24.0005 29C23.3405 29 22.7005 28.87 22.0905 28.62C21.4705 28.37 20.9305 28 20.4605 27.53C20.0005 27.07 19.6305 26.53 19.3805 25.91C19.1305 25.3 19.0005 24.66 19.0005 24L19.0005 15.2461L17.2371 12.1919ZM20.5844 10.3495L19.5665 8.58645C20.0927 8.15078 20.6788 7.79863 21.3205 7.53C22.1805 7.18 23.0705 7 24.0005 7C24.9305 7 25.8205 7.18 26.6805 7.53C27.5405 7.89 28.2905 8.39 28.9405 9.05C29.6005 9.7 30.1105 10.46 30.4705 11.32C30.8205 12.18 31.0005 13.07 31.0005 14L31.0005 24C31.0005 24.93 30.8205 25.82 30.4705 26.68C30.4074 26.8308 30.34 26.9782 30.2681 27.1222L28.9339 24.8113C28.9781 24.5448 29.0005 24.2741 29.0005 24L29.0005 14C29.0005 13.34 28.8705 12.7 28.6205 12.09C28.3705 11.47 28.0005 10.93 27.5405 10.47C27.0705 10 26.5305 9.63 25.9105 9.38C25.3005 9.13 24.6605 9 24.0005 9C23.3405 9 22.7005 9.13 22.0905 9.38C21.5209 9.60965 21.0189 9.94057 20.5844 10.3495ZM29.0433 32.6407C28.6567 32.8654 28.2524 33.0651 27.8305 33.24C26.6005 33.75 25.3305 34 24.0005 34C22.6705 34 21.4005 33.75 20.1705 33.24C18.9405 32.73 17.8605 32.01 16.9305 31.07C15.9905 30.14 15.2705 29.06 14.7605 27.83C14.2505 26.6 14.0005 25.33 14.0005 24C14.0005 23.93 13.9905 23.87 13.9805 23.8C13.9705 23.74 13.9505 23.68 13.9205 23.62C13.9005 23.56 13.8705 23.5 13.8305 23.44L13.7105 23.29L13.5605 23.17C13.5005 23.13 13.4405 23.1 13.3805 23.08C13.3205 23.05 13.2605 23.03 13.2005 23.02C13.1305 23.01 13.0705 23 13.0005 23C12.9305 23 12.8705 23.01 12.8005 23.02C12.7405 23.03 12.6805 23.05 12.6205 23.08C12.5605 23.1 12.5005 23.13 12.4405 23.17L12.2905 23.29L12.1705 23.44C12.1305 23.5 12.1005 23.56 12.0805 23.62C12.0505 23.68 12.0305 23.74 12.0205 23.8C12.0105 23.87 12.0005 23.93 12.0005 24C12.0005 25.59 12.3005 27.12 12.9105 28.6C13.5205 30.07 14.3905 31.36 15.5105 32.49C16.6405 33.61 17.9305 34.48 19.4005 35.09C20.5705 35.57 21.7705 35.86 23.0005 35.96L23.0005 40L18.0005 40C17.9305 40 17.8705 40.01 17.8005 40.02C17.7405 40.03 17.6805 40.05 17.6205 40.08C17.5605 40.1 17.5005 40.13 17.4405 40.17L17.2905 40.29L17.1705 40.44C17.1305 40.5 17.1005 40.56 17.0805 40.62C17.0505 40.68 17.0305 40.74 17.0205 40.8C17.0105 40.87 17.0005 40.93 17.0005 41C17.0005 41.07 17.0105 41.13 17.0205 41.2C17.0305 41.26 17.0505 41.32 17.0805 41.38C17.1005 41.44 17.1305 41.5 17.1705 41.56L17.2905 41.71L17.4405 41.83C17.5005 41.87 17.5605 41.9 17.6205 41.92C17.6805 41.95 17.7405 41.97 17.8005 41.98C17.8705 41.99 17.9305 42 18.0005 42L30.0005 42C30.0705 42 30.1305 41.99 30.2005 41.98C30.2605 41.97 30.3205 41.95 30.3805 41.92C30.4405 41.9 30.5005 41.87 30.5605 41.83L30.7105 41.71L30.8305 41.56C30.8705 41.5 30.9005 41.44 30.9205 41.38C30.9505 41.32 30.9705 41.26 30.9805 41.2C30.9905 41.13 31.0005 41.07 31.0005 41C31.0005 40.93 30.9905 40.87 30.9805 40.8C30.9705 40.74 30.9505 40.68 30.9205 40.62C30.9005 40.56 30.8705 40.5 30.8305 40.44L30.7105 40.29L30.5605 40.17C30.5005 40.13 30.4405 40.1 30.3805 40.08C30.3205 40.05 30.2605 40.03 30.2005 40.02C30.1305 40.01 30.0705 40 30.0005 40L25.0005 40L25.0005 35.96C26.2305 35.86 27.4305 35.57 28.6005 35.09C29.1025 34.8817 29.5835 34.6431 30.0443 34.3745L29.0433 32.6407ZM33.0318 31.9091L31.9627 30.0572C32.4809 29.3794 32.9059 28.637 33.2405 27.83C33.7505 26.6 34.0005 25.33 34.0005 24C34.0005 23.93 34.0105 23.87 34.0205 23.8C34.0305 23.74 34.0505 23.68 34.0805 23.62C34.1005 23.56 34.1305 23.5 34.1705 23.44C34.2005 23.39 34.2505 23.34 34.2905 23.29L34.4405 23.17C34.5005 23.13 34.5605 23.1 34.6205 23.08C34.6805 23.05 34.7405 23.03 34.8005 23.02C34.8705 23.01 34.9305 23 35.0005 23C35.0705 23 35.1305 23.01 35.2005 23.02C35.2605 23.03 35.3205 23.05 35.3805 23.08C35.4405 23.1 35.5005 23.13 35.5605 23.17L35.7105 23.29C35.7505 23.34 35.8005 23.39 35.8305 23.44C35.8705 23.5 35.9005 23.56 35.9205 23.62C35.9505 23.68 35.9705 23.74 35.9805 23.8C35.9905 23.87 36.0005 23.93 36.0005 24C36.0005 25.59 35.7005 27.12 35.0905 28.6C34.5826 29.824 33.8945 30.9231 33.0318 31.9091Z"
    private val onPathData = "M30.4705 26.68C30.1105 27.54 29.6105 28.29 28.9505 28.94C28.3005 29.6 27.5405 30.11 26.6805 30.47C25.8205 30.82 24.9305 31 24.0005 31C23.0705 31 22.1805 30.82 21.3205 30.47C20.4605 30.11 19.7105 29.61 19.0605 28.95C18.4005 28.3 17.8905 27.54 17.5305 26.68C17.1805 25.82 17.0005 24.93 17.0005 24L17.0005 14C17.0005 13.07 17.1805 12.18 17.5305 11.32C17.8905 10.46 18.3905 9.71 19.0505 9.06C19.7005 8.4 20.4605 7.89 21.3205 7.53C22.1805 7.18 23.0705 7 24.0005 7C24.9305 7 25.8205 7.18 26.6805 7.53C27.5405 7.89 28.2905 8.39 28.9405 9.05C29.6005 9.7 30.1105 10.46 30.4705 11.32C30.8205 12.18 31.0005 13.07 31.0005 14L31.0005 24C31.0005 24.93 30.8205 25.82 30.4705 26.68ZM35.8305 23.44C35.8705 23.5 35.9005 23.56 35.9205 23.62C35.9505 23.68 35.9705 23.74 35.9805 23.8C35.9905 23.87 36.0005 23.93 36.0005 24C36.0005 25.59 35.7005 27.12 35.0905 28.6C34.4805 30.07 33.6105 31.36 32.4905 32.49C31.3605 33.61 30.0705 34.48 28.6005 35.09C27.433 35.5712 26.2344 35.8595 24.9998 35.9598L24.9998 40.001L29.9996 40.001C30.0696 40.001 30.1296 40.011 30.1996 40.021C30.2596 40.031 30.3196 40.051 30.3796 40.081C30.4396 40.101 30.4996 40.131 30.5596 40.171L30.7096 40.291L30.8296 40.441C30.8696 40.501 30.8996 40.561 30.9196 40.621C30.9496 40.681 30.9696 40.741 30.9796 40.801C30.9896 40.871 30.9996 40.931 30.9996 41.001C30.9996 41.071 30.9896 41.131 30.9796 41.201C30.9696 41.261 30.9496 41.321 30.9196 41.381C30.8996 41.441 30.8696 41.501 30.8296 41.561L30.7096 41.711L30.5596 41.831C30.4996 41.871 30.4396 41.901 30.3796 41.921C30.3196 41.951 30.2596 41.971 30.1996 41.981C30.1296 41.991 30.0696 42.001 29.9996 42.001L17.9996 42.001C17.9296 42.001 17.8696 41.991 17.7996 41.981C17.7396 41.971 17.6796 41.951 17.6196 41.921C17.5596 41.901 17.4996 41.871 17.4396 41.831L17.2896 41.711L17.1696 41.561C17.1296 41.501 17.0996 41.441 17.0796 41.381C17.0496 41.321 17.0296 41.261 17.0196 41.201C17.0096 41.131 16.9996 41.071 16.9996 41.001C16.9996 40.931 17.0096 40.871 17.0196 40.801C17.0296 40.741 17.0496 40.681 17.0796 40.621C17.0996 40.561 17.1296 40.501 17.1696 40.441L17.2896 40.291L17.4396 40.171C17.4996 40.131 17.5596 40.101 17.6196 40.081C17.6796 40.051 17.7396 40.031 17.7996 40.021C17.8696 40.011 17.9296 40.001 17.9996 40.001L22.9998 40.001L22.9998 35.9597C21.7656 35.8593 20.5675 35.571 19.4005 35.09C17.9305 34.48 16.6405 33.61 15.5105 32.49C14.3905 31.36 13.5205 30.07 12.9105 28.6C12.3005 27.12 12.0005 25.59 12.0005 24C12.0005 23.93 12.0105 23.87 12.0205 23.8C12.0305 23.74 12.0505 23.68 12.0805 23.62C12.1005 23.56 12.1305 23.5 12.1705 23.44L12.2905 23.29L12.4405 23.17C12.5005 23.13 12.5605 23.1 12.6205 23.08C12.6805 23.05 12.7405 23.03 12.8005 23.02C12.8705 23.01 12.9305 23 13.0005 23C13.0705 23 13.1305 23.01 13.2005 23.02C13.2605 23.03 13.3205 23.05 13.3805 23.08C13.4405 23.1 13.5005 23.13 13.5605 23.17L13.7105 23.29L13.8305 23.44C13.8705 23.5 13.9005 23.56 13.9205 23.62C13.9505 23.68 13.9705 23.74 13.9805 23.8C13.9905 23.87 14.0005 23.93 14.0005 24C14.0005 25.33 14.2505 26.6 14.7605 27.83C15.2705 29.06 15.9905 30.14 16.9305 31.07C17.8605 32.01 18.9405 32.73 20.1705 33.24C21.4005 33.75 22.6705 34 24.0005 34C25.3305 34 26.6005 33.75 27.8305 33.24C29.0605 32.73 30.1405 32.01 31.0705 31.07C32.0105 30.14 32.7305 29.06 33.2405 27.83C33.7505 26.6 34.0005 25.33 34.0005 24C34.0005 23.93 34.0105 23.87 34.0205 23.8C34.0305 23.74 34.0505 23.68 34.0805 23.62C34.1005 23.56 34.1305 23.5 34.1705 23.44C34.2005 23.39 34.2505 23.34 34.2905 23.29L34.4405 23.17C34.5005 23.13 34.5605 23.1 34.6205 23.08C34.6805 23.05 34.7405 23.03 34.8005 23.02C34.8705 23.01 34.9305 23 35.0005 23C35.0705 23 35.1305 23.01 35.2005 23.02C35.2605 23.03 35.3205 23.05 35.3805 23.08C35.4405 23.1 35.5005 23.13 35.5605 23.17L35.7105 23.29C35.7505 23.34 35.8005 23.39 35.8305 23.44ZM23.9998 34.9997L23.9998 40.9997"
    private val onPathDataLevel = "M24,7L24,7A7,7 0,0 1,31 14L31,24A7,7 0,0 1,24 31L24,31A7,7 0,0 1,17 24L17,14A7,7 0,0 1,24 7z"

    // 路径对象
    private val warningPath = Path().apply {
        addPath(PathParser.createPathFromPathData(waringPathData).apply { this.fillType = Path.FillType.EVEN_ODD })
    }
    private val micOffDefaultPath = Path().apply {
        addPath(PathParser.createPathFromPathData(offPathData).apply { this.fillType = Path.FillType.EVEN_ODD })
    }
    private val micOffFocusedPath = Path().apply {
        addPath(PathParser.createPathFromPathData(offPathData).apply { this.fillType = Path.FillType.EVEN_ODD })
    }
    private val micOnDefaultPath = Path().apply {
        addPath(PathParser.createPathFromPathData(onPathData).apply { this.fillType = Path.FillType.EVEN_ODD })
    }
    private val micOnFocusedPath = Path().apply {
        addPath(PathParser.createPathFromPathData(onPathData).apply { this.fillType = Path.FillType.EVEN_ODD })
    }
    private val energyBarPath = Path().apply {
        addPath(PathParser.createPathFromPathData(onPathDataLevel).apply { this.fillType = Path.FillType.EVEN_ODD })
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
    init {
        isDuplicateParentStateEnabled = true
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        bFocused = gainFocus
        updateState()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 创建缩放矩阵以适应View尺寸
        val scale = width.coerceAtMost(height) / 48f
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