//package com.cn.core.ui.view.frosted.glass
//
//import android.animation.Animator
//import android.animation.ValueAnimator
//import android.annotation.SuppressLint
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.LinearGradient
//import android.graphics.Matrix
//import android.graphics.Paint
//import android.graphics.Path
//import android.graphics.PathMeasure
//import android.graphics.Rect
//import android.graphics.RectF
//import android.graphics.Shader
//import android.graphics.drawable.BitmapDrawable
//import android.graphics.drawable.Drawable
//import android.util.AttributeSet
//import android.view.ViewTreeObserver
//import android.view.animation.LinearInterpolator
//import androidx.constraintlayout.widget.ConstraintLayout
//import androidx.core.content.withStyledAttributes
//import androidx.core.graphics.toColorInt
//import androidx.core.view.ViewCompat
//import kotlin.math.max
//import kotlin.math.min
//import kotlin.math.sqrt
//import androidx.core.graphics.withSave
//import com.cn.core.ui.R
//import kotlin.math.abs
//
///**
// * 毛玻璃发光视图
// *
// * 结合了毛玻璃效果和内发光效果的自定义视图，支持流光边框和刷新效果动画。
// * 继承自 ConstraintLayout，保留了所有约束布局的特性。
// *
// * ## 特性
// * - 半透明背景（10%黑色透明度）
// * - 内发光效果（四边 + 四角）
// * - 顶部内阴影效果
// * - 内描边效果
// * - 流光边框动画（仅在有焦点时）
// * - 刷新效果动画（仅在有焦点时）
// * - 支持圆角矩形
// * - 支持单独设置每个角的圆角半径
// * - 支持 XML 属性配置
// * - 保留 ConstraintLayout 所有特性
// *
// * ## XML 属性
// * | 属性名 | 类型 | 默认值 | 说明 |
// * |--------|------|--------|------|
// * | cornerRadius | dimension | 16dp | 所有角的圆角半径 |
// * | topLeftRadius | dimension | 16dp | 左上角圆角半径 |
// * | topRightRadius | dimension | 16dp | 右上角圆角半径 |
// * | bottomLeftRadius | dimension | 16dp | 左下角圆角半径 |
// * | bottomRightRadius | dimension | 16dp | 右下角圆角半径 |
// * | glowColor | color | #0DFFFFFF | 内发光颜色 |
// * | glowRadius | dimension | 10dp | 内发光半径 |
// * | glowStrokeWidth | dimension | 1px | 内描边宽度 |
// * | glowStrokeColor | color | #1AFFFFFF | 内描边颜色 |
// * | innerShadowThickness | dimension | 0 | 顶部内阴影厚度 |
// * | innerShadowColor | color | #0DFFFFFF | 顶部内阴影颜色 |
// *
// * ## 使用示例
// * ```xml
// * <com.cn.core.ui.view.frosted.glass.FrostedGlowView
// *     android:layout_width="200dp"
// *     android:layout_height="200dp"
// *     android:focusable="true"
// *     android:focusableInTouchMode="true"
// *     app:cornerRadius="16dp"
// *     app:glowColor="#0DFFFFFF"
// *     app:glowRadius="10dp"
// *     app:glowStrokeWidth="1dp"
// *     app:glowStrokeColor="#1AFFFFFF"
// *     app:innerShadowThickness="20dp"
// *     app:innerShadowColor="#0DFFFFFF">
// *
// *     <!-- 子视图可以使用约束布局属性 -->
// *     <TextView
// *         android:layout_width="wrap_content"
// *         android:layout_height="wrap_content"
// *         android:text="Hello"
// *         app:layout_constraintBottom_toBottomOf="parent"
// *         app:layout_constraintEnd_toEndOf="parent"
// *         app:layout_constraintStart_toStartOf="parent"
// *         app:layout_constraintTop_toTopOf="parent" />
// *
// * </com.cn.core.ui.view.frosted.glass.FrostedGlowView>
// * ```
// *
// * ## 代码示例
// * ```kotlin
// * val frostedGlowView = FrostedGlowView(context).apply {
// *     setCornerRadius(16f)
// *     setGlowColor(Color.parseColor("#0DFFFFFF"))
// *     setGlowRadius(20f)
// *     setStrokeWidth(2f)
// *     setStrokeColor(Color.parseColor("#1AFFFFFF"))
// *     setInnerShadowThickness(30f)
// *     setInnerShadowColor(Color.parseColor("#0DFFFFFF"))
// *     isFocusable = true
// *     isFocusableInTouchMode = true
// * }
// *
// * // 请求焦点以显示流光效果
// * frostedGlowView.requestFocus()
// *
// * // 清除焦点以隐藏流光效果
// * frostedGlowView.clearFocus()
// * ```
// *
// * ## 实现原理
// * 1. 使用 SOFTWARE 图层类型确保渐变正确渲染
// * 2. 所有内部效果（背景、发光、阴影、刷新效果）都限制在边框内部
// * 3. 流光边框和刷新效果仅在视图获得焦点时显示
// * 4. 保留了 ConstraintLayout 的所有布局特性
// * 5. 使用 Path 和 Shader 实现各种视觉效果
// *
// * @constructor 创建毛玻璃发光视图实例
// * @param context 上下文
// * @param attrs XML 属性集
// * @param defStyleAttr 默认样式属性
// *
// * @see ConstraintLayout
// * @see LinearGradient
// * @see android.graphics.RadialGradient
// */
//@SuppressLint("Recycle")
//open class FrostedGlowView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0,
//) : ConstraintLayout(context, attrs, defStyleAttr) {
//
//    companion object {
//        /** 默认边框宽度 */
//        private const val DEFAULT_BORDER_WIDTH = 4f
//        /** 默认圆角半径 */
//        private const val DEFAULT_CORNER_RADIUS = 16f
//        /** 默认内发光半径（dp） */
//        private const val DEFAULT_GLOW_RADIUS_DP = 30f
//    }
//
//    /** 边框画笔 */
//    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
//    /** 刷新效果画笔 */
//    private val refreshPaint = Paint(Paint.ANTI_ALIAS_FLAG)
//    /** 流光效果画笔 */
//    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
//    /** 内发光画笔 */
//    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
//    /** 内描边画笔 */
//    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        style = Paint.Style.STROKE
//    }
//    /** 蒙层画笔 */
//    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
//
//    /** 边框绘制路径 */
//    private val borderPath = Path()
//    /** 裁剪路径 */
//    private val clipPath = Path()
//    /** 内发光绘制路径 */
//    private val glowPath = Path()
//    /** 内路径（用于内发光） */
//    private val innerPath = Path()
//    /** 段路径（用于流光效果） */
//    private val segmentPath = Path()
//    /** 内半径数组（缓存） */
//    private val innerRadius = FloatArray(8)
//    /** 路径缓存标志 */
//    private var borderPathDirty = true
//    private var clipPathDirty = true
//    private var glowPathDirty = true
//    private var innerPathDirty = true
//    /** 路径测量对象 */
//    private val pathMeasure = PathMeasure()
//    /** 缓存的路径总长度 */
//    private var cachedTotalLength = 0f
//    /** 缓存的光效长度 */
//    private var cachedGlowLength = 0f
//    /** 缓存的宽度和高度 */
//    private var cachedPathWidth = 0f
//    private var cachedPathHeight = 0f
//    /** 圆角半径数组 */
//    private val radii = FloatArray(8)
//    /** 矩阵对象（用于流光效果） */
//    private val matrix = Matrix()
//    /** 矩阵对象（用于刷新效果） */
//    private val refreshMatrix = Matrix()
//    /** 内发光矩形 */
//    private val glowRectF = RectF()
//
//    /** 边框渐变对象 */
//    private var borderGradient: LinearGradient? = null
//    /** 刷新效果渐变对象 */
//    private var refreshGradient: LinearGradient? = null
//    /** 顶部内发光渐变 */
//    private var topGlowGradient: LinearGradient? = null
//    /** 底部内发光渐变 */
//    private var bottomGlowGradient: LinearGradient? = null
//    /** 左侧内发光渐变 */
//    private var leftGlowGradient: LinearGradient? = null
//    /** 右侧内发光渐变 */
//    private var rightGlowGradient: LinearGradient? = null
//    /** 静态边框渐变 */
//    private var staticBorderGradient: LinearGradient? = null
//    /** 流动边框静态渐变 */
//    private var flowingStaticBorderGradient: LinearGradient? = null
//    /** 背景绘制区域路径 */
//    private val backgroundPath = Path()
//    /** 缓存的背景可绘制对象 */
//    var bgDrawable: Drawable? = null
//        set(value) {
//            // 检查新的drawable是否是BitmapDrawable，并且其Bitmap是否被回收
//            if (value is BitmapDrawable) {
//                if (value.bitmap.isRecycled) {
//                    // 如果Bitmap已经被回收，不设置这个drawable
//                    return
//                }
//            }
//            field = value
//            postInvalidate()
//        }
//    /** 背景绘制区域矩形 */
//    private val backgroundRect = RectF()
//
//    /** 边框宽度 */
//    private var borderWidth = DEFAULT_BORDER_WIDTH
//    /** 左上角圆角半径 */
//    private var topLeftRadius = DEFAULT_CORNER_RADIUS
//    /** 右上角圆角半径 */
//    private var topRightRadius = DEFAULT_CORNER_RADIUS
//    /** 左下角圆角半径 */
//    private var bottomLeftRadius = DEFAULT_CORNER_RADIUS
//    /** 右下角圆角半径 */
//    private var bottomRightRadius = DEFAULT_CORNER_RADIUS
//
//    /** 内发光颜色 */
//    private var glowColor: Int = Color.MAGENTA
//    /** 内发光半径 */
//    private var glowRadius: Float = DEFAULT_GLOW_RADIUS_DP
//    /** 内描边宽度 */
//    private var strokeWidth: Float = 1f
//    /** 内描边颜色 */
//    private var strokeColor: Int = "#1AFFFFFF".toColorInt()
//    /** 顶部内阴影厚度 */
//    private var innerShadowThickness: Float = 0f
//    /** 顶部内阴影颜色 */
//    private var innerShadowColor: Int = "#0DFFFFFF".toColorInt()
//    /** 阴影颜色 */
//    private var shadowColor: Int = Color.argb(30, 0, 0, 0)
//    /** 阴影偏移X */
//    private var shadowOffsetX: Float = 2f
//    /** 阴影偏移Y */
//    private var shadowOffsetY: Float = 4f
//    /** 阴影模糊半径 */
//    private var shadowBlurRadius: Float = 8f
//
//    /** 蒙层是否启用 */
//    var overlayEnabled: Boolean = false
//        set(value) {
//            field = value
//            invalidate()
//        }
//    /** 蒙层颜色 */
//    var overlayColor: Int = Color.argb(128, 0, 0, 0)
//        set(value) {
//            field = value
//            invalidate()
//        }
//
//    /** 缓存的内发光颜色 */
//    private var cachedGlowColor: Int = -1
//    /** 缓存的安全发光半径 */
//    private var cachedGradientSafeGlowRadius: Float = -1f
//
//    /** 边框动画 */
//    private var animator: ValueAnimator? = null
//    /** 刷新效果动画 */
//    private var refreshAnimator: ValueAnimator? = null
//    /** 边框动画进度 */
//    private var progress = 0f
//    /** 刷新效果动画进度 */
//    private var refreshProgress = 0f
//    /** 初始化状态 */
//    private var isInitialized = false
//    /** 刷新动画延迟任务 */
//    private var refreshRunnable: Runnable? = null
//
//    /** 对角线长度（缓存） */
//    private var diagonalLength = 0f
//    /** 刷新效果宽度（缓存） */
//    private var refreshWidth = 0f
//    /** 流光效果半径（缓存） */
//    private var radius = 0f
//    /** 缓存的宽度 */
//    private var cachedWidth = 0f
//    /** 缓存的高度 */
//    private var cachedHeight = 0f
//    /** 缓存的宽度一半 */
//    private var cachedHalfWidth = 0f
//    /** 缓存的高度一半 */
//    private var cachedHalfHeight = 0f
//    /** 圆角半径变化标志 */
//    private var radiiChanged = true
//    /** 效果运行状态 */
//    private var isEffectRunning = false
//    /** 缓存的内部宽度 */
//    private var cachedInnerWidth = 0f
//    /** 缓存的内部高度 */
//    private var cachedInnerHeight = 0f
//    /** 缓存的安全发光半径 */
//    private var cachedSafeGlowRadius = 0f
//    /** 缓存的安全阴影厚度 */
//    private var cachedSafeShadowThickness = 0f
//    /** 缓存的视图宽度和高度 */
//    private var cachedViewWidth = 0f
//    private var cachedViewHeight = 0f
//
//    /**
//     * 初始化视图
//     *
//     * 从 XML 属性中读取配置值，设置图层类型，初始化画笔，并添加布局监听器。
//     */
//    init {
//        attrs?.let {
//            context.withStyledAttributes(it, R.styleable.FrostedGlowView, defStyleAttr) {
//                topLeftRadius = getDimension(R.styleable.FrostedGlowView_topLeftRadius, DEFAULT_CORNER_RADIUS)
//                topRightRadius = getDimension(R.styleable.FrostedGlowView_topRightRadius, DEFAULT_CORNER_RADIUS)
//                bottomLeftRadius = getDimension(R.styleable.FrostedGlowView_bottomLeftRadius, DEFAULT_CORNER_RADIUS)
//                bottomRightRadius = getDimension(R.styleable.FrostedGlowView_bottomRightRadius, DEFAULT_CORNER_RADIUS)
//
//                val cornerRadius = getDimension(R.styleable.FrostedGlowView_cornerRadius, -1f)
//                if (cornerRadius >= 0) {
//                    setCornerRadius(cornerRadius)
//                }
//
//                glowColor = getColor(R.styleable.FrostedGlowView_glowColor, innerShadowColor)
//                glowRadius = getDimension(R.styleable.FrostedGlowView_glowRadius, DEFAULT_GLOW_RADIUS_DP * resources.displayMetrics.density)
//                strokeWidth = getDimension(R.styleable.FrostedGlowView_glowStrokeWidth, 1f)
//                strokeColor = getColor(R.styleable.FrostedGlowView_glowStrokeColor, strokeColor)
//                innerShadowThickness = getDimension(R.styleable.FrostedGlowView_innerShadowThickness, 0f)
//                innerShadowColor = getColor(R.styleable.FrostedGlowView_innerShadowColor, innerShadowColor)
//                overlayEnabled = getBoolean(R.styleable.FrostedGlowView_overlayEnabled, false)
//                overlayColor = getColor(R.styleable.FrostedGlowView_overlayColor, Color.argb(128, 0, 0, 0))
//            }
//        }
//        // 使用硬件加速，利用GPU处理复杂的图形操作，降低CPU占用
//        setLayerType(LAYER_TYPE_HARDWARE, null)
//        setupPaints()
//
//        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                isInitialized = true
//                viewTreeObserver.removeOnGlobalLayoutListener(this)
//            }
//        })
//    }
//
//    /**
//     * 设置画笔属性
//     *
//     * 初始化各种画笔的样式、宽度和颜色。
//     */
//    private fun setupPaints() {
//        borderPaint.style = Paint.Style.STROKE
//        borderPaint.strokeWidth = borderWidth
//        borderPaint.color = Color.argb(25, 255, 255, 255)
//
//        refreshPaint.style = Paint.Style.FILL
//        refreshPaint.strokeWidth = 2f
//
//        lightPaint.style = Paint.Style.STROKE
//        lightPaint.strokeWidth = borderWidth
//    }
//
//    /**
//     * 启动动画
//     *
//     * 启动边框动画和刷新效果动画。
//     */
//    private fun startAnimations() {
//        if (isEffectRunning) return
//        isEffectRunning = true
//        setupBorderAnimation()
//        setupRefreshAnimation()
//    }
//
//    /**
//     * 停止动画
//     *
//     * 停止所有动画，清理资源，并重置动画进度。
//     */
//    private fun stopAnimations() {
//        isEffectRunning = false
//        animator?.cancel()
//        refreshAnimator?.cancel()
//        refreshRunnable?.let { removeCallbacks(it) }
//        refreshRunnable = null
//        progress = 0f
//        refreshProgress = 0f
//        // 只清理动画相关的渐变对象，保留内发光渐变
//        borderGradient = null
//        refreshGradient = null
//        staticBorderGradient = null
//        flowingStaticBorderGradient = null
//        segmentGradient = null
//        invalidate()
//    }
//
//    /**
//     * 设置边框动画
//     *
//     * 配置并启动流光边框动画，使其无限循环。
//     */
//    private fun setupBorderAnimation() {
//        animator = ValueAnimator.ofFloat(0f, 1f)
//        animator?.duration = 4000 // 增加动画时长，使动画更平滑
//        animator?.repeatCount = ValueAnimator.INFINITE
//        animator?.repeatMode = ValueAnimator.RESTART
//        animator?.interpolator = LinearInterpolator() // 使用加速减速插值器，使动画开始和结束更平滑
//        animator?.addUpdateListener {
//            val newProgress = it.animatedValue as Float
//            // 进一步调整阈值，减少重绘次数，降低CPU占用
//            if (abs(newProgress - progress) > 0.02f) {
//                progress = newProgress
//                // 只刷新边框区域
//                val borderExtra = (borderWidth * 2).toInt()
//                ViewCompat.postInvalidateOnAnimation(
//                    this,
//                    borderExtra,
//                    borderExtra,
//                    width - borderExtra,
//                    height - borderExtra
//                )
//            }
//        }
//        animator?.addListener(object : Animator.AnimatorListener {
//            override fun onAnimationStart(animation: Animator) {}
//            override fun onAnimationEnd(animation: Animator) {}
//            override fun onAnimationCancel(animation: Animator) {}
//            override fun onAnimationRepeat(animation: Animator) {
//                ViewCompat.postInvalidateOnAnimation(this@FrostedGlowView)
//            }
//        })
//        animator?.start()
//    }
//
//    /**
//     * 设置刷新效果动画
//     *
//     * 配置并启动刷新效果动画，完成后重新启动。
//     */
//    private fun setupRefreshAnimation() {
//        refreshAnimator = ValueAnimator.ofFloat(0f, 1f)
//        refreshAnimator?.duration = 3000
//        refreshAnimator?.repeatCount = 0
//        refreshAnimator?.interpolator = LinearInterpolator()
//        refreshAnimator?.addUpdateListener {
//            val newProgress = it.animatedValue as Float
//            // 进一步调整阈值，减少重绘次数，降低CPU占用
//            if (abs(newProgress - refreshProgress) > 0.05f) {
//                refreshProgress = newProgress
//                // 只刷新内部区域
//                val borderExtra = borderWidth
//                ViewCompat.postInvalidateOnAnimation(
//                    this,
//                    borderExtra.toInt(),
//                    borderExtra.toInt(),
//                    (width - borderExtra).toInt(),
//                    (height - borderExtra).toInt()
//                )
//            }
//        }
//        refreshAnimator?.addListener(object : Animator.AnimatorListener {
//            override fun onAnimationStart(animation: Animator) {}
//            override fun onAnimationEnd(animation: Animator) {
//                refreshRunnable = Runnable {
//                    refreshProgress = 0f
//                    setupRefreshAnimation()
//                }
//                postDelayed(refreshRunnable, 0) // 增加延迟，避免动画过于频繁
//            }
//            override fun onAnimationCancel(animation: Animator) {}
//            override fun onAnimationRepeat(animation: Animator) {}
//        })
//        refreshAnimator?.start()
//    }
//
//    /**
//     * 绘制视图内容
//     *
//     * 按顺序绘制：阴影 → 背景（裁剪到描边范围内） → 内发光 → 顶部内阴影 → 刷新效果（有焦点时） → 内描边 → 边框（静态/流光） → 子视图
//     *
//     * @param canvas 画布
//     */
//    override fun dispatchDraw(canvas: Canvas) {
//        if (radiiChanged) {
//            updateRadiiArray()
//        }
//
//        val width = width.toFloat()
//        val height = height.toFloat()
//
//        // 绘制自然阴影
//        if (shadowBlurRadius > 0 && width > 0 && height > 0) {
//            canvas.withSave() {
//                // 创建阴影路径（稍微扩大以容纳模糊效果）
//                val shadowPath = Path()
//                shadowPath.addRoundRect(
//                    borderWidth - shadowBlurRadius / 2,
//                    borderWidth - shadowBlurRadius / 2,
//                    width - borderWidth + shadowBlurRadius / 2,
//                    height - borderWidth + shadowBlurRadius / 2,
//                    radii, Path.Direction.CW
//                )
//
//                // 设置阴影画笔
//                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
//                shadowPaint.color = shadowColor
//                shadowPaint.setShadowLayer(
//                    shadowBlurRadius,
//                    shadowOffsetX,
//                    shadowOffsetY,
//                    shadowColor
//                )
//
//                // 绘制阴影
//                drawShadowPath(this, shadowPath, shadowPaint)
//            }
//        }
//
//        // 绘制背景（限制在描边范围内）
//        val background = bgDrawable
//        if (background != null && width > 0 && height > 0) {
//            // 检查背景是否是BitmapDrawable，并且其Bitmap是否被回收
//            val isBitmapDrawableValid = if (background is BitmapDrawable) {
//                !background.bitmap.isRecycled
//            } else {
//                true
//            }
//
//            if (isBitmapDrawableValid) {
//                canvas.withSave() {
//                    // 创建背景裁剪路径
//                    backgroundPath.reset()
//                    backgroundPath.addRoundRect(
//                        borderWidth, borderWidth, width - borderWidth, height - borderWidth,
//                        radii, Path.Direction.CW
//                    )
//                    clipPath(backgroundPath)
//
//                    // 绘制背景
//                    backgroundRect.set(0f, 0f, width, height)
//                    background.setBounds(0, 0, width.toInt(), height.toInt())
//                    background.draw(this)
//                }
//            }
//        }
//
//        // 绘制蒙层
//        if (overlayEnabled && width > 0 && height > 0) {
//            canvas.withSave() {
//                backgroundPath.reset()
//                backgroundPath.addRoundRect(
//                    borderWidth, borderWidth, width - borderWidth, height - borderWidth,
//                    radii, Path.Direction.CW
//                )
//                clipPath(backgroundPath)
//
//                overlayPaint.color = overlayColor
//                drawPath(backgroundPath, overlayPaint)
//            }
//        }
//
//        // 绘制内发光等效果
//        canvas.withSave() {
//            // 创建裁剪路径
//            backgroundPath.reset()
//            backgroundPath.addRoundRect(
//                borderWidth, borderWidth, width - borderWidth, height - borderWidth,
//                radii, Path.Direction.CW
//            )
//            clipPath(backgroundPath)
//
//            drawInnerGlow(this, width, height)
////            drawTopInnerShadow(this, width, height)
//
//            if (isFocused) {
//                drawRefreshEffect(this, width, height)
//            }
//
//            drawInnerStroke(this, width, height)
//
//        }
//
//        // 绘制边框
//        if (isFocused) {
//            drawFlowingBorder(canvas, width, height)
//        } else {
//            drawStaticBorder(canvas, width, height)
//        }
//
//        // 绘制子视图
//        super.dispatchDraw(canvas)
//    }
//
//    /**
//     * 焦点变化监听
//     *
//     * 获得焦点时启动动画，失去焦点时停止动画。
//     *
//     * @param gainFocus 是否获得焦点
//     * @param direction 焦点方向
//     * @param previouslyFocusedRect 之前的焦点矩形
//     */
//    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
//        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
//        if (gainFocus) {
//            startAnimations()
//        } else {
//            stopAnimations()
//        }
//    }
//
//    /**
//     * 更新圆角半径数组
//     *
//     * 同步圆角半径到 radii 数组，用于绘制圆角矩形。
//     */
//    private fun updateRadiiArray() {
//        radii[0] = topLeftRadius
//        radii[1] = topLeftRadius
//        radii[2] = topRightRadius
//        radii[3] = topRightRadius
//        radii[4] = bottomRightRadius
//        radii[5] = bottomRightRadius
//        radii[6] = bottomLeftRadius
//        radii[7] = bottomLeftRadius
//        radiiChanged = false
//    }
//
//
//
//    /**
//     * 绘制内发光效果
//     *
//     * 沿着borderPath向内绘制内发光效果，避免重叠。
//     *
//     * @param canvas 画布
//     * @param width 视图宽度
//     * @param height 视图高度
//     */
//    private fun drawInnerGlow(canvas: Canvas, width: Float, height: Float) {
//        if (width <= 0 || height <= 0) return
//
//        // 检查宽度和高度是否变化
//        val sizeChanged = cachedViewWidth != width || cachedViewHeight != height
//        val glowChanged = cachedGlowColor != glowColor || cachedGradientSafeGlowRadius != glowRadius
//
//        if (sizeChanged || glowChanged) {
//            cachedViewWidth = width
//            cachedViewHeight = height
//            cachedInnerWidth = width - borderWidth * 2
//            cachedInnerHeight = height - borderWidth * 2
//            // 重新计算安全发光半径
//            cachedSafeGlowRadius = min(glowRadius, min(cachedInnerWidth / 2f, cachedInnerHeight / 2f))
//
//            // 重新创建内发光路径
//            if (cachedInnerWidth > 0 && cachedInnerHeight > 0 && cachedSafeGlowRadius > 0f) {
//                glowPath.reset()
//                // 首先创建与borderPath相同的路径
//                glowPath.addRoundRect(
//                    borderWidth, borderWidth, width - borderWidth, height - borderWidth,
//                    radii, Path.Direction.CW
//                )
//
//                // 创建一个向内偏移的路径
//                for (i in 0 until 8) {
//                    innerRadius[i] = max(0f, radii[i] - cachedSafeGlowRadius)
//                }
//                innerPath.reset()
//                innerPath.addRoundRect(
//                    borderWidth + cachedSafeGlowRadius,
//                    borderWidth + cachedSafeGlowRadius,
//                    width - borderWidth - cachedSafeGlowRadius,
//                    height - borderWidth - cachedSafeGlowRadius,
//                    innerRadius, Path.Direction.CW
//                )
//
//                // 从外路径中减去内路径，得到环形发光区域
//                glowPath.op(innerPath, Path.Op.DIFFERENCE)
//
//                // 重新创建渐变对象
//                topGlowGradient = LinearGradient(
//                    borderWidth, borderWidth,
//                    borderWidth, borderWidth + cachedSafeGlowRadius,
//                    intArrayOf(glowColor, Color.TRANSPARENT),
//                    floatArrayOf(0f, 1f),
//                    Shader.TileMode.CLAMP
//                )
//
//                bottomGlowGradient = LinearGradient(
//                    borderWidth, height - borderWidth,
//                    borderWidth, height - borderWidth - cachedSafeGlowRadius,
//                    intArrayOf(glowColor, Color.TRANSPARENT),
//                    floatArrayOf(0f, 1f),
//                    Shader.TileMode.CLAMP
//                )
//
//                leftGlowGradient = LinearGradient(
//                    borderWidth, borderWidth,
//                    borderWidth + cachedSafeGlowRadius, borderWidth,
//                    intArrayOf(glowColor, Color.TRANSPARENT),
//                    floatArrayOf(0f, 1f),
//                    Shader.TileMode.CLAMP
//                )
//
//                rightGlowGradient = LinearGradient(
//                    width - borderWidth, borderWidth,
//                    width - borderWidth - cachedSafeGlowRadius, borderWidth,
//                    intArrayOf(glowColor, Color.TRANSPARENT),
//                    floatArrayOf(0f, 1f),
//                    Shader.TileMode.CLAMP
//                )
//
//                cachedGlowColor = glowColor
//                cachedGradientSafeGlowRadius = glowRadius
//            }
//        }
//
//        if (cachedInnerWidth <= 0 || cachedInnerHeight <= 0 || cachedSafeGlowRadius <= 0f) return
//
//        // 分别绘制四个边的内发光，每个边使用不同的渐变方向
//        // 顶部发光（从上到下渐变）
//        topGlowGradient?.let {
//            glowPaint.shader = it
//            canvas.drawPath(glowPath, glowPaint)
//        }
//
//        // 底部发光（从下到上渐变）
//        bottomGlowGradient?.let {
//            glowPaint.shader = it
//            canvas.drawPath(glowPath, glowPaint)
//        }
//
//        // 左侧发光（从左到右渐变）
//        leftGlowGradient?.let {
//            glowPaint.shader = it
//            canvas.drawPath(glowPath, glowPaint)
//        }
//
//        // 右侧发光（从右到左渐变）
//        rightGlowGradient?.let {
//            glowPaint.shader = it
//            canvas.drawPath(glowPath, glowPaint)
//        }
//
//        glowPaint.shader = null
//    }
//
//    /**
//     * 绘制内描边
//     *
//     * @param canvas 画布
//     * @param width 视图宽度
//     * @param height 视图高度
//     */
//    private fun drawInnerStroke(canvas: Canvas, width: Float, height: Float) {
//        if (strokeWidth <= 0f) return
//
//        strokePaint.color = strokeColor
//        strokePaint.strokeWidth = strokeWidth * 2
//
//        val strokeRect = RectF(
//            borderWidth + strokeWidth,
//            borderWidth + strokeWidth,
//            width - borderWidth - strokeWidth,
//            height - borderWidth - strokeWidth
//        )
//
//        if (topLeftRadius > 0f || topRightRadius > 0f || bottomLeftRadius > 0f || bottomRightRadius > 0f) {
//            innerRadius[0] = max(0f, topLeftRadius - strokeWidth)
//            innerRadius[1] = innerRadius[0]
//            innerRadius[2] = max(0f, topRightRadius - strokeWidth)
//            innerRadius[3] = innerRadius[2]
//            innerRadius[4] = max(0f, bottomRightRadius - strokeWidth)
//            innerRadius[5] = innerRadius[4]
//            innerRadius[6] = max(0f, bottomLeftRadius - strokeWidth)
//            innerRadius[7] = innerRadius[6]
//
//            glowPath.reset()
//            glowPath.addRoundRect(strokeRect, innerRadius, Path.Direction.CW)
//            canvas.drawPath(glowPath, strokePaint)
//        } else {
//            glowPath.reset()
//            glowPath.addRect(strokeRect, Path.Direction.CW)
//            canvas.drawPath(glowPath, strokePaint)
//        }
//    }
//
//    /**
//     * 绘制刷新效果
//     *
//     * @param canvas 画布
//     * @param width 视图宽度
//     * @param height 视图高度
//     */
//    private fun drawRefreshEffect(canvas: Canvas, width: Float, height: Float) {
//        if (cachedInnerWidth != width - borderWidth * 2 || cachedInnerHeight != height - borderWidth * 2) {
//            cachedInnerWidth = width - borderWidth * 2
//            cachedInnerHeight = height - borderWidth * 2
//        }
//
//        if (cachedInnerWidth <= 0 || cachedInnerHeight <= 0) return
//
//        if (diagonalLength == 0f || refreshWidth == 0f) {
//            diagonalLength = sqrt((cachedInnerWidth * cachedInnerWidth + cachedInnerHeight * cachedInnerHeight).toDouble()).toFloat()
//            refreshWidth = diagonalLength / 6
//        }
//
//        val progress = refreshProgress
//        val startX = -refreshWidth
//        val startY = -refreshWidth
//        val endX = cachedInnerWidth + refreshWidth
//        val endY = cachedInnerHeight + refreshWidth
//
//        val currentX = startX + (endX - startX) * progress
//        val currentY = startY + (endY - startY) * progress
//
//        if (refreshGradient == null || cachedWidth != cachedInnerWidth || cachedHeight != cachedInnerHeight) {
//            refreshGradient = LinearGradient(
//                -refreshWidth, -refreshWidth,
//                refreshWidth, refreshWidth,
//                intArrayOf(
//                    Color.TRANSPARENT,
//                    Color.argb(100, 255, 255, 255),
//                    Color.TRANSPARENT
//                ),
//                floatArrayOf(0f, 0.5f, 1f),
//                Shader.TileMode.CLAMP
//            )
//        }
//
//        refreshMatrix.reset()
//        refreshMatrix.setTranslate(currentX, currentY)
//        refreshGradient?.setLocalMatrix(refreshMatrix)
//        refreshPaint.shader = refreshGradient
//
//        canvas.drawRect(borderWidth, borderWidth, width - borderWidth, height - borderWidth, refreshPaint)
//    }
//
//    /**
//     * 绘制静态边框
//     *
//     * @param canvas 画布
//     * @param width 视图宽度
//     * @param height 视图高度
//     */
//    private fun drawStaticBorder(canvas: Canvas, width: Float, height: Float) {
//        borderPath.reset()
//        borderPath.addRoundRect(
//            borderWidth, borderWidth, width - borderWidth, height - borderWidth,
//            radii, Path.Direction.CW
//        )
//
//        // 创建边框内发光渐变
//        if (staticBorderGradient == null || cachedWidth != width || cachedHeight != height) {
//            staticBorderGradient = LinearGradient(
//                0f, 0f,
//                0f, borderWidth * 2,
//                intArrayOf(Color.argb(50, 255, 255, 255), Color.TRANSPARENT),
//                floatArrayOf(0f, 1f),
//                Shader.TileMode.CLAMP
//            )
//            cachedWidth = width
//            cachedHeight = height
//        }
//        borderPaint.shader = staticBorderGradient
//
//        canvas.drawPath(borderPath, borderPaint)
//        borderPaint.shader = null // 重置 shader
//    }
//
//    /**
//     * 绘制流光边框
//     *
//     * @param canvas 画布
//     * @param width 视图宽度
//     * @param height 视图高度
//     */
//    private fun drawFlowingBorder(canvas: Canvas, width: Float, height: Float) {
//        // 1. 构建圆角矩形边框路径
//        if (cachedPathWidth != width || cachedPathHeight != height || borderPathDirty) {
//            borderPath.reset()
//            borderPath.addRoundRect(
//                borderWidth, borderWidth, width - borderWidth, height - borderWidth,
//                radii, Path.Direction.CW
//            )
//            // 2. 重新计算路径长度
//            pathMeasure.setPath(borderPath, false)
//            cachedTotalLength = pathMeasure.length
//            // 确保光效长度不超过路径总长度的四分之一，保证只显示一段光源
//            cachedGlowLength = min(cachedTotalLength / 4f, 200f) // 限制最大光效长度为200像素
//            cachedPathWidth = width
//            cachedPathHeight = height
//            borderPathDirty = false
//        }
//
//        // 如果路径长度为0，直接返回
//        if (cachedTotalLength <= 0f) return
//
//        // 3. 计算循环进度，确保平滑过渡
//        val cycleProgress = (progress * cachedTotalLength) % cachedTotalLength
//        val start = cycleProgress          // 光效起点（沿路径距离）
//        val end = start + cachedGlowLength                // 光效终点
//
//        // 4. 设置画笔样式
//        lightPaint.style = Paint.Style.STROKE
//        lightPaint.strokeWidth = borderWidth
//        lightPaint.pathEffect = null
//
//        // 5. 绘制光效，确保只显示一段光源
//        // 当光效跨越路径终点时，只绘制一段连续的光源
//        if (end <= cachedTotalLength) {
//            // 光效在路径范围内，直接绘制
//            drawSegment(canvas, pathMeasure, start.toFloat(), end)
//        } else {
//            // 光效跨越路径终点，计算实际需要绘制的长度
//            // 确保光效长度不超过路径总长度
//            val actualEnd = start + cachedGlowLength
//            if (actualEnd <= cachedTotalLength) {
//                drawSegment(canvas, pathMeasure, start.toFloat(), actualEnd)
//            } else {
//                // 重新计算起点，确保光效在路径范围内
//                val adjustedStart = (start - cachedTotalLength) % cachedTotalLength
//                val adjustedEnd = adjustedStart + cachedGlowLength
//                if (adjustedEnd <= cachedTotalLength) {
//                    drawSegment(canvas, pathMeasure, adjustedStart, adjustedEnd)
//                }
//            }
//        }
//
//        // 6. 清除 shader，避免影响其他绘制
//        lightPaint.shader = null
//    }
//
//    /**
//     * 计算从路径起点（左上角）到右上角的精确距离
//     */
//    private fun calculateOffsetToTopRight(width: Float, height: Float): Float {
//        // 上边的直线段长度
//        val topEdgeLength = width - borderWidth * 2 - topLeftRadius - topRightRadius
//
//        // 路径起点在左上角，直接沿着上边直线到达右上角
//        // 不需要加圆角弧长，因为右上角点位于上边直线的末端
//        return topEdgeLength
//    }
//
//    /**
//     * 绘制阴影路径
//     *
//     * @param canvas 画布
//     * @param path 阴影路径
//     * @param paint 阴影画笔
//     */
//    private fun drawShadowPath(canvas: Canvas, path: Path, paint: Paint) {
//        // 绘制阴影路径
//        canvas.drawPath(path, paint)
//    }
//
//    /**
//     * 绘制一段路径，并为其创建从起点到终点的线性渐变
//     * @param segStart 段落起始距离（沿路径）
//     * @param segEnd   段落结束距离
//     */
//    /** 点数组（用于路径测量） */
//    private val startPoint = FloatArray(2)
//    private val endPoint = FloatArray(2)
//    /** 缓存的渐变对象 */
//    private var segmentGradient: LinearGradient? = null
//    /** 缓存的渐变起点和终点 */
//    private val cachedGradientStart = FloatArray(2)
//    private val cachedGradientEnd = FloatArray(2)
//
//    private fun drawSegment(canvas: Canvas, pathMeasure: PathMeasure, segStart: Float, segEnd: Float) {
//        segmentPath.reset()
//        val success = pathMeasure.getSegment(segStart, segEnd, segmentPath, true)
//        if (!success || segmentPath.isEmpty) return
//
//        // 获取段落起点和终点的实际坐标
//        pathMeasure.getPosTan(segStart, startPoint, null)
//        pathMeasure.getPosTan(segEnd, endPoint, null)
//
//        // 检查是否需要重新创建渐变对象
//        val gradientChanged = startPoint[0] != cachedGradientStart[0] || startPoint[1] != cachedGradientStart[1] ||
//                endPoint[0] != cachedGradientEnd[0] || endPoint[1] != cachedGradientEnd[1]
//
//        if (gradientChanged || segmentGradient == null) {
//            // 创建渐变：起点透明 → 中间亮白 → 终点透明
//            segmentGradient = LinearGradient(
//                startPoint[0], startPoint[1],
//                endPoint[0], endPoint[1],
//                intArrayOf(
//                    Color.TRANSPARENT,
//                    Color.argb(230, 255, 255, 255),   // 亮白，可调整透明度
//                    Color.TRANSPARENT
//                ),
//                floatArrayOf(0f, 0.5f, 1f),
//                Shader.TileMode.CLAMP
//            )
//            // 更新缓存
//            cachedGradientStart[0] = startPoint[0]
//            cachedGradientStart[1] = startPoint[1]
//            cachedGradientEnd[0] = endPoint[0]
//            cachedGradientEnd[1] = endPoint[1]
//        }
//
//        lightPaint.shader = segmentGradient
//        canvas.drawPath(segmentPath, lightPaint)
//    }
//    /*private fun drawFlowingBorder(canvas: Canvas, width: Float, height: Float) {
//        if (borderPathDirty) {
//            borderPath.reset()
//            borderPath.addRoundRect(
//                borderWidth, borderWidth, width - borderWidth, height - borderWidth,
//                radii, Path.Direction.CW
//            )
//            borderPathDirty = false
//        }
//
//        // 绘制静态内发光边框
//        if (flowingStaticBorderGradient == null) {
//            flowingStaticBorderGradient = LinearGradient(
//                0f, 0f,
//                0f, borderWidth * 2,
//                intArrayOf(Color.argb(50, 255, 255, 255), Color.TRANSPARENT),
//                floatArrayOf(0f, 1f),
//                Shader.TileMode.CLAMP
//            )
//        }
//        borderPaint.shader = flowingStaticBorderGradient
//        canvas.drawPath(borderPath, borderPaint)
//        borderPaint.shader = null // 重置 shader
//
//        if (borderGradient == null || cachedWidth != width || cachedHeight != height) {
//            // 创建更窄的渐变，只显示一段光效
//            val glowLength = min(width, height) / 3 // 光效长度
//            borderGradient = LinearGradient(
//                -glowLength / 2, -glowLength / 2,
//                glowLength / 2, glowLength / 2,
//                intArrayOf(
//                    Color.TRANSPARENT,
//                    Color.WHITE,
//                    Color.argb(200, 255, 255, 255),
//                    Color.TRANSPARENT
//                ),
//                floatArrayOf(0f, 0.3f, 0.5f, 1f),
//                Shader.TileMode.CLAMP
//            )
//            cachedWidth = width
//            cachedHeight = height
//            cachedHalfWidth = width / 2
//            cachedHalfHeight = height / 2
//        }
//
//        if (radius == 0f) {
//            radius = sqrt((cachedHalfWidth * cachedHalfWidth + cachedHalfHeight * cachedHalfHeight).toDouble()).toFloat()
//        }
//
//        // 计算光效位置，使其围绕边框循环移动
//        val angle = progress * 360
//        val radian = Math.toRadians(angle.toDouble())
//        val offsetX = (cos(radian) * radius).toFloat()
//        val offsetY = (sin(radian) * radius).toFloat()
//
//        matrix.reset()
//        matrix.setTranslate(offsetX + cachedHalfWidth, offsetY + cachedHalfHeight)
//        matrix.postRotate(angle + 45, cachedHalfWidth, cachedHalfHeight)
//
//        borderGradient?.setLocalMatrix(matrix)
//        lightPaint.shader = borderGradient
//
//        canvas.drawPath(borderPath, lightPaint)
//    }*/
//
//    /**
//     * 尺寸变化回调
//     *
//     * 当视图尺寸变化时，清理缓存的渐变对象、尺寸数据和背景 bitmap。
//     *
//     * @param w 新宽度
//     * @param h 新高度
//     * @param oldw 旧宽度
//     * @param oldh 旧高度
//     */
//    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
//        super.onSizeChanged(w, h, oldw, oldh)
//        diagonalLength = 0f
//        refreshWidth = 0f
//        radius = 0f
//        borderGradient = null
//        refreshGradient = null
//        topGlowGradient = null
//        bottomGlowGradient = null
//        leftGlowGradient = null
//        rightGlowGradient = null
//        staticBorderGradient = null
//        flowingStaticBorderGradient = null
//        segmentGradient = null
////        topShadowGradient = null
////        topLeftCornerShadowGradient = null
////        topRightCornerShadowGradient = null
//        cachedWidth = 0f
//        cachedHeight = 0f
//        cachedHalfWidth = 0f
//        cachedHalfHeight = 0f
//        cachedInnerWidth = 0f
//        cachedInnerHeight = 0f
//        cachedSafeGlowRadius = 0f
//        cachedSafeShadowThickness = 0f
//        cachedViewWidth = 0f
//        cachedViewHeight = 0f
//        cachedTotalLength = 0f
//        cachedGlowLength = 0f
//        cachedPathWidth = 0f
//        cachedPathHeight = 0f
//        cachedGlowColor = -1
//        cachedGradientSafeGlowRadius = -1f
//        cachedGradientStart[0] = 0f
//        cachedGradientStart[1] = 0f
//        cachedGradientEnd[0] = 0f
//        cachedGradientEnd[1] = 0f
//        borderPathDirty = true
//        clipPathDirty = true
//        glowPathDirty = true
//        innerPathDirty = true
//    }
//
//    /**
//     * 视图分离回调
//     *
//     * 当视图从窗口分离时，停止所有动画。
//     */
//    override fun onDetachedFromWindow() {
//        super.onDetachedFromWindow()
//        stopAnimations()
//    }
//
//    /**
//     * 设置是否禁止绘制
//     *
//     * 强制设置为 false，确保视图能够正常绘制。
//     *
//     * @param willNotDraw 是否禁止绘制
//     */
//    override fun setWillNotDraw(willNotDraw: Boolean) {
//        super.setWillNotDraw(false)
//    }
//
//    /**
//     * 设置圆角半径
//     *
//     * @param topLeft 左上角圆角半径
//     * @param topRight 右上角圆角半径
//     * @param bottomLeft 左下角圆角半径
//     * @param bottomRight 右下角圆角半径
//     */
//    fun setCornerRadii(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float) {
//        this.topLeftRadius = topLeft
//        this.topRightRadius = topRight
//        this.bottomLeftRadius = bottomLeft
//        this.bottomRightRadius = bottomRight
//        markRadiiChanged()
//    }
//
//    /**
//     * 设置左上角圆角半径
//     *
//     * @param radius 圆角半径
//     */
//    fun setTopLeftRadius(radius: Float) {
//        this.topLeftRadius = radius
//        markRadiiChanged()
//    }
//
//    /**
//     * 设置右上角圆角半径
//     *
//     * @param radius 圆角半径
//     */
//    fun setTopRightRadius(radius: Float) {
//        this.topRightRadius = radius
//        markRadiiChanged()
//    }
//
//    /**
//     * 设置左下角圆角半径
//     *
//     * @param radius 圆角半径
//     */
//    fun setBottomLeftRadius(radius: Float) {
//        this.bottomLeftRadius = radius
//        markRadiiChanged()
//    }
//
//    /**
//     * 设置右下角圆角半径
//     *
//     * @param radius 圆角半径
//     */
//    fun setBottomRightRadius(radius: Float) {
//        this.bottomRightRadius = radius
//        markRadiiChanged()
//    }
//
//    /**
//     * 标记圆角半径已变化
//     *
//     * 更新 radii 数组并触发重绘。
//     */
//    private fun markRadiiChanged() {
//        radiiChanged = true
//        borderPathDirty = true
//        clipPathDirty = true
//        glowPathDirty = true
//        innerPathDirty = true
//        invalidate()
//    }
//
//    /**
//     * 获取左上角圆角半径
//     *
//     * @return 左上角圆角半径
//     */
//    fun getTopLeftRadius(): Float = topLeftRadius
//
//    /**
//     * 获取右上角圆角半径
//     *
//     * @return 右上角圆角半径
//     */
//    fun getTopRightRadius(): Float = topRightRadius
//
//    /**
//     * 获取左下角圆角半径
//     *
//     * @return 左下角圆角半径
//     */
//    fun getBottomLeftRadius(): Float = bottomLeftRadius
//
//    /**
//     * 获取右下角圆角半径
//     *
//     * @return 右下角圆角半径
//     */
//    fun getBottomRightRadius(): Float = bottomRightRadius
//
//    /**
//     * 设置所有角的圆角半径
//     *
//     * @param radius 圆角半径
//     */
//    fun setCornerRadius(radius: Float) {
//        topLeftRadius = radius
//        topRightRadius = radius
//        bottomLeftRadius = radius
//        bottomRightRadius = radius
//        markRadiiChanged()
//    }
//
//    /**
//     * 获取圆角半径
//     *
//     * @return 圆角半径（所有角相同）
//     */
//    fun getCornerRadius(): Float = topLeftRadius
//
//    /**
//     * 设置内发光颜色
//     *
//     * @param color 内发光颜色
//     */
//    fun setGlowColor(color: Int) {
//        this.glowColor = color
//        invalidate()
//    }
//
//    /**
//     * 设置内发光半径
//     *
//     * @param radius 内发光半径
//     */
//    fun setGlowRadius(radius: Float) {
//        this.glowRadius = max(0f, radius)
//        invalidate()
//    }
//
//    /**
//     * 设置内描边宽度
//     *
//     * @param width 内描边宽度
//     */
//    fun setStrokeWidth(width: Float) {
//        this.strokeWidth = max(0f, width)
//        invalidate()
//    }
//
//    /**
//     * 设置内描边颜色
//     *
//     * @param color 内描边颜色
//     */
//    fun setStrokeColor(color: Int) {
//        this.strokeColor = color
//        invalidate()
//    }
//
//    /**
//     * 设置顶部内阴影厚度
//     *
//     * @param thickness 顶部内阴影厚度
//     */
//    fun setInnerShadowThickness(thickness: Float) {
//        this.innerShadowThickness = max(0f, thickness)
//        invalidate()
//    }
//
//    /**
//     * 设置顶部内阴影颜色
//     *
//     * @param color 顶部内阴影颜色
//     */
//    fun setInnerShadowColor(color: Int) {
//        this.innerShadowColor = color
//        invalidate()
//    }
//
//    /**
//     * 设置阴影颜色
//     *
//     * @param color 阴影颜色
//     */
//    fun setShadowColor(color: Int) {
//        this.shadowColor = color
//        invalidate()
//    }
//
//    /**
//     * 设置阴影偏移
//     *
//     * @param offsetX 阴影偏移X
//     * @param offsetY 阴影偏移Y
//     */
//    fun setShadowOffset(offsetX: Float, offsetY: Float) {
//        this.shadowOffsetX = offsetX
//        this.shadowOffsetY = offsetY
//        invalidate()
//    }
//
//    /**
//     * 设置阴影模糊半径
//     *
//     * @param blurRadius 阴影模糊半径
//     */
//    fun setShadowBlurRadius(blurRadius: Float) {
//        this.shadowBlurRadius = max(0f, blurRadius)
//        invalidate()
//    }
//
//    /**
//     * 获取阴影颜色
//     *
//     * @return 阴影颜色
//     */
//    fun getShadowColor(): Int = shadowColor
//
//    /**
//     * 获取阴影偏移X
//     *
//     * @return 阴影偏移X
//     */
//    fun getShadowOffsetX(): Float = shadowOffsetX
//
//    /**
//     * 获取阴影偏移Y
//     *
//     * @return 阴影偏移Y
//     */
//    fun getShadowOffsetY(): Float = shadowOffsetY
//
//    /**
//     * 获取阴影模糊半径
//     *
//     * @return 阴影模糊半径
//     */
//    fun getShadowBlurRadius(): Float = shadowBlurRadius
//
//}
