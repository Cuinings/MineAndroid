package com.cn.core.ui.view.microphone

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.PathParser
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withSave
import com.cn.core.ui.R

/**
 * @Author: CuiNing
 * @Time: 2025/8/12 15:12
 * @Description:麦克风图标 开启关闭、能量波动（支持）
 */
class MicrophoneView: View {


    companion object {
        val TAG = MicrophoneView::class.simpleName
        private const val STATE_OFF_DEFAULT = 0
        private const val STATE_OFF_FOCUSED = 1
        private const val STATE_ON_DEFAULT = 2
        private const val STATE_ON_FOCUSED = 3
    }

    private var currentState = STATE_ON_DEFAULT
        set(value) { value.takeIf { it != field }?.let {
            field = it
        } }
    private var bFocused = false
        get() = if (isDuplicateParentStateEnabled) (parent as ViewGroup).hasFocus() else field
    private var energyEnabled = true
    private var energyLevel = 0.0f

    private var hasBackground = false

    var scaleValue = 32f
    var roundBackgroundPathData = "M16,16m-16,0a16,16 0,1 1,32 0a16,16 0,1 1,-32 0"
    var waringPathData = "M10.866,3.5L10.866,3.5A1,1 54.217,0 1,12.232 3.866L24.732,25.517A1,1 104.197,0 1,24.366 26.883L24.366,26.883A1,1 104.197,0 1,23 26.517L10.5,4.866A1,1 54.217,0 1,10.866 3.5z"
    var offPathData = "M11.16,8.741C11.056,9.143 11,9.565 11,10L11,16C11,18.761 13.239,21 16,21C16.71,21 17.386,20.852 17.998,20.585L16.987,18.834C16.678,18.941 16.346,19 16,19C14.343,19 13,17.657 13,16L13,11.928L11.16,8.741ZM18.562,17.562C18.84,17.107 19,16.572 19,16L19,10C19,8.343 17.657,7 16,7C14.864,7 13.875,7.631 13.366,8.562L12.272,6.668C13.188,5.644 14.519,5 16,5C18.761,5 21,7.239 21,10L21,16C21,17.327 20.483,18.533 19.64,19.428L18.562,17.562ZM19.003,22.325C18.093,22.758 17.075,23 16,23C12.134,23 9,19.866 9,16C9,15.448 8.552,15 8,15C7.448,15 7,15.448 7,16C7,20.633 10.501,24.448 15.002,24.945C15,24.963 15,24.982 15,25L15,27C15,27.552 15.448,28 16,28C16.552,28 17,27.552 17,27L17,25C17,24.982 17,24.963 16.999,24.945C18.069,24.827 19.082,24.521 20.006,24.062L19.003,22.325ZM21.688,22.975L20.671,21.214C22.101,19.932 23,18.071 23,16C23,15.448 23.448,15 24,15C24.552,15 25,15.448 25,16C25,18.813 23.709,21.325 21.688,22.975Z"
    var onPathData = "M9,16C9,19.866 12.134,23 16,23C19.866,23 23,19.866 23,16C23,15.448 23.448,15 24,15C24.552,15 25,15.448 25,16C25,20.633 21.499,24.448 16.999,24.945C17,24.963 17,24.982 17,25L17,27C17,27.552 16.552,28 16,28C15.448,28 15,27.552 15,27L15,25C15,24.982 15,24.963 15.002,24.945C10.501,24.448 7,20.633 7,16C7,15.448 7.448,15 8,15C8.552,15 9,15.448 9,16Z"
    var energyPathData = "M16,5L16,5A5,5 0,0 1,21 10L21,16A5,5 0,0 1,16 21L16,21A5,5 0,0 1,11 16L11,10A5,5 0,0 1,16 5z"

    var energyBarBounds = RectF(17f, 7f, 31f, 31f)

    @ColorInt var waringColor = "#B32424".toColorInt()
    @ColorInt var waringFocusColor = "#B32424".toColorInt()

    @ColorInt var micOnPathColor = "#3D3D3D".toColorInt()
    @ColorInt var micOnPathFocusColor = Color.WHITE

