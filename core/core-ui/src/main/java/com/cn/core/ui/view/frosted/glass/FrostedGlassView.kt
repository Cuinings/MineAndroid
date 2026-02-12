package com.cn.core.ui.view.frosted.glass

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import com.cn.core.ui.R
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class FrostedGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG) // 边框画笔
    private val refreshPaint = Paint(Paint.ANTI_ALIAS_FLAG) // 刷新效果画笔
    private val backgroundPaint = Paint() // 背景画笔（复用）
    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG) // 流光效果画笔（复用）
    private val borderWidth = 4f // 边框宽度
    // 圆角半径 (默认16f)
    private var topLeftRadius = 16f
    private var topRightRadius = 16f
    private var bottomLeftRadius = 16f
    private var bottomRightRadius = 16f
    private var animator: ValueAnimator? = null // 边框动画
    private var refreshAnimator: ValueAnimator? = null // 刷新效果动画
    private var progress = 0f // 边框动画进度
    private var refreshProgress = 0f // 刷新效果动画进度
    private var isInitialized = false // 初始化状态
    
    // 缓存对象，避免频繁创建
    private val backgroundPath = Path()
    private val borderPath = Path()
    private val clipPath = Path()
    private val radii = FloatArray(8) // 圆角半径数组
    private val matrix = Matrix() // 矩阵对象（复用）
    private var diagonalLength = 0f // 对角线长度（缓存）
    private var refreshWidth = 0f // 刷新效果宽度（缓存）
    private var radius = 0f // 用于流光效果的半径（缓存）
    // 缓存渐变对象
    private var borderGradient: LinearGradient? = null
    private var refreshGradient: LinearGradient? = null
    // 缓存计算值
    private var cachedWidth = 0f
    private var cachedHeight = 0f
    // 刷新效果矩阵
    private val refreshMatrix = Matrix() // 矩阵对象（复用）
    // 缓存的矩形对象
    private val refreshAreaRect = RectF() // 刷新区域矩形（复用）
    private val viewAreaRect = RectF() // 视图区域矩形（复用）
    // 圆角半径变化标志
    private var radiiChanged = true

    init {
        // 启用硬件加速（如果可用）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        
        // 从XML布局中读取自定义属性
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.FrostedGlassView)
            try {
                // 读取圆角半径属性，如果未设置则使用默认值
                val defaultRadius = 16f
                topLeftRadius = typedArray.getDimension(R.styleable.FrostedGlassView_topLeftRadius, defaultRadius)
                topRightRadius = typedArray.getDimension(R.styleable.FrostedGlassView_topRightRadius, defaultRadius)
                bottomLeftRadius = typedArray.getDimension(R.styleable.FrostedGlassView_bottomLeftRadius, defaultRadius)
                bottomRightRadius = typedArray.getDimension(R.styleable.FrostedGlassView_bottomRightRadius, defaultRadius)
                // 保持向后兼容：如果设置了cornerRadius，则使用它
                val cornerRadius = typedArray.getDimension(R.styleable.FrostedGlassView_cornerRadius, -1f)
                if (cornerRadius >= 0) {
                    setCornerRadius(cornerRadius)
                }
            } finally {
                typedArray.recycle()
            }
        }
        
        setupPaints()
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (!isInitialized) {
                    setupBlurEffect()
                    // 当布局准备就绪时，同时启动两个动画
                    startAnimations()
                    isInitialized = true
                }
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }
    
    /**
     * 设置单个角的圆角半径
     * @param topLeft 左上角圆角半径
     * @param topRight 右上角圆角半径
     * @param bottomLeft 左下角圆角半径
     * @param bottomRight 右下角圆角半径
     */
    fun setCornerRadii(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float) {
        this.topLeftRadius = topLeft
        this.topRightRadius = topRight
        this.bottomLeftRadius = bottomLeft
        this.bottomRightRadius = bottomRight
        markRadiiChanged()
    }
    
    /**
     * 设置左上角圆角半径
     * @param radius 圆角半径值
     */
    fun setTopLeftRadius(radius: Float) {
        this.topLeftRadius = radius
        markRadiiChanged()
    }
    
    /**
     * 设置右上角圆角半径
     * @param radius 圆角半径值
     */
    fun setTopRightRadius(radius: Float) {
        this.topRightRadius = radius
        markRadiiChanged()
    }
    
    /**
     * 设置左下角圆角半径
     * @param radius 圆角半径值
     */
    fun setBottomLeftRadius(radius: Float) {
        this.bottomLeftRadius = radius
        markRadiiChanged()
    }
    
    /**
     * 设置右下角圆角半径
     * @param radius 圆角半径值
     */
    fun setBottomRightRadius(radius: Float) {
        this.bottomRightRadius = radius
        markRadiiChanged()
    }
    
    /**
     * 标记圆角半径已更改，并触发重绘
     */
    private fun markRadiiChanged() {
        radiiChanged = true
        invalidate() // 触发重绘以应用新的圆角半径
    }
    
    /**
     * 获取左上角圆角半径
     * @return 左上角圆角半径值
     */
    fun getTopLeftRadius(): Float {
        return topLeftRadius
    }
    
    /**
     * 获取右上角圆角半径
     * @return 右上角圆角半径值
     */
    fun getTopRightRadius(): Float {
        return topRightRadius
    }
    
    /**
     * 获取左下角圆角半径
     * @return 左下角圆角半径值
     */
    fun getBottomLeftRadius(): Float {
        return bottomLeftRadius
    }
    
    /**
     * 获取右下角圆角半径
     * @return 右下角圆角半径值
     */
    fun getBottomRightRadius(): Float {
        return bottomRightRadius
    }

    private fun setupPaints() {
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = borderWidth
        // 设置默认边框颜色为10%透明度的白色
        borderPaint.color = Color.argb(25, 255, 255, 255) // 10%不透明度白色
        
        // 设置刷新效果画笔
        refreshPaint.style = Paint.Style.FILL
        refreshPaint.strokeWidth = 2f
        
        // 初始化背景画笔
        backgroundPaint.color = Color.argb(25, 0, 0, 0) // 10%黑色透明度
        
        // 初始化流光效果画笔
        lightPaint.style = Paint.Style.STROKE
        lightPaint.strokeWidth = borderWidth
    }

    private fun setupBlurEffect() {
        // 在dispatchDraw中处理模糊效果，而不是使用RenderEffect
        // 这样可以只模糊背景，不模糊子视图
    }

    private fun startAnimations() {
        // 同时启动两个动画，确保效果同步
        setupBorderAnimation()
        setupRefreshAnimation()
    }
    
    private fun setupBorderAnimation() {
        // 边框动画（流光效果）
        animator = ValueAnimator.ofFloat(0f, 1f)
        animator?.duration = 5000 // 恢复到原始持续时间
        animator?.repeatCount = ValueAnimator.INFINITE // 无限循环
        animator?.repeatMode = ValueAnimator.RESTART // 确保动画重复时正确重置
        animator?.interpolator = LinearInterpolator()
        animator?.addUpdateListener {
            val newProgress = it.animatedValue as Float
            progress = newProgress
            // 使用postInvalidateOnAnimation()代替invalidate()，提高动画流畅度
            ViewCompat.postInvalidateOnAnimation(this)
        }
        // 添加重复监听器，确保动画重复时也触发重绘
        animator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            
            override fun onAnimationEnd(animation: Animator) {}
            
            override fun onAnimationCancel(animation: Animator) {}
            
            override fun onAnimationRepeat(animation: Animator) {
                // 动画重复时触发重绘，确保流光效果持续可见
                ViewCompat.postInvalidateOnAnimation(this@FrostedGlassView)
            }
        })
        animator?.start()
    }
    
    private fun setupRefreshAnimation() {
        refreshAnimator = ValueAnimator.ofFloat(0f, 1f)
        refreshAnimator?.duration = 1000 // 缩短刷新动画持续时间，使其更流畅
        refreshAnimator?.repeatCount = 0 // 不无限重复
        refreshAnimator?.interpolator = LinearInterpolator()
        // 优化：设置动画更新监听器，减少不必要的更新
        refreshAnimator?.addUpdateListener {
            val newProgress = it.animatedValue as Float
            // 只有当进度变化超过一定阈值时才触发重绘
            if (Math.abs(newProgress - refreshProgress) > 0.02f) {
                refreshProgress = newProgress
                // 使用postInvalidateOnAnimation()代替invalidate()，提高动画流畅度
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }
        refreshAnimator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            
            override fun onAnimationEnd(animation: Animator) {
                // 延迟5秒后重新启动动画，确保无缝衔接
                // 使用postDelayed()确保在UI线程中执行，消除动画间隙
                postDelayed({
                    // 重置refreshProgress为0，确保动画从开始位置重新开始
                    refreshProgress = 0f
                    setupRefreshAnimation()
                }, 5000) // 5秒延迟
            }
            
            override fun onAnimationCancel(animation: Animator) {}
            
            override fun onAnimationRepeat(animation: Animator) {}
        })
        refreshAnimator?.start()
    }

    override fun dispatchDraw(canvas: Canvas) {
        // 只有在圆角半径变化时才更新缓存的圆角半径数组
        if (radiiChanged) {
            updateRadiiArray()
        }
        
        // 绘制10%黑色透明背景
        // 使用缓存的Path对象
        backgroundPath.reset()
        backgroundPath.addRoundRect(
            borderWidth, borderWidth, width - borderWidth, height - borderWidth,
            radii,
            Path.Direction.CW
        )
        canvas.drawPath(backgroundPath, backgroundPaint)

        // 绘制动态流光边框
        drawFlowingBorder(canvas)
        
        // 绘制刷新效果（左上角到右下角）
        drawRefreshEffect(canvas)

        super.dispatchDraw(canvas)
    }
    
    /**
     * 更新圆角半径数组
     */
    private fun updateRadiiArray() {
        radii[0] = topLeftRadius
        radii[1] = topLeftRadius
        radii[2] = topRightRadius
        radii[3] = topRightRadius
        radii[4] = bottomRightRadius
        radii[5] = bottomRightRadius
        radii[6] = bottomLeftRadius
        radii[7] = bottomLeftRadius
        radiiChanged = false
    }
    
    private fun drawRefreshEffect(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        
        // 缓存对角线长度和刷新宽度计算
        if (diagonalLength == 0f || refreshWidth == 0f) {
            diagonalLength = sqrt((width * width + height * height).toDouble()).toFloat()
            refreshWidth = diagonalLength / 6
        }
        
        // 根据refreshProgress计算位置
        val progress = refreshProgress
        
        // 计算当前位置：从左上角外到右下角外
        val startX = -refreshWidth
        val startY = -refreshWidth
        val endX = width + refreshWidth
        val endY = height + refreshWidth
        
        // 计算当前点
        val currentX = startX + (endX - startX) * progress
        val currentY = startY + (endY - startY) * progress
        
        // 只在尺寸变化时创建新的渐变对象
        if (refreshGradient == null || cachedWidth != width || cachedHeight != height) {
            refreshGradient = LinearGradient(
                -refreshWidth,
                -refreshWidth,
                refreshWidth,
                refreshWidth,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(100, 255, 255, 255), // 透明白色，更透明
                    Color.TRANSPARENT
                ),
                floatArrayOf(
                    0f,
                    0.5f,
                    1f
                ),
                Shader.TileMode.CLAMP
            )
        }
        
        // 使用矩阵变换来调整渐变位置
        refreshMatrix.reset()
        refreshMatrix.setTranslate(currentX, currentY)
        refreshGradient?.setLocalMatrix(refreshMatrix)
        
        refreshPaint.shader = refreshGradient
        
        // 只在刷新效果接近边缘时使用裁剪路径
        // 这样可以减少昂贵的裁剪操作次数
        // 使用缓存的矩形对象，避免每次都创建新对象
        refreshAreaRect.set(
            currentX - refreshWidth,
            currentY - refreshWidth,
            currentX + refreshWidth,
            currentY + refreshWidth
        )
        
        viewAreaRect.set(
            borderWidth,
            borderWidth,
            width - borderWidth,
            height - borderWidth
        )
        
        // 检查刷新区域是否与圆角边缘相交
        val intersectsWithEdges = !viewAreaRect.contains(refreshAreaRect)
        
        if (intersectsWithEdges) {
            // 当刷新效果接近边缘时，使用裁剪路径确保效果在圆角内
            clipPath.reset()
            clipPath.addRoundRect(
                borderWidth, borderWidth, width - borderWidth, height - borderWidth,
                radii,
                Path.Direction.CW
            )
            canvas.clipPath(clipPath)
        }
        
        // 绘制刷新效果
        canvas.drawRect(
            0f, 0f,
            width, height,
            refreshPaint
        )
    }

    private fun drawFlowingBorder(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        
        // 首先绘制默认边框（10%透明白色）
        // 这提供了基础边框
        borderPath.reset()
        borderPath.addRoundRect(
            borderWidth, borderWidth, width - borderWidth, height - borderWidth,
            radii,
            Path.Direction.CW
        )
        canvas.drawPath(borderPath, borderPaint)
        
        // 为流光效果创建更强烈的渐变
        // 只在尺寸变化时创建新的渐变
        if (borderGradient == null || cachedWidth != width || cachedHeight != height) {
            // 增强起点高亮效果
            borderGradient = LinearGradient(
                -width, -height,
                0f, 0f,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.WHITE, // 完全不透明的白色，确保最大可见度
                    Color.argb(200, 255, 255, 255), // 半透明白色，使起点更明显
                    Color.TRANSPARENT
                ),
                floatArrayOf(
                    0f,
                    0.3f, // 调整渐变分布，使高亮区域更集中
                    0.5f,
                    1f
                ),
                Shader.TileMode.CLAMP
            )
            cachedWidth = width
            cachedHeight = height
        }

        // 缓存半径计算，避免每次重绘都计算
        if (radius == 0f) {
            radius = sqrt((width / 2).toDouble() * (width / 2).toDouble() + (height / 2).toDouble() * (height / 2).toDouble()).toFloat()
        }
        
        // 根据进度计算位置
        val angle = progress * 360
        
        // 根据角度计算偏移
        val offsetX = (cos(Math.toRadians(angle.toDouble())) * radius).toFloat()
        val offsetY = (sin(Math.toRadians(angle.toDouble())) * radius).toFloat()
        
        // 使用缓存的矩阵对象
        matrix.reset()
        // 平移渐变以创建流动效果
        matrix.setTranslate(offsetX + width/2, offsetY + height/2)
        
        // 旋转渐变以跟随边框
        matrix.postRotate(angle + 45, width/2, height/2)
        
        borderGradient?.setLocalMatrix(matrix)
        
        // 复用缓存的lightPaint对象
        lightPaint.shader = borderGradient

        // 在基础边框上绘制流光效果
        canvas.drawPath(borderPath, lightPaint)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            val childLeft = (width - childWidth) / 2
            val childTop = (height - childHeight) / 2
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
        }
        
        // 当视图尺寸改变时，重置缓存的计算值
        if (changed) {
            diagonalLength = 0f
            refreshWidth = 0f
            radius = 0f
            borderGradient = null
            refreshGradient = null
            cachedWidth = 0f
            cachedHeight = 0f
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        refreshAnimator?.cancel()
    }
    
    override fun setWillNotDraw(willNotDraw: Boolean) {
        // 确保视图会被绘制，因为我们需要自定义绘制逻辑
        super.setWillNotDraw(false)
    }
    
    /**
     * 设置所有角的圆角半径（保持向后兼容）
     * @param radius 圆角半径值
     */
    fun setCornerRadius(radius: Float) {
        topLeftRadius = radius
        topRightRadius = radius
        bottomLeftRadius = radius
        bottomRightRadius = radius
        markRadiiChanged()
    }
    
    /**
     * 获取当前圆角半径（保持向后兼容）
     * @return 当前圆角半径值
     */
    fun getCornerRadius(): Float {
        return topLeftRadius // 返回左上角的圆角半径作为默认值
    }
}
