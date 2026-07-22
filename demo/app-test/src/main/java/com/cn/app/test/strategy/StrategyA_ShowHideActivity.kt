package com.cn.app.test.strategy

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cn.app.test.R
import com.cn.app.test.fragment.HomeFragment
import com.cn.app.test.fragment.MessageFragment
import com.cn.app.test.fragment.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * ## 策略 A: add + show/hide
 *
 * 原理：
 * - 首次创建时 add 全部 Fragment
 * - 切换时 hide 当前、show 目标
 * - Fragment 实例和 View 都驻留内存，不走任何销毁生命周期
 *
 * 效果：
 * - 切换零延迟、无闪烁
 * - 点击计数、输入内容、消息计数等全部保留
 * - 内存占用最高（所有 Fragment 常驻）
 */
class StrategyA_ShowHideActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "StrategyA"
        private val FRAGMENT_TAGS = listOf("home", "message", "profile")
    }

    private val fragments = arrayOfNulls<Fragment>(3)
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_strategy)

        setTitle("策略 A - show/hide")
        findViewById<TextView>(R.id.tv_strategy_label).text =
            "策略 A: add + show/hide | Fragment 全部驻留，状态完整保留"

        // 预创建全部 Fragment
        if (savedInstanceState == null) {
            fragments[0] = HomeFragment()
            fragments[1] = MessageFragment()
            fragments[2] = ProfileFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragments[0]!!, FRAGMENT_TAGS[0])
                .add(R.id.fragment_container, fragments[1]!!, FRAGMENT_TAGS[1])
                .hide(fragments[1]!!)  // 隐藏非当前页
                .add(R.id.fragment_container, fragments[2]!!, FRAGMENT_TAGS[2])
                .hide(fragments[2]!!)  // 隐藏非当前页
                .commitAllowingStateLoss()
        } else {
            // 恢复时从 FragmentManager 找回
            for (i in FRAGMENT_TAGS.indices) {
                fragments[i] = supportFragmentManager.findFragmentByTag(FRAGMENT_TAGS[i])
            }
        }

        // 底部导航点击
        findViewById<BottomNavigationView>(R.id.bottom_nav).setOnItemSelectedListener { item ->
            val index = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_message -> 1
                R.id.nav_profile -> 2
                else -> return@setOnItemSelectedListener false
            }
            switchTo(index)
            true
        }
    }

    private fun switchTo(index: Int) {
        if (index == currentIndex) return
        val current = fragments[currentIndex]
        val target = fragments[index] ?: return

        supportFragmentManager.beginTransaction()
            .hide(current!!)
            .show(target)
            .commitAllowingStateLoss()

        currentIndex = index
        android.util.Log.d(TAG, "切换到 index=$index")
    }
}
