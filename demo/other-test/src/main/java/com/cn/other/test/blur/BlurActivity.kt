package com.cn.other.test.blur

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.WindowManager
import android.widget.TextView
import com.cn.other.test.R
import java.util.function.Consumer


/**
 * @author: cn
 * @time: 2026/4/10 16:16
 * @history
 * @description:
 */
class BlurActivity : Activity() {
    //窗口背景高斯模糊程度，数值越高越模糊且越消耗性能
    private val mBackgroundBlurRadius = 10

    //窗口周边背景高斯模糊程度
    private val mBlurBehindRadius = 10

    //根据窗口高斯模糊功能是否开启来设置窗口周边暗色的程度
    private val mDimAmountWithBlur = 0f
    private val mDimAmountNoBlur = 0.4f

    // 根据窗口高斯模糊功能是否开启来为窗口设置不同的不透明度
    private val mWindowBackgroundAlphaWithBlur = 100
    private val mWindowBackgroundAlphaNoBlur = 255

    //使用一个矩形drawable文件作为窗口背景，这个矩形的轮廓和圆角确定了窗口高斯模糊的区域
    private var mWindowBackgroundDrawable: Drawable? = null

    /**
     * 高斯模糊的类型
     * 0代表只模糊背景
     * 1代表之模糊后方屏幕
     * 2代表同时模糊背景和后方屏幕
     */
    private var mBlurType = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBlurType = intent.getIntExtra(EXTRA_KEY_BLUR_TYPE, BLUR_TYPE_BLUR_BACKGROUND)
        setContentView(R.layout.activity_blur)
        initBlur()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initBlur() {
        //替换window默认的背景
        mWindowBackgroundDrawable = getDrawable(R.drawable.window_background)
        findViewById<TextView>(R.id.textview).background = mWindowBackgroundDrawable

        //注册一个监听者去监听窗口UI视图是否可见以便调整窗口高斯模糊功能是否开启
        setupWindowBlurListener()

        //允许背景模糊，也可以通过样式属性R.attr#windowBlurBehindEnabled来实现
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)

        // 允许背景变暗，也可以通过样式属性R.attr#backgroundDimEnabled来实现
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        findViewById<View?>(R.id.ll_content).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                finish()
            }
        })
    }

    /**
     * 设置一个窗口视图状态监听者，监听窗口视图是否可见以便是否更新窗口模糊的状态
     */
    private fun setupWindowBlurListener() {
        val windowBlurEnabledListener: Consumer<Boolean?> = Consumer { blursEnabled: Boolean ->
            this.updateWindowForBlurs(blursEnabled)
        }
        window.decorView.addOnAttachStateChangeListener(
            object : OnAttachStateChangeListener {
                @SuppressLint("NewApi")
                override fun onViewAttachedToWindow(v: View) {
                    windowManager.addCrossWindowBlurEnabledListener(windowBlurEnabledListener)
                }

                @SuppressLint("NewApi")
                override fun onViewDetachedFromWindow(v: View) {
                    windowManager.removeCrossWindowBlurEnabledListener(
                        windowBlurEnabledListener
                    )
                }
            }
        )
    }

    /**
     * 更新窗口的高斯模糊效果
     *
     * @param blursEnabled
     */
    @SuppressLint("NewApi")
    private fun updateWindowForBlurs(blursEnabled: Boolean) {
        Log.d(BlurActivity::class.simpleName, "updateWindowForBlurs: $blursEnabled, $mBlurType")
        if (mBlurType == BLUR_TYPE_BLUR_BACKGROUND) {
            //仅模糊背景
            mWindowBackgroundDrawable?.alpha = if (blursEnabled) mWindowBackgroundAlphaWithBlur else mWindowBackgroundAlphaNoBlur //调整背景的透明度
            window.setDimAmount(if (blursEnabled) mDimAmountWithBlur else mDimAmountNoBlur) //调整背景周边昏暗的程度
            window.setBackgroundBlurRadius(mBackgroundBlurRadius) //设置背景模糊程度
            return
        }
        if (mBlurType == BLUR_TYPE_BLUR_BEHIND) {
            //仅模糊后方屏幕
            window.setDimAmount(if (blursEnabled) mDimAmountWithBlur else mDimAmountNoBlur) //调整背景周边昏暗的程度
            window.attributes.blurBehindRadius = mBlurBehindRadius //设置背景周边模糊程度
            window.setAttributes(window.attributes) //让上面的高斯模糊效果生效
            return
        }
        //同时模糊背景和后方屏幕
        mWindowBackgroundDrawable?.alpha = if (blursEnabled) mWindowBackgroundAlphaWithBlur else mWindowBackgroundAlphaNoBlur //调整背景的透明度
        window.setBackgroundBlurRadius(mBackgroundBlurRadius) //设置背景模糊程度
        window.setDimAmount(if (blursEnabled) mDimAmountWithBlur else mDimAmountNoBlur) //调整背景周边昏暗的程度
        window.attributes.blurBehindRadius = mBlurBehindRadius //设置背景周边模糊程度
        window.setAttributes(window.attributes) //让上面的高斯模糊效果生效
    }

    companion object {
        const val EXTRA_KEY_BLUR_TYPE: String = "blur_type"
        const val BLUR_TYPE_BLUR_BACKGROUND: Int = 0
        const val BLUR_TYPE_BLUR_BEHIND: Int = 1
        const val BLUR_TYPE_BLUR_BACKGROUND_AND_BEHIND: Int = 2
    }
}

