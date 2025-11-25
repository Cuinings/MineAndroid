package com.cn.sample.test.child.textview.marquee

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import com.cn.library.common.activity.BasicVBActivity
import com.cn.library.common.textview.marquee.MarqueeTextView
import com.cn.sample.test.databinding.ActivityMarqueeBinding

/**
 * @Author: CuiNing
 * @Time: 2025/11/5 15:51
 * @Description:
 */
class MarqueeActivity : BasicVBActivity<ActivityMarqueeBinding>({ ActivityMarqueeBinding.inflate(it) }) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(binding) {
            // 设置动画完成监听器
            marqueeText.apply {
                layoutParams = layoutParams.apply {
                    height = width
                }
            }.setOnAnimationCompleteListener(object : MarqueeTextView.OnAnimationCompleteListener {
                @SuppressLint("SetTextI18n")
                override fun onAnimationComplete() {
                    loopCountText.text = "动画完成！总循环次数: ${marqueeText.getCurrentLoop()}"
                }
            })

            btnLeft.setOnClickListener {
                marqueeText.setDirection(MarqueeTextView.Direction.LEFT)
                updateLoopCount()
            }

            btnRight.setOnClickListener {
                marqueeText.setDirection(MarqueeTextView.Direction.RIGHT)
                updateLoopCount()
            }

            btnUp.setOnClickListener {
                marqueeText.setDirection(MarqueeTextView.Direction.UP)
                updateLoopCount()
            }

            btnDown.setOnClickListener {
                marqueeText.setDirection(MarqueeTextView.Direction.DOWN)
                updateLoopCount()
            }

            btnLoop3.setOnClickListener {
                marqueeText.setLoopCount(3)
                loopCountText.text = "设置循环次数: 3次"
            }

            btnLoop5.setOnClickListener {
                marqueeText.setLoopCount(5)
                loopCountText.text = "设置循环次数: 5次"
            }

            btnLoopInfinite.setOnClickListener {
                marqueeText.setLoopCount(ObjectAnimator.INFINITE)
                loopCountText.text = "设置循环次数: 无限循环"
            }

            btnStop.setOnClickListener {
                marqueeText.stopAnimation()
                loopCountText.text = "动画已停止"
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun ActivityMarqueeBinding.updateLoopCount() {
        loopCountText.text = "当前循环: ${marqueeText.getCurrentLoop()}"
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.marqueeText.stopAnimation()
    }

}