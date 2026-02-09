package com.cn.core.resources.drawable.dsl

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.annotation.ColorInt
import kotlin.math.roundToInt

/**
 * @Author: CuiNing
 * @Time: 2024/6/5 15:53
 * @Description:
 */
class ShapeBuilder: DrawableBuilder {

    var width = 0f                                                              //宽
    var height = 0f                                                             //高
    var radius = 0f                                                             //圆角
        set(value) {
            if (value != field) {
                field = value
                corner(radius, radius, radius, radius)
            }
        }
    private var cornerRadii: FloatArray? = null                                 //指定圆角
    @ColorInt
    private var color = Color.TRANSPARENT                                       //背景色
    private var colors: IntArray? = null                                        //渐变背景色
    @ColorInt
    var strokeColor = Color.TRANSPARENT                                         //描边颜色
    var strokeWidth = 0f                                                        //描边宽度
    var angle = 0                                                               //渐变背景色角度 必须45的倍数
        set(value) {
            if (value != field) {
                require(value % 45 == 0) { "'angle' attribute to be a multiple of 45" }
                field = value
            }
        }
    var type = Type.RECTANGLE
        set(value) {
            if (value != field) {
                field = value
                shape = when (field) {
                    Type.RECTANGLE -> GradientDrawable.RECTANGLE
                    Type.OVAL -> GradientDrawable.OVAL
                    Type.LINE -> GradientDrawable.LINE
                }
            }
        }

    private var shape = GradientDrawable.RECTANGLE

    /**
     * 指定某个角圆角
     */
    fun corner(leftTop: Float, rightTop: Float, rightBottom: Float, leftBottom: Float) {
        cornerRadii = floatArrayOf(
            leftTop, leftTop,
            rightTop, rightTop,
            rightBottom, rightBottom,
            leftBottom, leftBottom)
    }

    fun solid(@ColorInt color: Int) {
        this@ShapeBuilder.color = color
    }

    fun colors(@ColorInt start: Int, @ColorInt center: Int? = null, @ColorInt end: Int) {
        colors = center?.let { intArrayOf(start, it, end) }?: intArrayOf(start, end)
    }

    /**
     * 生成drawable
     */

    override fun build(): GradientDrawable  = GradientDrawable().apply {
        orientation = when (angle % 360) {
            45  -> GradientDrawable.Orientation.BL_TR
            90  -> GradientDrawable.Orientation.BOTTOM_TOP
            135 -> GradientDrawable.Orientation.BR_TL
            180 -> GradientDrawable.Orientation.RIGHT_LEFT
            225 -> GradientDrawable.Orientation.TR_BL
            270 -> GradientDrawable.Orientation.TOP_BOTTOM
            315 -> GradientDrawable.Orientation.TL_BR
            0   -> GradientDrawable.Orientation.LEFT_RIGHT
            else -> GradientDrawable.Orientation.LEFT_RIGHT
        }
        this@ShapeBuilder.colors?.let { colors = it }?: setColor(this@ShapeBuilder.color)
        this@ShapeBuilder.cornerRadii?.let { cornerRadii = it }?:{
            cornerRadii = floatArrayOf(
                radius, radius,
                radius, radius,
                radius, radius,
                radius, radius
            )
        }
        setStroke(strokeWidth.roundToInt(), strokeColor)
        setSize(width.roundToInt(), height.roundToInt())
        shape = this@ShapeBuilder.shape
    }

    enum class Type {
        RECTANGLE,
        OVAL,
        LINE
    }

}