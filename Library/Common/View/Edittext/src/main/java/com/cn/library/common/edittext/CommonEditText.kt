package com.cn.library.common.edittext

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.text.InputType
import android.util.AttributeSet
import android.view.FocusFinder
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.isVisible

/**
 * @Author: CuiNing
 * @Time: 2025/4/8 15:32
 * @Description:
 */
@SuppressLint("AppCompatCustomView")
class CommonEditText: EditText {

    private data class KeyboardState(val isVisible: Boolean, val height: Int)

    private var inputMethodManager: InputMethodManager? = null

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) { initEditText() }

    @SuppressLint("PrivateApi", "UseCompatLoadingForDrawables", "ResourceType", "NewApi")
    private fun initEditText() {
        gravity = Gravity.CENTER_VERTICAL
        background = resources.getDrawable(R.drawable.edit_text_background, null)
        setHintTextColor(resources.getColorStateList(R.drawable.edit_text_hilt_text_color, null))
        setTextColor(resources.getColorStateList(R.drawable.edit_text_text_color, null))
        maxLines = 1
        isSingleLine = true
        resources.getDimensionPixelOffset(R.dimen.dp5).let { setPadding(it, 0, it, 0) }

        setTextCursorDrawable(R.drawable.edit_text_cursor_color)
        setTextSelectHandleLeft(R.drawable.icon_text_selected_left)
        setTextSelectHandleRight(R.drawable.icon_text_selected_right)
        setTextSelectHandle(R.drawable.icon_text_selected_middle)
        inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        setOnClickListener {
            inputType = InputType.TYPE_CLASS_TEXT
            inputMethodManager?.showSoftInput(this, 0)
        }
        /*
        //部分场景需使用此设置
        try {
            val mCursorDrawableRes = TextView::class.java.getDeclaredField("mCursorDrawableRes")
            mCursorDrawableRes.isAccessible = true
            mCursorDrawableRes[this] = R.drawable.edit_text_cursor_color
            Log.d(CommonEditText::class.simpleName, "initEditText: mCursorDrawableRes -> ${R.drawable.edit_text_cursor_color}, ${mCursorDrawableRes[this]}")

            val mTextSelectHandleLeftRes = TextView::class.java.getDeclaredField("mTextSelectHandleLeftRes")
            mTextSelectHandleLeftRes.isAccessible = true
            mTextSelectHandleLeftRes[this] = R.drawable.icon_text_selected_left
            Log.d(CommonEditText::class.simpleName, "initEditText: mTextSelectHandleLeftRes -> ${R.drawable.icon_text_selected_left}, ${mTextSelectHandleLeftRes[this]}")

            val mTextSelectHandleRightRes = TextView::class.java.getDeclaredField("mTextSelectHandleRightRes")
            mTextSelectHandleRightRes.isAccessible = true
            mTextSelectHandleRightRes[this] = R.drawable.icon_text_selected_right
            Log.d(CommonEditText::class.simpleName, "initEditText: mTextSelectHandleRightRes -> ${R.drawable.icon_text_selected_right}, ${mTextSelectHandleRightRes[this]}")

            val mTextSelectHandleRes = TextView::class.java.getDeclaredField("mTextSelectHandleRes")
            mTextSelectHandleRes.isAccessible = true
            mTextSelectHandleRes[this] = R.drawable.icon_text_selected_middle
            Log.d(CommonEditText::class.simpleName, "initEditText: mTextSelectHandleRes -> ${R.drawable.icon_text_selected_middle}, ${mTextSelectHandleRes[this]}")
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        */
    }

    private val windowVisibleDisplayFrame = Rect()
    private var lastVisibleHeight = 0

    private var keyboardState: KeyboardState? = null
        set(value) { value.takeIf { it?.isVisible != field?.isVisible }?.let {
            field = it
        } }

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        rootView.getWindowVisibleDisplayFrame(windowVisibleDisplayFrame)
        val visibleHeight = windowVisibleDisplayFrame.height()

        if (lastVisibleHeight == 0) {
            lastVisibleHeight = visibleHeight
            return@OnGlobalLayoutListener
        }

        val heightDiff = rootView.height - visibleHeight
        val isKeyboardVisible = heightDiff > rootView.height * 0.15

        // 计算键盘高度
        val keyboardHeight = if (isKeyboardVisible) heightDiff else 0

        // 检测状态变化
        keyboardState = KeyboardState(isKeyboardVisible, keyboardHeight)

        lastVisibleHeight = visibleHeight
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
    }

    override fun onFocusChanged(
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        focused.takeIf { !it }?.let {
            inputType = InputType.TYPE_NULL
            inputMethodManager?.hideSoftInputFromWindow(this.windowToken, 0)
        }
    }

    fun showInputSoft(): Boolean? = this.apply {
        inputType = InputType.TYPE_CLASS_TEXT
    }.let { inputMethodManager?.showSoftInput(this, 0) }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isDirectKeyCode(keyCode)) {
            var direction = when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> FOCUS_UP
                KeyEvent.KEYCODE_DPAD_DOWN -> FOCUS_DOWN
                KeyEvent.KEYCODE_DPAD_LEFT -> FOCUS_LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> FOCUS_RIGHT
                else -> FOCUS_DOWN
            }
            return findNextFocusView(direction, this, rootView)?.let {
                it.requestFocus()
                true
            }?: false//super.onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }/**
     * 查找下一个焦点视图
     * @param direction 焦点方向 (View.FOCUS_UP, View.FOCUS_DOWN 等)
     * @param currentView 当前拥有焦点的视图
     * @param rootView 根视图
     * @return 下一个焦点视图，找不到时返回null
     */
    private fun findNextFocusView(
        direction: Int,
        currentView: View,
        rootView: View,
    ): View? {
        // 1. 尝试通过预定义ID查找
        val predefinedId = when (direction) {
            FOCUS_UP -> currentView.nextFocusUpId
            FOCUS_DOWN -> currentView.nextFocusDownId
            FOCUS_LEFT -> currentView.nextFocusLeftId
            FOCUS_RIGHT -> currentView.nextFocusRightId
            else -> NO_ID
        }

        // 检查预定义ID是否有效
        if (predefinedId != NO_ID) {
            rootView.findViewById<View>(predefinedId)?.let { view ->
                if (isFocusable(view)) return view
            }
        }

        // 2. 使用FocusFinder在父视图中查找
        val parent = currentView.parent as? ViewGroup
        parent?.let {
            FocusFinder.getInstance().findNextFocus(it, currentView, direction)?.let { view ->
                if (isFocusable(view)) return view
            }
        }

//        takeIf { direction == FOCUS_RIGHT }?.let {
//            rootView.findViewById<View>(inputNoClearRightFocusId)?.let { view ->
//                if (isFocusable(view)) return view
//            }
//        }

        // 3. 在整个根视图中查找备选焦点
        return findAlternativeFocus(rootView, currentView, direction)
    }

    /**
     * 检查视图是否可获取焦点
     */
    private fun isFocusable(view: View): Boolean {
        return view.isVisible &&
                view.isFocusable &&
                !view.isInTouchMode // 确保非触摸模式下
    }

    /**
     * 备选焦点查找策略
     */
    private fun findAlternativeFocus(
        rootView: View,
        currentView: View,
        direction: Int,
    ): View? {
        // 策略1: 查找同类型控件
        if (currentView is EditText) {
            rootView.findViewsWithText { view ->
                view is EditText && view != currentView && isFocusable(view)
            }.firstOrNull()?.let { return it }
        }

        // 策略2: 按方向查找最近的控件
        val allFocusableViews = rootView.getFocusables(FOCUS_FORWARD)
            .filter { it != currentView && isFocusable(it) }

        return when (direction) {
            FOCUS_DOWN -> allFocusableViews.minByOrNull { it.y }
            FOCUS_UP -> allFocusableViews.maxByOrNull { it.y }
            FOCUS_RIGHT -> allFocusableViews.minByOrNull { it.x }
            FOCUS_LEFT -> allFocusableViews.maxByOrNull { it.x }
            else -> null
        }
    }


    // 扩展函数：查找包含特定文本的视图
    private fun View.findViewsWithText(
        filter: (View) -> Boolean,
    ): List<View> {
        val result = mutableListOf<View>()
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                result.addAll(child.findViewsWithText(filter))
            }
        }
        if (filter(this)) result.add(this)
        return result
    }

    private fun isDirectKeyCode(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
    }
}