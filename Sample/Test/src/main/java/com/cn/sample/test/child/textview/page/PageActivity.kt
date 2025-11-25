package com.cn.sample.test.child.textview.page

import android.annotation.SuppressLint
import android.os.Bundle
import com.cn.library.common.activity.BasicVBActivity
import com.cn.library.common.textview.page.PagedTextView
import com.cn.sample.test.databinding.ActivityPageBinding

/**
 * @Author: CuiNing
 * @Time: 2025/11/5 15:51
 * @Description:
 */
class PageActivity : BasicVBActivity<ActivityPageBinding>({ ActivityPageBinding.inflate(it) }) {

    private val sampleText = """
        第一页：这是使用XML布局方式实现的PagedTextView示例。通过XML可以更方便地控制界面布局和样式。
        
        第二页：XML布局提供了更好的可视化编辑体验，支持在Android Studio中实时预览界面效果。
        
        第三页：自定义属性允许在XML中直接设置文本样式、页面背景、边框颜色等视觉属性。
        
        第四页：循环次数功能仍然完整支持，可以设置有限循环或无限循环模式。
        
        第五页：状态信息区域实时显示当前页码、循环次数和循环方向，方便用户了解阅读状态。
        
        第六页：通过XML布局，可以轻松调整界面结构和组件样式，满足不同的设计需求。
        
        第七页：按钮样式通过drawable资源定义，支持按下状态和正常状态的不同视觉效果。
        
        第八页：这种实现方式结合了XML布局的便利性和自定义View的灵活性。
        
        第九页：PagedTextView现在支持在XML中直接设置初始文本内容和其他样式属性。
        
        第十页：感谢使用XML布局方式的PagedTextView实现，希望这个示例对您有帮助。
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(binding) {
            with(pageText) {
                setText(sampleText)
                setTextSize(42f)
                setOnCycleChangedListener(object: PagedTextView.OnCycleChangedListener {
                    override fun onCycleChanged(cycleCount: Int) {
                        updateStatusInfo()
                    }
                })
            }
            btnPrev.setOnClickListener {
                pageText.showPreviousPage()
                updateStatusInfo()
            }
            btnNext.setOnClickListener {
                pageText.showNextPage()
                updateStatusInfo()
            }
            btnEnableCycle3.setOnClickListener {
                pageText.enableCycleMode(3, 1) // 正向循环3次
                updateStatusInfo()
            }

            btnEnableCycleInfinite.setOnClickListener {
                pageText.enableCycleMode(0, 1) // 无限正向循环
                updateStatusInfo()
            }

            btnDisableCycle.setOnClickListener {
                pageText.disableCycleMode()
                updateStatusInfo()
            }

            btnReset.setOnClickListener {
                pageText.resetToFirstPage()
                updateStatusInfo()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun ActivityPageBinding.updateStatusInfo() {
        // 更新页码信息
        val currentPage = pageText.getCurrentPage() + 1
        val totalPages = pageText.getTotalPages()
        tvPageInfo.text = "页码: $currentPage/$totalPages"

        // 更新循环信息
        if (pageText.isCycleEnabled()) {
            val currentCycle = pageText.getCurrentCycle()
            val maxCycle = pageText.getMaxCycleCount()

            val cycleText = if (maxCycle == 0) {
                "循环模式: 无限循环 | 当前循环: $currentCycle"
            } else {
                "循环模式: 有限循环 | 当前: $currentCycle/$maxCycle"
            }
            tvCycleInfo.text = cycleText

            // 更新循环方向信息
            val direction = if (pageText.getCycleDirection() == 1) "正向" else "反向"
            tvDirectionInfo.text = "循环方向: $direction"
        } else {
            tvCycleInfo.text = "循环模式: 未启用"
            tvDirectionInfo.text = "循环方向: -"
        }
    }

}