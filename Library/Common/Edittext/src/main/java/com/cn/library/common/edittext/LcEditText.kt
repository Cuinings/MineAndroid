package com.cn.library.common.edittext

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
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
}