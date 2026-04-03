package com.cn.core.ui.view.frosted.glass

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import com.cn.core.ui.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.toColorInt

@SuppressLint("Recycle")
class WaterDropRefractionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_REFRACTION_INTENSITY = 0.3f
        private const val DEFAULT_WAVE_AMPLITUDE = 5f
        private const val DEFAULT_WAVE_SPEED = 1f
    }
    private val DEFAULT_DROP_COLOR = "#E0FFFFFF".toColorInt()
    private val DEFAULT_HIGHLIGHT_COLOR = "#80FFFFFF".toColorInt()
    private val DEFAULT_SHADOW_COLOR = "#40000000".toColorInt()

    private val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val refractionPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val innerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val causticsPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val dropPath = Path()
    private val shadowPath = Path()
    private val matrix = Matrix()
    private val dropRect = RectF()
    private val shadowRect = RectF()

    private var dropColor: Int = DEFAULT_DROP_COLOR
    private var highlightColor: Int = DEFAULT_HIGHLIGHT_COLOR
    private var shadowColor: Int = DEFAULT_SHADOW_COLOR
    private var refractionIntensity: Float = DEFAULT_REFRACTION_INTENSITY
    private var waveAmplitude: Float = DEFAULT_WAVE_AMPLITUDE
    private var waveSpeed: Float = DEFAULT_WAVE_SPEED

    private var dropRadius: Float = 0f
    private var dropCenterX: Float = 0f
    private var dropCenterY: Float = 0f
    private var shadowOffset: Float = 8f
    private var shadowBlur: Float = 15f

    private var waveAnimator: ValueAnimator? = null
    private var rippleAnimator: ValueAnimator? = null
    private var waveProgress: Float = 0f
    private var rippleProgress: Float = 0f
    private var rippleRadius: Float = 0f
    private var isRippling: Boolean = false

    private var backgroundBitmap: Bitmap? = null
    private var refractionBitmap: Bitmap? = null
    private var refractionCanvas: Canvas? = null
    private var refractionShader: BitmapShader? = null

    private var causticsPoints: FloatArray = FloatArray(0)
    private var causticsPhases: FloatArray = FloatArray(0)

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.WaterDropRefractionView, defStyleAttr) {
                dropColor = getColor(R.styleable.WaterDropRefractionView_dropColor, DEFAULT_DROP_COLOR)
                highlightColor = getColor(R.styleable.WaterDropRefractionView_highlightColor, DEFAULT_HIGHLIGHT_COLOR)
                shadowColor = getColor(R.styleable.WaterDropRefractionView_shadowColor, DEFAULT_SHADOW_COLOR)
                refractionIntensity = getFloat(R.styleable.WaterDropRefractionView_refractionIntensity, DEFAULT_REFRACTION_INTENSITY)
                waveAmplitude = getDimension(R.styleable.WaterDropRefractionView_waveAmplitude, DEFAULT_WAVE_AMPLITUDE)
                waveSpeed = getFloat(R.styleable.WaterDropRefractionView_waveSpeed, DEFAULT_WAVE_SPEED)
            }
        }

        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setupPaints()
        initCaustics()
    }

    private fun setupPaints() {
        dropPaint.style = Paint.Style.FILL
        dropPaint.color = dropColor

        highlightPaint.style = Paint.Style.FILL
        highlightPaint.color = highlightColor

        shadowPaint.style = Paint.Style.FILL
        shadowPaint.color = shadowColor

        refractionPaint.style = Paint.Style.FILL

        ripplePaint.style = Paint.Style.STROKE
        ripplePaint.strokeWidth = 2f
        ripplePaint.color = Color.parseColor("#40FFFFFF")

        innerGlowPaint.style = Paint.Style.FILL

        causticsPaint.style = Paint.Style.FILL
        causticsPaint.color = Color.parseColor("#30FFFFFF")
    }

    private fun initCaustics() {
        val count = 12
        causticsPoints = FloatArray(count * 2)
        causticsPhases = FloatArray(count)
        for (i in 0 until count) {
            causticsPhases[i] = (i * 30f)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        dropRadius = minOf(w, h) / 2f * 0.7f
        dropCenterX = w / 2f
        dropCenterY = h / 2f

        shadowOffset = dropRadius * 0.1f
        shadowBlur = dropRadius * 0.15f

        createRefractionBitmap()
        updateDropPath()
        startWaveAnimation()
    }

    private fun createRefractionBitmap() {
        refractionBitmap?.recycle()
        refractionBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        refractionCanvas = Canvas(refractionBitmap!!)
        refractionShader = BitmapShader(refractionBitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    private fun updateDropPath() {
        dropPath.reset()

        val waveOffset = sin(waveProgress * 2 * PI).toFloat() * waveAmplitude
        val dynamicRadius = dropRadius + waveOffset * 0.5f

        dropRect.set(
            dropCenterX - dynamicRadius,
            dropCenterY - dynamicRadius,
            dropCenterX + dynamicRadius,
            dropCenterY + dynamicRadius
        )

        drawWaterDropShape(dropPath, dropCenterX, dropCenterY, dynamicRadius, waveOffset)

        shadowRect.set(
            dropCenterX - dynamicRadius + shadowOffset,
            dropCenterY - dynamicRadius + shadowOffset,
            dropCenterX + dynamicRadius + shadowOffset,
            dropCenterY + dynamicRadius + shadowOffset
        )
    }

    private fun drawWaterDropShape(path: Path, cx: Float, cy: Float, radius: Float, waveOffset: Float) {
        val topY = cy - radius * 1.2f
        val controlOffset = radius * 0.8f

        path.moveTo(cx, topY)

        path.cubicTo(
            cx + radius + waveOffset, cy - radius * 0.3f,
            cx + radius * 0.9f, cy + radius * 0.8f,
            cx, cy + radius
        )

        path.cubicTo(
            cx - radius * 0.9f, cy + radius * 0.8f,
            cx - radius - waveOffset, cy - radius * 0.3f,
            cx, topY
        )

        path.close()
    }

    private fun startWaveAnimation() {
        waveAnimator?.cancel()
        waveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = (3000 / waveSpeed).toLong()
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                waveProgress = it.animatedValue as Float
                updateDropPath()
                ViewCompat.postInvalidateOnAnimation(this@WaterDropRefractionView)
            }
        }
        waveAnimator?.start()
    }

    fun startRipple() {
        rippleAnimator?.cancel()
        rippleProgress = 0f
        rippleRadius = dropRadius * 0.3f
        isRippling = true

        rippleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            addUpdateListener {
                rippleProgress = it.animatedValue as Float
                rippleRadius = dropRadius * (0.3f + rippleProgress * 0.7f)
                ripplePaint.alpha = ((1 - rippleProgress) * 100).toInt()
                ViewCompat.postInvalidateOnAnimation(this@WaterDropRefractionView)
            }
        }
        rippleAnimator?.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawShadow(canvas)
        drawRefractionEffect(canvas)
        drawDrop(canvas)
        drawHighlight(canvas)
        drawInnerGlow(canvas)
        drawCaustics(canvas)
        drawRipple(canvas)
    }

    private fun drawShadow(canvas: Canvas) {
        canvas.save()

        val shadowGradient = RadialGradient(
            dropCenterX + shadowOffset,
            dropCenterY + shadowOffset,
            dropRadius * 1.2f,
            intArrayOf(shadowColor, Color.TRANSPARENT),
            floatArrayOf(0.3f, 1f),
            Shader.TileMode.CLAMP
        )
        shadowPaint.shader = shadowGradient

        shadowPath.reset()
        drawWaterDropShape(shadowPath, dropCenterX + shadowOffset, dropCenterY + shadowOffset, dropRadius, 0f)

        canvas.drawPath(shadowPath, shadowPaint)
        canvas.restore()
    }

    private fun drawRefractionEffect(canvas: Canvas) {
        if (refractionShader == null || refractionBitmap == null) return

        refractionBitmap?.eraseColor(Color.TRANSPARENT)

        val refractionMatrix = Matrix()
        val scale = 1f + refractionIntensity * 0.2f * sin(waveProgress * 4 * PI).toFloat()
        refractionMatrix.setScale(scale, scale, dropCenterX, dropCenterY)

        refractionCanvas?.save()
        refractionCanvas?.concat(refractionMatrix)

        val centerX = dropCenterX + sin(waveProgress * 2 * PI).toFloat() * waveAmplitude * refractionIntensity
        val centerY = dropCenterY + cos(waveProgress * 2 * PI).toFloat() * waveAmplitude * refractionIntensity

        val gradient = RadialGradient(
            centerX, centerY, dropRadius * 1.5f,
            intArrayOf(
                Color.parseColor("#10FFFFFF"),
                Color.parseColor("#05FFFFFF"),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        refractionPaint.shader = gradient

        refractionCanvas?.drawPath(dropPath, refractionPaint)
        refractionCanvas?.restore()

        refractionShader?.setLocalMatrix(matrix)
        dropPaint.shader = refractionShader
    }

    private fun drawDrop(canvas: Canvas) {
        canvas.save()

        val dropGradient = RadialGradient(
            dropCenterX - dropRadius * 0.3f,
            dropCenterY - dropRadius * 0.3f,
            dropRadius * 1.5f,
            intArrayOf(
                dropColor,
                Color.parseColor("#C0FFFFFF"),
                Color.parseColor("#90FFFFFF")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        dropPaint.shader = dropGradient

        canvas.drawPath(dropPath, dropPaint)
        canvas.restore()
    }

    private fun drawHighlight(canvas: Canvas) {
        canvas.save()

        val highlightPath = Path()
        val highlightWidth = dropRadius * 0.4f
        val highlightHeight = dropRadius * 0.25f
        val highlightX = dropCenterX - dropRadius * 0.3f
        val highlightY = dropCenterY - dropRadius * 0.5f

        highlightPath.addOval(
            highlightX - highlightWidth / 2,
            highlightY - highlightHeight / 2,
            highlightX + highlightWidth / 2,
            highlightY + highlightHeight / 2,
            Path.Direction.CW
        )

        val highlightGradient = LinearGradient(
            highlightX, highlightY - highlightHeight / 2,
            highlightX, highlightY + highlightHeight / 2,
            intArrayOf(
                Color.parseColor("#90FFFFFF"),
                Color.parseColor("#20FFFFFF"),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        highlightPaint.shader = highlightGradient

        canvas.clipPath(dropPath)
        canvas.drawPath(highlightPath, highlightPaint)
        canvas.restore()

        drawSecondaryHighlight(canvas)
    }

    private fun drawSecondaryHighlight(canvas: Canvas) {
        canvas.save()

        val secondaryHighlightPath = Path()
        val secondaryWidth = dropRadius * 0.15f
        val secondaryHeight = dropRadius * 0.1f
        val secondaryX = dropCenterX + dropRadius * 0.2f
        val secondaryY = dropCenterY - dropRadius * 0.3f

        secondaryHighlightPath.addOval(
            secondaryX - secondaryWidth / 2,
            secondaryY - secondaryHeight / 2,
            secondaryX + secondaryWidth / 2,
            secondaryY + secondaryHeight / 2,
            Path.Direction.CW
        )

        val secondaryGradient = RadialGradient(
            secondaryX, secondaryY, secondaryWidth,
            intArrayOf(
                Color.parseColor("#60FFFFFF"),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = secondaryGradient
        }

        canvas.clipPath(dropPath)
        canvas.drawPath(secondaryHighlightPath, secondaryPaint)
        canvas.restore()
    }

    private fun drawInnerGlow(canvas: Canvas) {
        canvas.save()

        val glowGradient = RadialGradient(
            dropCenterX, dropCenterY, dropRadius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.parseColor("#15FFFFFF"),
                Color.parseColor("#25FFFFFF")
            ),
            floatArrayOf(0.6f, 0.85f, 1f),
            Shader.TileMode.CLAMP
        )
        innerGlowPaint.shader = glowGradient

        canvas.clipPath(dropPath)
        canvas.drawCircle(dropCenterX, dropCenterY, dropRadius, innerGlowPaint)
        canvas.restore()
    }

    private fun drawCaustics(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(dropPath)

        val count = causticsPoints.size / 2
        for (i in 0 until count) {
            val angle = (i * 30 + waveProgress * 360 + causticsPhases[i]) * PI / 180
            val distance = dropRadius * (0.3f + 0.3f * sin(waveProgress * 4 * PI + i).toFloat())
            val x = dropCenterX + cos(angle).toFloat() * distance
            val y = dropCenterY + sin(angle).toFloat() * distance

            val causticSize = dropRadius * 0.08f * (0.5f + 0.5f * sin(waveProgress * 6 * PI + i * 0.5f).toFloat())

            val causticGradient = RadialGradient(
                x, y, causticSize,
                intArrayOf(
                    Color.parseColor("#40FFFFFF"),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            causticsPaint.shader = causticGradient

            canvas.drawCircle(x, y, causticSize, causticsPaint)
        }

        canvas.restore()
    }

    private fun drawRipple(canvas: Canvas) {
        if (!isRippling || rippleProgress >= 1f) {
            isRippling = false
            return
        }

        canvas.save()
        canvas.drawCircle(dropCenterX, dropCenterY, rippleRadius, ripplePaint)
        canvas.restore()
    }

    fun setDropColor(color: Int) {
        this.dropColor = color
        dropPaint.color = color
        invalidate()
    }

    fun setHighlightColor(color: Int) {
        this.highlightColor = color
        highlightPaint.color = color
        invalidate()
    }

    fun setShadowColor(color: Int) {
        this.shadowColor = color
        shadowPaint.color = color
        invalidate()
    }

    fun setRefractionIntensity(intensity: Float) {
        this.refractionIntensity = intensity.coerceIn(0f, 1f)
        invalidate()
    }

    fun setWaveAmplitude(amplitude: Float) {
        this.waveAmplitude = amplitude
        updateDropPath()
        invalidate()
    }

    fun setWaveSpeed(speed: Float) {
        this.waveSpeed = speed.coerceIn(0.1f, 5f)
        waveAnimator?.duration = (3000 / this.waveSpeed).toLong()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        waveAnimator?.cancel()
        rippleAnimator?.cancel()
        refractionBitmap?.recycle()
        refractionBitmap = null
    }
}
