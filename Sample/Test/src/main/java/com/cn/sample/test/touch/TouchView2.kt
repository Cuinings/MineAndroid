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
class TouchView2: ConstraintLayout {

    companion object {
        val TAG = TouchView2::class.simpleName
    }

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        Log.d(TAG, "dispatchTouchEvent: $ev")
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        Log.d(TAG, "onInterceptTouchEvent: $ev")
        return false//super.onInterceptTouchEvent(ev)
    }

    override fun dispatchKeyEvent(ev: KeyEvent?): Boolean {
        Log.d(TAG, "dispatchKeyEvent: $ev")
        return super.dispatchKeyEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        Log.d(TAG, "onTouchEvent: $ev")
        return super.onTouchEvent(ev)
    }
}