    @ColorInt var micOffPathColor = "#3D3D3D".toColorInt()
    @ColorInt var micOffPathFocusColor = Color.WHITE

    @ColorInt var energyNormalColor = "#4C7C7C7C".toColorInt()
    @ColorInt var energyNormalFocusColor = Color.WHITE

    @ColorInt var energyColor = "#FF1A9AEB".toColorInt()

    @ColorInt var roundBackgroundColor = "#33939393".toColorInt()
    @ColorInt var roundBackgroundFocusColor = Color.WHITE

    private lateinit var roundBackgroundPath: Path
    private lateinit var waringPath: Path
    private lateinit var micOffDefaultPath: Path
    private lateinit var micOffFocusedPath: Path
    private lateinit var micOnDefaultPath: Path
    private lateinit var micOnFocusedPath: Path
    private lateinit var energyBarPath: Path

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val energyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = energyColor
    }

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, ): super(context, attrs, defStyleAttr) {
        context.withStyledAttributes(attrs, R.styleable.MicrophoneView) {

            scaleValue = getFloat(R.styleable.MicrophoneView_scaleValue, scaleValue)

            waringColor = getColor(R.styleable.MicrophoneView_waringColor, waringColor)
            waringFocusColor = getColor(R.styleable.MicrophoneView_waringFocusColor, waringFocusColor)

            micOnPathColor = getColor(R.styleable.MicrophoneView_micOnColor, micOnPathColor)
            micOnPathFocusColor = getColor(R.styleable.MicrophoneView_micOnFocusColor, micOnPathFocusColor)

            micOffPathColor = getColor(R.styleable.MicrophoneView_micOffColor, micOffPathColor)
            micOffPathFocusColor = getColor(R.styleable.MicrophoneView_micOffFocusColor, micOffPathFocusColor)

            energyNormalColor = getColor(R.styleable.MicrophoneView_energyNormalColor, energyNormalColor)
            energyNormalFocusColor = getColor(R.styleable.MicrophoneView_energyNormalFocusColor, energyNormalFocusColor)

            energyColor = getColor(R.styleable.MicrophoneView_energyColor, energyColor)

            roundBackgroundColor = getColor(R.styleable.MicrophoneView_roundBackgroundColor, roundBackgroundColor)
            roundBackgroundFocusColor = getColor(R.styleable.MicrophoneView_roundBackgroundFocusColor, roundBackgroundFocusColor)

            waringPathData = getString(R.styleable.MicrophoneView_waringPath)?:waringPathData
            offPathData = getString(R.styleable.MicrophoneView_offPath)?:offPathData
            onPathData = getString(R.styleable.MicrophoneView_onPath)?:onPathData
            energyPathData = getString(R.styleable.MicrophoneView_energyPath)?:energyPathData

            energyBarBounds.run {
                left = getFloat(R.styleable.MicrophoneView_energyLeft, left)
                top = getFloat(R.styleable.MicrophoneView_energyTop, top)
                right = getFloat(R.styleable.MicrophoneView_energyRight, right)
                bottom = getFloat(R.styleable.MicrophoneView_energyBottom, bottom)
            }
        }
        initPath()
    }

    private fun initPath() {
        roundBackgroundPath = Path().apply {
            addPath(PathParser.createPathFromPathData(roundBackgroundPathData))
        }
        waringPath = Path().apply {
            addPath(
                PathParser.createPathFromPathData(waringPathData)
                    .apply { this.fillType = Path.FillType.EVEN_ODD })
        }
        micOffDefaultPath = Path().apply {
            addPath(
                PathParser.createPathFromPathData(offPathData)
                    .apply { this.fillType = Path.FillType.EVEN_ODD })
        }
        micOffFocusedPath = Path().apply {
            addPath(PathParser.createPathFromPathData(offPathData))
        }
        micOnDefaultPath = Path().apply {
            addPath(PathParser.createPathFromPathData(onPathData))
        }
        micOnFocusedPath = Path().apply {
            addPath(PathParser.createPathFromPathData(onPathData))
        }
        energyBarPath = Path().apply {
            addPath(PathParser.createPathFromPathData(energyPathData))
        }
    }

    private var mWidth = 0
    private var mHeight = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mWidth = MeasureSpec.makeMeasureSpec(widthMeasureSpec, MeasureSpec.UNSPECIFIED)
        mHeight = MeasureSpec.makeMeasureSpec(heightMeasureSpec, MeasureSpec.UNSPECIFIED)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val scale = width.coerceAtMost(height) / scaleValue
        val matrix = Matrix().apply { setScale(scale, scale) }
        roundBackgroundPath.transform(matrix)
        micOffDefaultPath.transform(matrix)
        waringPath.transform(matrix)
        micOffFocusedPath.transform(matrix)
        micOnDefaultPath.transform(matrix)
        micOnFocusedPath.transform(matrix)
        energyBarPath.transform(matrix)
        energyBarPath.computeBounds(energyBarBounds, true)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        bFocused = drawableState.contains(android.R.attr.state_focused)
        updateState()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawRoundBackground(canvas)
        drawMic(canvas)
        when {
            currentState == STATE_OFF_DEFAULT || currentState == STATE_OFF_FOCUSED -> drawWarning(canvas)
            energyEnabled -> drawEnergyBar(canvas)
        }
    }

    private fun drawMic(canvas: Canvas) {
        when(currentState) {
            STATE_OFF_DEFAULT, STATE_OFF_FOCUSED -> {
                paint.color = if (currentState == STATE_OFF_DEFAULT) micOffPathColor else micOffPathFocusColor
                canvas.withSave { drawPath(micOffDefaultPath, paint) }
            }
            STATE_ON_DEFAULT, STATE_ON_FOCUSED -> {
                canvas.withSave {
                    drawPath(energyBarPath, paint.apply {
                        color = if (currentState == STATE_ON_DEFAULT) energyNormalColor else energyNormalFocusColor
                    })
                }
                canvas.withSave { drawPath(micOnDefaultPath, paint.apply {
                    color = if (currentState == STATE_ON_DEFAULT) micOnPathColor else micOnPathFocusColor
                }) }
            }
            else -> micOffDefaultPath
        }
    }


    private fun drawRoundBackground(canvas: Canvas) {
        if (hasBackground) {
            backgroundPaint.color = if (bFocused) roundBackgroundFocusColor else roundBackgroundColor
            canvas.withSave {
                drawPath(roundBackgroundPath, backgroundPaint)
            }
        }
    }

    private fun drawMicPath(canvas: Canvas, path: Path, color: Int) {
        paint.color = color
        canvas.drawPath(path, paint)
    }

    private fun drawWarning(canvas: Canvas) {
        paint.color = waringColor
        canvas.drawPath(waringPath, paint)
    }

    private fun drawEnergyBar(canvas: Canvas) {
        canvas.withSave {
            val clipTop = energyBarBounds.top + energyBarBounds.height() * (1 - energyLevel)
            clipRect(
                energyBarBounds.left,
                clipTop,
                energyBarBounds.right,
                energyBarBounds.bottom
            )
            drawPath(energyBarPath, energyPaint)
        }
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        bFocused = gainFocus
        updateState()
    }

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
        invalidate()
    }

    fun toggleActive(active: Boolean) {
        post {
            currentState = when (active) {
                true -> if (bFocused) STATE_ON_FOCUSED else STATE_ON_DEFAULT
                false -> if (bFocused) STATE_OFF_FOCUSED else STATE_OFF_DEFAULT
            }
            invalidate()
        }
    }

    fun setEnergyLevel(@FloatRange(from = 0.00, to = 100.0)  level: Float) {
        post {
            energyLevel = (level / 100.00f).coerceIn(0f, 1f)
            if (currentState == STATE_ON_DEFAULT || currentState == STATE_ON_FOCUSED) {
                invalidate()
            }
        }
    }

    fun useBackground(use: Boolean) {
        post {
            hasBackground = use
            invalidate()
        }
    }
}