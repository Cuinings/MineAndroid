package com.cn.library.common.edittext

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.FocusFinder
import android.view.KeyEvent
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

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        initEditText()
    }

    @SuppressLint("PrivateApi")
    private fun initEditText() {
        try {
            val f = TextView::class.java.getDeclaredField("mCursorDrawableRes")
            f.isAccessible = true
            f[this] = R.drawable.et_cursor_cololr

            val mTextSelectHandleLeftRes = TextView::class.java.getDeclaredField("mTextSelectHandleLeftRes")
            mTextSelectHandleLeftRes.isAccessible = true
            mTextSelectHandleLeftRes[this] = R.drawable.icon_text_selected_left

            val mTextSelectHandleRightRes = TextView::class.java.getDeclaredField("mTextSelectHandleRightRes")
            mTextSelectHandleRightRes.isAccessible = true
            mTextSelectHandleRightRes[this] = R.drawable.icon_text_selected_right

            val mTextSelectHandleRes = TextView::class.java.getDeclaredField("mTextSelectHandleRes")
            mTextSelectHandleRes.isAccessible = true
            mTextSelectHandleRes[this] = R.drawable.icon_text_selected_middle
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
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