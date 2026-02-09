package com.cn.other.test.blur

import android.os.Bundle
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.view.WindowManager
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.cn.other.test.R


/**
 * @author: cn
 * @time: 2026/2/9 10:05
 * @history
 * @description:
 */
class BlurActivity: AppCompatActivity() {

    private var blurSurfaceView: BlurSurfaceView? = null
    private var underlyingScrollView: ScrollView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blur)

        blurSurfaceView = findViewById<BlurSurfaceView?>(R.id.blurSurfaceView)
        underlyingScrollView = findViewById<ScrollView?>(R.id.underlyingScrollView)


        // 设置滚动监听，实时更新模糊效果
        underlyingScrollView?.getViewTreeObserver()?.addOnScrollChangedListener(object : OnScrollChangedListener {
            override fun onScrollChanged() {
                blurSurfaceView?.updateBlurSource(underlyingScrollView)
            }
        })


        // 初始更新
        blurSurfaceView?.postDelayed(object : Runnable {
            override fun run() {
                blurSurfaceView?.updateBlurSource(underlyingScrollView)
            }
        }, 500)
    }

    override fun onResume() {
        super.onResume()
        blurSurfaceView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        blurSurfaceView?.onPause()
    }
}