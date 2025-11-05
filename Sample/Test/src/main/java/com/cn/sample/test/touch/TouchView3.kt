package com.cn.sample.test.touch

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * @Author: CuiNing
 * @Time: 2025/9/10 10:21
 * @Description:
 */
class TouchView3: ConstraintLayout {

    companion object {
        val TAG = TouchView3::class.simpleName
    }

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        Log.e(TAG, "dispatchTouchEvent: $ev")
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        Log.i(TAG, "onInterceptTouchEvent: $ev")
        return super.onInterceptTouchEvent(ev)
    }

    override fun dispatchKeyEvent(ev: KeyEvent?): Boolean {
        Log.e(TAG, "dispatchKeyEvent: $ev")
        return super.dispatchKeyEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        Log.d(TAG, "onTouchEvent: $ev")
        return when(ev?.action) {
            else -> true//super.onTouchEvent(ev)
        }
    }
}