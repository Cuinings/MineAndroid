package com.cn.board.home.widget

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.abs

/**
 * 自定义全屏滑动切换容器（不使用 ViewPager / ViewPager2 / RecyclerView）。
 *
 * 用法：把要切换的全屏页面（如两个 FragmentContainerView）作为本 View 的直接子节点即可。
 * 本组件会把它们**叠放在同一位置 (0,0)**，并通过 [View.setTranslationX] 施加整体平移
 * （第 i 页 translationX = i * pageWidth - offsetPx）形成整屏滑动；松手按位移/速度吸附到目标页。
 *
 * 与内部横向滚动的共存：
 *  - 若当前页内部有可横向滚动的内容（如横向 RecyclerView），手势拦截仅在
 *    「明显横向 且 当前页内部在该方向已到边缘（无法再滚动）」时接管翻页，
 *    否则把事件让给内部控件，避免「滑列表变成翻页」。
 *  - 因此本组件**不重载** requestDisallowInterceptTouchEvent，尊重子 View 的 disallow 请求。
 *
 * 焦点（DPad / 遥控器）左右切换：
 *  - 重写 [dispatchKeyEvent]：先让系统处理焦点移动（super）；当焦点已在该页横向边缘、
 *    系统未消费方向键时，再翻到上一页 / 下一页，并把焦点移入新页。
 *  - 离屏页在静止时被设为 INVISIBLE，既省绘制也阻断其抢焦点。
 */
class SwipeFragmentPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    /** 参与滑动切换的页面数量（直接子 View 数量，最多 2 个）。 */
    private val pageCount: Int get() = childCount.coerceAtMost(MAX_PAGE)

    /** 当前所在页（0 基）。 */
    var currentPage: Int = 0
        private set

    /** 页面切换回调（参数：切换完成后的目标页 index）。 */
    var onPageChangeListener: ((index: Int) -> Unit)? = null

    /** 横向偏移量（px）。0 表示完全停留在第 0 页，pageWidth 表示完全在第 1 页。 */
    private var offsetPx = 0f

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maxFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity

    private val switchAnimator = ValueAnimator().apply {
        duration = SWITCH_DURATION.toLong()
        interpolator = DecelerateInterpolator()
        addUpdateListener { offsetPx = it.animatedValue as Float; applyOffset() }
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {
                focusCurrentPage()
            }
            override fun onAnimationEnd(animation: android.animation.Animator) {
                focusCurrentPage()
            }
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
    }

    // 手势跟踪状态
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var downX = 0f
    private var downY = 0f
    private var startOffset = 0f
    private var decided = false          // 是否已判断过横/纵方向
    private var interceptHoriz = false   // 本次手势是否判定为横向（需要本组件消费）
    private var velocityTracker: VelocityTracker? = null

    private var firstLayoutDone = false

    /** 是否正在拖拽（用于 onLayout 时判断是否可以安全取消动画）。 */
    private var isDragging = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = measuredWidth
        val h = measuredHeight
        if (w == 0 || h == 0) return
        // 每个页面都按整屏尺寸测量（叠放，所以都是同样的整屏尺寸）
        val childW = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY)
        val childH = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) measureChild(child, childW, childH)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val w = width
        val h = height
        if (w == 0 || h == 0) return
        // 所有子 View 都铺满整屏、叠放在 (0,0)。真正的「分页位移」靠 translationX 完成。
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            child.layout(0, 0, w, h)
        }
        // 布局变化后按当前 offset 重新摆放。
        // 关键：动画或拖拽进行中不要取消动画，否则回弹/拖拽被布局变化（如内部列表刷新、
        // RecyclerView 尺寸变化）打断后会「卡在半屏」无法归位——只按当前 offsetPx 同步一次平移。
        if (switchAnimator.isRunning || isDragging) {
            applyOffset()
        } else {
            switchAnimator.cancel()
            applyOffset()
        }
        if (!firstLayoutDone) {
            firstLayoutDone = true
            // 首帧后把焦点放进当前页，避免全屏无焦点导致方向键失效
            post { focusCurrentPage() }
        }
    }

    /** 依据当前 [offsetPx] 把每个子 View 平移到对应位置，并管理离屏页可见性。 */
    private fun applyOffset() {
        val w = width
        if (w == 0) return
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val tx = i * w - offsetPx
            child.translationX = tx
            // 仅在「部分可见」时 VISIBLE；完全离屏则 INVISIBLE（省绘制 + 阻断焦点抢走）
            val visible = tx > -w && tx < w
            child.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        }
    }

    /** 最大可偏移量（px）。 */
    private val maxOffset: Float get() = (pageCount - 1).coerceAtLeast(0) * width.toFloat()

    /**
     * 边缘 overscroll 阻尼（橡皮筋效果）。
     * 在可切换范围内是 1:1 跟手；超出边界（首/尾页继续往无法滚动的方向滑）时，
     * 超出部分按非线性比例压缩（越拉越紧），产生「拉不动」的阻尼感，松手由回弹动画归位。
     */
    private fun applyResistance(raw: Float): Float {
        val min = 0f
        val max = maxOffset
        if (raw in min..max) return raw           // 范围内：原样跟手
        val over = if (raw < min) raw - min else raw - max
        // 阻尼距离随屏幕宽度自适应：拉到约 0.4*width 时阻尼比例降到约一半
        val dist = (width * 0.4f).coerceAtLeast(1f)
        val damped = over * RESISTANCE_RATIO / (1f + abs(over) / dist)
        return if (raw < min) min + damped else max + damped
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val idx = ev.actionIndex
                activePointerId = ev.getPointerId(idx)
                downX = ev.getX(idx)
                downY = ev.getY(idx)
                startOffset = offsetPx
                decided = false
                interceptHoriz = false
                isDragging = false
                switchAnimator.cancel()
                initVelocity()
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = ev.findPointerIndex(activePointerId)
                if (idx < 0) return false
                if (!decided) {
                    val dx = ev.getX(idx) - downX
                    val dy = ev.getY(idx) - downY
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        decided = true
                        val horiz = abs(dx) > abs(dy)
                        if (horiz) {
                            // dx > 0：手指向右 → 想去上一页，且内部应可向左滚动（direction = -1）
                            // dx < 0：手指向左 → 想去下一页，且内部应可向右滚动（direction = +1）
                            // 首/尾页在边缘方向（内部已无法再滚动）也由本组件接管，
                            // 以便施加 overscroll 阻尼；中间页则先看内部是否还能滚动。
                            interceptHoriz = if (dx > 0) {
                                if (currentPage > 0) !innerCanScrollHorizontally(-1) else true
                            } else {
                                if (currentPage < pageCount - 1) !innerCanScrollHorizontally(1) else true
                            }
                        } else {
                            interceptHoriz = false
                        }
                    }
                }
                if (decided && interceptHoriz) {
                    isDragging = true
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                decided = false
                interceptHoriz = false
                isDragging = false
            }
        }
        return false
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val idx = ev.actionIndex
                activePointerId = ev.getPointerId(idx)
                downX = ev.getX(idx)
                downY = ev.getY(idx)
                startOffset = offsetPx
                decided = false
                interceptHoriz = true
                isDragging = true
                switchAnimator.cancel()
                initVelocity()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = ev.findPointerIndex(activePointerId)
                if (idx < 0) return false
                trackVelocity(ev)
                val x = ev.getX(idx)
                val y = ev.getY(idx)
                if (!decided) {
                    val dx = x - downX
                    val dy = y - downY
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        decided = true
                        interceptHoriz = abs(dx) > abs(dy)
                    }
                }
                if (interceptHoriz) {
                    // 用「相对按下点的累计位移」计算，避免逐帧增量导致不累加 / 抖动；
                    // 经 applyResistance 施加边缘 overscroll 阻尼（范围外越拉越紧）
                    val totalDx = x - downX
                    offsetPx = applyResistance(startOffset - totalDx)
                    applyOffset()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (interceptHoriz) {
                    trackVelocity(ev)
                    velocityTracker?.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                    val vx = velocityTracker?.xVelocity ?: 0f
                    recycleVelocity()
                    settlePage(vx)
                }
                activePointerId = MotionEvent.INVALID_POINTER_ID
                decided = false
                interceptHoriz = false
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(ev)
    }

    /**
     * 依据松手时的位置与速度决定目标页。
     *
     * 惯性加成：将速度映射为一段"额外惯性位移"（vx * FLING_FRICTION），加到当前 offsetPx 上
     * 得到投影位置 [projected]，再与降低后的阈值（35% 页宽）比较。快滑时即使手指位移没过半，
     * 惯性加成也能推过阈值触发翻页；慢滑时则依赖实际位移判定。
     *
     * Overscroll 回弹：offsetPx 超出 [0, maxOffset] 时快速回弹（200ms + Overshoot 弹性）。
     */
    private fun settlePage(vx: Float) {
        val pageW = width
        if (pageW <= 0) return
        // 速度惯性：vx < 0 为左滑（去下一页），flingBoost > 0 表示向下一页压入
        val flingBoost = -vx * FLING_FRICTION
        val projected = (offsetPx + flingBoost).coerceIn(-pageW.toFloat(), maxOffset + pageW.toFloat())
        val threshold = pageW * SETTLE_THRESHOLD

        val target = when {
            projected > currentPage * pageW + threshold -> (currentPage + 1).coerceAtMost(pageCount - 1)
            projected < currentPage * pageW - threshold -> (currentPage - 1).coerceAtLeast(0)
            else -> currentPage
        }

        val targetOffset = target * pageW.toFloat()
        val distance = abs(targetOffset - offsetPx)
        val isOverscroll = target == currentPage && (offsetPx < 0f || offsetPx > maxOffset)

        // 动画时长动态：距离大 → 适当延长，距离小 → 快速到位；overscroll 回弹更快
        val duration = if (isOverscroll) {
            200L
        } else {
            (distance / pageW * SWITCH_DURATION).toLong().coerceIn(120L, 420L)
        }

        val interpolator = if (isOverscroll) OvershootInterpolator(1.5f) else DecelerateInterpolator()
        smoothScrollToPage(target, duration, interpolator)
    }

    private fun smoothScrollToPage(
        page: Int,
        durationMs: Long = SWITCH_DURATION.toLong(),
        interpolator: android.animation.TimeInterpolator = DecelerateInterpolator()
    ) {
        val target = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        val prevPage = currentPage
        currentPage = target
        if (target != prevPage) onPageChangeListener?.invoke(target)
        val targetOffset = target * width.toFloat()
        if (targetOffset == offsetPx) {
            focusCurrentPage()
            return
        }
        switchAnimator.duration = durationMs
        switchAnimator.interpolator = interpolator
        switchAnimator.setFloatValues(offsetPx, targetOffset)
        switchAnimator.start()
    }

    // ------------------------------------------------------------------
    //  对外 API：供方向键 / 按钮 / 外部逻辑直接切换页面
    // ------------------------------------------------------------------
    fun showPage(page: Int, smooth: Boolean = true) {
        val target = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        if (target == currentPage && offsetPx == target * width.toFloat()) return
        if (smooth && width > 0) {
            switchAnimator.cancel()
            smoothScrollToPage(target)
        } else {
            currentPage = target
            onPageChangeListener?.invoke(target)
            offsetPx = target * width.toFloat()
            applyOffset()
            focusCurrentPage()
        }
    }

    fun nextPage() = showPage(currentPage + 1)
    fun prevPage() = showPage(currentPage - 1)

    // ------------------------------------------------------------------
    //  焦点（DPad / 遥控器）左右切换
    //  先让系统处理焦点移动；焦点已到该页横向边缘、系统未消费方向键时，再翻页。
    // ------------------------------------------------------------------
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (super.dispatchKeyEvent(event)) return true
        // super 未消费：说明当前页内焦点已无法再向该方向移动（或无可聚焦项）→ 尝试翻页
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (currentPage > 0) {
                        prevPage()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (currentPage < pageCount - 1) {
                        nextPage()
                        return true
                    }
                }
            }
        }
        return false
    }

    /** 把焦点移入当前页（离屏页已被设 INVISIBLE，不会抢焦点）。 */
    private fun focusCurrentPage() {
        val child = getChildAt(currentPage) ?: return
        if (child.visibility != View.VISIBLE) child.visibility = View.VISIBLE
        if (child.findFocus() != null) return
        child.requestFocus()
    }

    /**
     * 当前页内部是否还能在 [direction] 方向上横向滚动。
     * direction < 0 表示向左（显示左侧内容），> 0 表示向右。
     */
    private fun innerCanScrollHorizontally(direction: Int): Boolean {
        val child = getChildAt(currentPage) ?: return false
        return canScrollHorizontallyRec(child, direction)
    }

    private fun canScrollHorizontallyRec(v: View, direction: Int): Boolean {
        if (v.canScrollHorizontally(direction)) return true
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                if (canScrollHorizontallyRec(v.getChildAt(i), direction)) return true
            }
        }
        return false
    }

    // ------------------------------------------------------------------
    //  速度跟踪
    // ------------------------------------------------------------------
    private fun initVelocity() {
        velocityTracker?.clear() ?: run { velocityTracker = VelocityTracker.obtain() }
    }

    private fun trackVelocity(ev: MotionEvent) {
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain()
        velocityTracker?.addMovement(ev)
    }

    private fun recycleVelocity() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    // ------------------------------------------------------------------
    //  旋转 / 进程重建后保存当前页
    // ------------------------------------------------------------------
    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState() ?: return SavedState(null, currentPage)
        return SavedState(superState, currentPage)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            currentPage = state.page
            offsetPx = currentPage * width.toFloat()
            // onLayout 里会按 offsetPx 重新 applyOffset
            requestLayout()
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private class SavedState(
        val superState: Parcelable?,
        val page: Int
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readParcelable(Parcelable::class.java.classLoader),
            parcel.readInt()
        )

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeParcelable(superState, flags)
            dest.writeInt(page)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }

    companion object {
        private const val MAX_PAGE = 2
        private const val SWITCH_DURATION = 300
        /** 边缘 overscroll 阻尼系数：越小越「拉不动」。0.4 = 拉到约 0.4*width 时位移只剩约一半。 */
        private const val RESISTANCE_RATIO = 0.4f
        /** 速度惯性衰减系数（秒）。值越大松手后惯性滑动越远、翻页越灵敏。 */
        private const val FLING_FRICTION = 0.22f
        /** 切页阈值（页宽比例）。0.35 表示滑过 35% 即触发切页（含惯性加成）。 */
        private const val SETTLE_THRESHOLD = 0.35f
    }
}
