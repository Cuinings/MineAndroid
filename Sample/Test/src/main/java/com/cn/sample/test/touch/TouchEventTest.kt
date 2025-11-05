package com.cn.sample.test.touch

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import com.cn.sample.test.R

/**
 * @Author: CuiNing
 * @Time: 2025/9/10 10:19
 * @Description:
 */
class TouchEventTest: Activity() {

    companion object {
        val TAG = TouchEventTest::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_touch_event_test)
//        findViewById<TouchView1>(R.id.touchView1).setOnClickListener {  }
//        findViewById<TouchView2>(R.id.touchView2).setOnClickListener {  }
//        findViewById<TouchView3>(R.id.touchView3).setOnClickListener {  }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        Log.w(TAG, "dispatchTouchEvent: $ev")
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        Log.w(TAG, "onTouchEvent: $ev")
        return super.onTouchEvent(ev)
    }

}