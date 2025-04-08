package com.cn.library.common.edittext

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.FocusFinder
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView

/**
 * @Author: CuiNing
 * @Time: 2025/4/8 15:32
 * @Description:
 */
@SuppressLint("AppCompatCustomView")
class LcEditText: EditText {

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
        /*
        //部分场景需使用此设置
        try {
            val mCursorDrawableRes = TextView::class.java.getDeclaredField("mCursorDrawableRes")
            mCursorDrawableRes.isAccessible = true
            mCursorDrawableRes[this] = R.drawable.edit_text_cursor_color
            Log.d(LcEditText::class.simpleName, "initEditText: mCursorDrawableRes -> ${R.drawable.edit_text_cursor_color}, ${mCursorDrawableRes[this]}")

            val mTextSelectHandleLeftRes = TextView::class.java.getDeclaredField("mTextSelectHandleLeftRes")
            mTextSelectHandleLeftRes.isAccessible = true
            mTextSelectHandleLeftRes[this] = R.drawable.icon_text_selected_left
            Log.d(LcEditText::class.simpleName, "initEditText: mTextSelectHandleLeftRes -> ${R.drawable.icon_text_selected_left}, ${mTextSelectHandleLeftRes[this]}")

            val mTextSelectHandleRightRes = TextView::class.java.getDeclaredField("mTextSelectHandleRightRes")
            mTextSelectHandleRightRes.isAccessible = true
            mTextSelectHandleRightRes[this] = R.drawable.icon_text_selected_right
            Log.d(LcEditText::class.simpleName, "initEditText: mTextSelectHandleRightRes -> ${R.drawable.icon_text_selected_right}, ${mTextSelectHandleRightRes[this]}")

            val mTextSelectHandleRes = TextView::class.java.getDeclaredField("mTextSelectHandleRes")
            mTextSelectHandleRes.isAccessible = true
            mTextSelectHandleRes[this] = R.drawable.icon_text_selected_middle
            Log.d(LcEditText::class.simpleName, "initEditText: mTextSelectHandleRes -> ${R.drawable.icon_text_selected_middle}, ${mTextSelectHandleRes[this]}")
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        */
    }

    fun requestFocusWithInput() {
        postDelayed({
            requestFocus()
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(this, 0)
        }, 300)
    }

    private fun isDirectKeyCode(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isDirectKeyCode(keyCode)) {
            var direction = FOCUS_DOWN
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> direction = FOCUS_UP
                KeyEvent.KEYCODE_DPAD_DOWN -> direction = FOCUS_DOWN
                KeyEvent.KEYCODE_DPAD_LEFT -> direction = FOCUS_LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> direction = FOCUS_RIGHT
            }
            val nextFocusView = FocusFinder.getInstance().findNextFocus(
                parent as ViewGroup,
                this, direction
            )
            if (null != nextFocusView) {
                nextFocusView.requestFocus()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}