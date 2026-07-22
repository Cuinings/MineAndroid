package com.cn.app.test.strategy

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cn.app.test.R
import com.cn.app.test.fragment.HomeFragment
import com.cn.app.test.fragment.MessageFragment
import com.cn.app.test.fragment.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * ## 策略 C: replace
 *
 * 原理：
 * - 每次切换都 remove 当前 Fragment + add 新 Fragment
 * - 旧 Fragment 走完整销毁生命周期（onPause → onStop → onDestroyView → onDestroy → onDetach）
 * - 新 Fragment 走完整创建生命周期（onAttach → onCreate → onCreateView → onViewCreated → ...）
 *
 * 效果：
 * - 内存最省（仅当前 Fragment 存在）
 * - 所有状态丢失（点击计数归零、输入内容清空、消息计数归零）
 * - ProfileFragment 创建时间每次切换都变
 * - 切换有卡顿感（完整生命周期 + View inflate）
 * - onDestroyView → onDestroy 间隔短，注意异步任务泄漏
 */
class StrategyC_ReplaceActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "StrategyC"
    }

    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_strategy)

        setTitle("策略 C - replace")
        findViewById<TextView>(R.id.tv_strategy_label).text =
            "策略 C: replace | 即用即毁，全量重建"

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commitAllowingStateLoss()
            currentIndex = 0
        }

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

        val newFragment = when (index) {
            0 -> HomeFragment()
            1 -> MessageFragment()
            2 -> ProfileFragment()
            else -> return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, newFragment)
            .commitAllowingStateLoss()

        currentIndex = index
        android.util.Log.d(TAG, "切换到 index=$index (全量重建)")
    }
}
