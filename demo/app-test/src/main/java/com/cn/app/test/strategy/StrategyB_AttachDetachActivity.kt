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
 * ## 策略 B: add + attach/detach
 *
 * 原理：
 * - add 一次，实例常驻
 * - 切换时 detach 当前（销毁 View，保留 Fragment 实例）
 * - 目标未 add 则 add，已 add 则 attach（重建 View）
 *
 * 效果：
 * - Fragment 实例不重建，实例变量（如 clickCount、msgCount）保留
 * - View 每次 attach 都会重建 → View 引用需在 onViewCreated 中重新获取
 * - EditText 输入内容会丢失（View 级别状态）
 * - 内存占用中等（View 被回收，实例保留）
 */
class StrategyB_AttachDetachActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "StrategyB"
        private val FRAGMENT_TAGS = listOf("home", "message", "profile")
    }

    private val fragments = arrayOfNulls<Fragment>(3)
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_strategy)

        setTitle("策略 B - attach/detach")
        findViewById<TextView>(R.id.tv_strategy_label).text =
            "策略 B: add + attach/detach | 实例保留，View 回收/重建"

        // 首次创建时只 add 首页
        if (savedInstanceState == null) {
            fragments[0] = HomeFragment()
            fragments[1] = MessageFragment()
            fragments[2] = ProfileFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragments[0]!!, FRAGMENT_TAGS[0])
                .commitAllowingStateLoss()
        } else {
            fragments[0] = supportFragmentManager.findFragmentByTag(FRAGMENT_TAGS[0])
            // 从 FragmentManager 恢复已 detach 的 Fragment
            fragments[1] = supportFragmentManager.findFragmentByTag(FRAGMENT_TAGS[1])
            fragments[2] = supportFragmentManager.findFragmentByTag(FRAGMENT_TAGS[2])
            // 找到当前 attach 的作为 currentIndex
            for (i in fragments.indices) {
                val f = fragments[i]
                if (f != null && !f.isDetached && f.isAdded) {
                    currentIndex = i
                    break
                }
            }
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
        val current = fragments[currentIndex]
        val target = fragments[index] ?: return

        val transaction = supportFragmentManager.beginTransaction()

        // detach 当前（销毁 View，保留实例）
        if (current != null) {
            transaction.detach(current)
        }

        // attach 或 add 目标
        if (target.isAdded) {
            transaction.attach(target)
        } else {
            transaction.add(R.id.fragment_container, target, FRAGMENT_TAGS[index])
        }

        transaction.commitAllowingStateLoss()
        currentIndex = index
        android.util.Log.d(TAG, "切换到 index=$index")
    }
}
