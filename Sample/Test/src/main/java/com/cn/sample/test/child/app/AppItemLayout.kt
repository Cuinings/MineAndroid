package com.cn.sample.test.child.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.cn.sample.test.R

/**
 * @Author: CuiNing
 * @Time: 2025/12/2 11:13
 * @Description:
 */
class AppItemLayout: LinearLayout {

    private val focusScaleDefault = 1.0f
    private var mScaleX = 1.15f
    private var mScaleY = 1.15f

    private val paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#E5FFFFFF".toColorInt()
        isAntiAlias = true
    } }

    private val path = Path()

    private val rect = RectF()

    private val cornerFloat by lazy { floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius) }

    var cornerRadius: Float = 0f

    var nameSize: Float = 24f

    private var iconView: ImageView? = null
    private var nameView: TextView? = null

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    @SuppressLint("NewApi", "CustomViewStyleable", "UseKtx")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        outlineSpotShadowColor = Color.GRAY
        orientation = VERTICAL
        gravity = Gravity.CENTER
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), Color.TRANSPARENT.toDrawable())
            addState(intArrayOf(android.R.attr.state_pressed), Color.TRANSPARENT.toDrawable())
            addState(intArrayOf(), Color.BLACK.toDrawable())
        }
        ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setImageResource(R.drawable.ic_launcher_foreground)
        }.let {
            iconView = it
            addView(it)
        }
        TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, nameSize)
            setTextColor(Color.BLACK)
            text = "123123123"
        }.let {
            nameView = it
            addView(it)
        }
    }

    fun setName(@StringRes value: Int) {
        nameView?.setText(value)
    }

    fun setName(value: String) {
        nameView?.text = value
    }

    fun setIcon(@DrawableRes value: Int) {
        iconView?.setImageResource(value)
    }

    fun setIcon( value: Bitmap) {
        iconView?.setImageBitmap(value)
    }

    private var lastCornerType = CornerType.IDLE
        set(value) { value.takeIf { it != field }?.let {
            field = it
        } }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        elevation = if (gainFocus) context.resources.getDimension(R.dimen.dp4) else 0f
        if (gainFocus) {
            corner()
            translationZ = 10f
            elevation = 10f
            /*post {
                layoutParams = layoutParams.apply {
                    width = width * mScaleX.toInt()
                    height = height * mScaleX.toInt()
                }
            }*/
            scaleX = mScaleX
            scaleY = mScaleY
            // 通过设置pivot为视图中心，避免位置偏移
//            pivotX = width / 2f
//            pivotY = height / 2f
            // 对子视图进行反向缩放，但不改变位置
//            for (i in 0 until childCount) {
//                val child = getChildAt(i)
//                child?.pivotX = child.width / 2f
//                child?.pivotY = child.height / 2f
//                child?.scaleX = 1f / mScaleX
//                child?.scaleY = 1f / mScaleY
//            }
        } else {
            cornerType = lastCornerType
            translationZ = 0f
            elevation = 0f
            /*post {
                layoutParams = layoutParams.apply {
                    width = width / mScaleX.toInt()
                    height = height / mScaleX.toInt()
                }
            }*/
            scaleX = focusScaleDefault
            scaleY = focusScaleDefault
            // 子视图的正常缩放
//            for (i in 0 until childCount) {
//                val child = getChildAt(i)
//                child?.scaleX = 1f
//                child?.scaleY = 1f
//            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        // 设置中心点为变换中心
        pivotX = w / 2f
        pivotY = h / 2f
    }

    enum class CornerType {
        IDLE,
        NONE_CORNER,
        FOCUSED_CORNER,
        TOP_LEFT_CORNER,
        TOP_RIGHT_CORNER,
        BOTTOM_LEFT_CORNER,
        BOTTOM_RIGHT_CORNER,
    }

    private var cornerType = CornerType.IDLE
        set(value) { value.takeIf { it != field }?.let {
            lastCornerType = field
            field = it
            postInvalidate()
//            Log.d(TAG, "cornerType: ${it.name}")
        } }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.reset()
        path.addRoundRect(rect, cornerFloat.apply {
            when(cornerType) {
                CornerType.FOCUSED_CORNER -> cornerFloat.let {
                    it[0] = cornerRadius
                    it[1] = cornerRadius
                    it[2] = cornerRadius
                    it[3] = cornerRadius
                    it[4] = cornerRadius
                    it[5] = cornerRadius
                    it[6] = cornerRadius
                    it[7] = cornerRadius
                }
                CornerType.TOP_LEFT_CORNER -> cornerFloat.let {
                    it[0] = cornerRadius
                    it[1] = cornerRadius
                    it[2] = 0f
                    it[3] = 0f
                    it[4] = 0f
                    it[5] = 0f
                    it[6] = 0f
                    it[7] = 0f
                }
                CornerType.TOP_RIGHT_CORNER -> cornerFloat.let {
                    it[0] = 0f
                    it[1] = 0f
                    it[2] = cornerRadius
                    it[3] = cornerRadius
                    it[4] = 0f
                    it[5] = 0f
                    it[6] = 0f
                    it[7] = 0f
                }
                CornerType.BOTTOM_LEFT_CORNER -> cornerFloat.let {
                    it[0] = 0f
                    it[1] = 0f
                    it[2] = 0f
                    it[3] = 0f
                    it[4] = 0f
                    it[5] = 0f
                    it[6] = cornerRadius
                    it[7] = cornerRadius
                }
                CornerType.BOTTOM_RIGHT_CORNER -> cornerFloat.let {
                    it[0] = 0f
                    it[1] = 0f
                    it[2] = 0f
                    it[3] = 0f
                    it[4] = cornerRadius
                    it[5] = cornerRadius
                    it[6] = 0f
                    it[7] = 0f
                }
                else -> cornerFloat.let {
                    it[0] = 0f
                    it[1] = 0f
                    it[2] = 0f
                    it[3] = 0f
                    it[4] = 0f
                    it[5] = 0f
                    it[6] = 0f
                    it[7] = 0f
                }
            }
        }, Path.Direction.CW)
        canvas.clipPath(path)
        canvas.drawPath(path, paint.apply {
            color = if (hasFocus()) "#FFFFFF".toColorInt() else "#E5FFFFFF".toColorInt()
        })
        path.close()
    }

    fun topLeftCorner() {
        cornerType = CornerType.TOP_LEFT_CORNER
        if (hasFocus()) corner()
    }

    fun topRightCorner() {
        cornerType = CornerType.TOP_RIGHT_CORNER
        if (hasFocus()) corner()
    }

    fun bottomLeftCorner() {
        cornerType = CornerType.BOTTOM_LEFT_CORNER
        if (hasFocus()) corner()
    }

    fun bottomRightCorner() {
        cornerType = CornerType.BOTTOM_RIGHT_CORNER
        if (hasFocus()) corner()
    }

    fun corner() {
        cornerType = CornerType.FOCUSED_CORNER
    }

    fun noCorner() {
        cornerType = CornerType.NONE_CORNER
        if (hasFocus()) corner()
    }

    fun setIconWh(width: Int, height: Int) = post {
        iconView?.layoutParams = iconView?.layoutParams?.apply {
            this.width = width
            this.height = height
        }
    }

    companion object {
        val TAG = AppItemLayout::class.simpleName
    }
}