package com.cn.core.ui.view.textview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withTranslation
import com.cn.core.ui.R
import kotlin.math.abs

class PagedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = 48f
        color = Color.BLACK
    }

    private var fullText = ""
    private var pages = mutableListOf<String>()
    private var currentPage = 0
    private var pageWidth = 0
    private var pageHeight = 0
    private var pageMargin = 50
    private var pagePadding = 40
    private var currentOffset = 0f
    private var targetOffset = 0f

    // 循环相关属性
    private var maxCycleCount = 0
    private var currentCycle = 0
    private var isCycleEnabled = false
    private var cycleDirection = 1

    // XML自定义属性
    private var textSize = 18f
    private var textColor = Color.BLACK

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val deltaX = e2.x - (e1?.x?:0f)
            if (abs(deltaX) > 100 && abs(velocityX) > 500) {
                if (deltaX > 0) {
                    showPreviousPage()
                    return true
                } else {
                    showNextPage()
                    return true
                }
            }
            return false
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val pageCenterX = width / 2f
            if (e.x < pageCenterX) {
                showPreviousPage()
                return true
            } else {
                showNextPage()
                return true
            }
        }
    })

    private var onCycleChangedListener: OnCycleChangedListener? = null

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.PagedTextView, defStyleAttr, 0) {
                textSize = getDimension(R.styleable.PagedTextView_textSize, 18f)
                textColor = getColor(R.styleable.PagedTextView_textColor, Color.BLACK)
                // 从XML属性设置初始文本（如果提供了）
                val textFromXml = getString(R.styleable.PagedTextView_android_text)
                if (!textFromXml.isNullOrEmpty()) {
                    fullText = textFromXml
                }
            }
        }

        // 应用XML属性到paint
        textPaint.textSize = textSize
        textPaint.color = textColor
    }

    fun setText(text: String) {
        fullText = text
        resetToFirstPage()
        calculatePages()
        invalidate()
    }

    fun setTextSize(size: Float) {
        textSize = size
        textPaint.textSize = size
        calculatePages()
        invalidate()
    }

    fun setTextColor(color: Int) {
        textColor = color
        textPaint.color = color
        invalidate()
    }

    fun enableCycleMode(maxCycles: Int = 0, direction: Int = 1) {
        isCycleEnabled = true
        maxCycleCount = maxCycles
        cycleDirection = direction
        currentCycle = 0
        invalidate()
    }

    fun disableCycleMode() {
        isCycleEnabled = false
        currentCycle = 0
        invalidate()
    }

    fun setMaxCycleCount(count: Int) {
        maxCycleCount = count
        invalidate()
    }

    fun getCurrentCycle(): Int = currentCycle
    fun getMaxCycleCount(): Int = maxCycleCount
    fun getCycleDirection(): Int = cycleDirection

    fun resetToFirstPage() {
        currentPage = 0
        currentCycle = 0
        currentOffset = 0f
        targetOffset = 0f
        invalidate()
    }

    fun showNextPage(): Boolean {
        if (currentPage < pages.size - 1) {
            currentPage++
            invalidate()
            return true
        } else if (isCycleEnabled && canContinueCycle()) {
            handleCycleTransition(true)
            return true
        }
        return false
    }

    fun showPreviousPage(): Boolean {
        if (currentPage > 0) {
            currentPage--
            invalidate()
            return true
        } else if (isCycleEnabled && canContinueCycle()) {
            handleCycleTransition(false)
            return true
        }
        return false
    }

    private fun canContinueCycle(): Boolean {
        return maxCycleCount == 0 || currentCycle < maxCycleCount
    }

    private fun handleCycleTransition(isNext: Boolean) {
        if (cycleDirection == 1) {
            if (isNext) {
                currentPage = 0
                currentCycle++
            } else {
                currentPage = pages.size - 1
                currentCycle++
            }
        } else {
            if (isNext) {
                currentPage = 0
                currentCycle--
            } else {
                currentPage = pages.size - 1
                currentCycle--
            }
        }
        invalidate()
        onCycleChangedListener?.onCycleChanged(currentCycle)
    }

    interface OnCycleChangedListener {
        fun onCycleChanged(cycleCount: Int)
    }

    fun setOnCycleChangedListener(listener: OnCycleChangedListener) {
        onCycleChangedListener = listener
    }

    fun getCurrentPage(): Int = currentPage
    fun getTotalPages(): Int = pages.size
    fun isCycleEnabled(): Boolean = isCycleEnabled

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculatePages()
    }

    private fun calculatePages() {
        if (width == 0 || height == 0 || fullText.isEmpty()) return

        pageWidth = width - 2 * pageMargin
        pageHeight = height - 2 * pageMargin - 100

        pages.clear()
        var startIndex = 0

        while (startIndex < fullText.length) {
            val layout = StaticLayout.Builder.obtain(
                fullText, startIndex, fullText.length, textPaint, pageWidth - 2 * pagePadding
            ).build()

            var lineCount = 0
            var endIndex = startIndex

            val maxLines = pageHeight / (textPaint.textSize * 1.2).toInt()

            while (lineCount < maxLines && endIndex < fullText.length) {
                val lineEnd = layout.getLineEnd(lineCount)
                if (lineEnd > endIndex) {
                    endIndex = lineEnd
                }
                lineCount++
                if (lineCount >= layout.lineCount) break
            }

            if (endIndex <= startIndex) break

            val pageText = fullText.substring(startIndex, endIndex).trim()
            if (pageText.isNotEmpty()) {
                pages.add(pageText)
            }
            startIndex = endIndex
        }

        if (pages.isEmpty() && fullText.isNotEmpty()) {
            pages.add(fullText)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (pages.isEmpty()) return

        canvas.drawColor(Color.WHITE)

        val pageLeft = pageMargin.toFloat()
        val pageTop = pageMargin.toFloat()
        val pageRight = pageLeft + pageWidth
        val pageBottom = pageTop + pageHeight

        canvas.withTranslation(currentOffset, 0f) {
            drawPage(
                this,
                pages[currentPage],
                pageLeft,
                pageTop,
                pageRight,
                pageBottom,
                currentPage
            )
        }

        /*if (isAnimating) {
            val otherPageOffset = if (targetOffset < 0) {
                width + currentOffset
            } else {
                -width + currentOffset
            }

            canvas.withTranslation(otherPageOffset, 0f) {
                val otherPage = if (targetOffset < 0) {
                    if (currentPage + 1 < pages.size) currentPage + 1 else currentPage
                } else {
                    if (currentPage - 1 >= 0) currentPage - 1 else currentPage
                }
                drawPage(
                    this,
                    pages[otherPage],
                    pageLeft,
                    pageTop,
                    pageRight,
                    pageBottom,
                    otherPage
                )
            }
        }*/
    }

    private fun drawPage(canvas: Canvas, text: String, left: Float, top: Float, right: Float, bottom: Float, pageNum: Int) {

        val textLeft = left + pagePadding
        val textTop = top + pagePadding + textPaint.textSize

        val layout = StaticLayout.Builder.obtain(
            text, 0, text.length, textPaint, (right - left - 2 * pagePadding).toInt()
        ).build()

        canvas.withTranslation(textLeft, textTop) {
            layout.draw(this)
        }
    }
}
