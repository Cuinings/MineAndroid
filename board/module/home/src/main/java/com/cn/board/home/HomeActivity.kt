package com.cn.board.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.cn.board.wallpaper.WallpaperSettingsActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cn.board.home.databinding.ActivityHomeBinding
import com.cn.core.ui.activity.BasicVmDBActivity
import com.cn.core.utils.FocusDebugHelper
import kotlinx.coroutines.launch

/**
 * HomeActivity
 */
class HomeActivity : BasicVmDBActivity<HomeActivityViewModel, ActivityHomeBinding>(
    { viewModelProvider -> viewModelProvider[HomeActivityViewModel::class.java] },
    { ActivityHomeBinding.inflate(it) }
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FocusDebugHelper.install(this)
    }

    override fun onDestroy() {
        FocusDebugHelper.uninstall(this)
        super.onDestroy()
    }

    override fun onBindLayout() {
        // 视图已由 BasicVmDBActivity 绑定至 binding，此处做额外的数据/观察者绑定
        observeUi()
        // 两页（HomeFrag / SoftFrag）的滑动切换与方向键左右切换由 SwipeFragmentPager 内部处理，
        // 见 activity_home.xml 的 home_swipe_pager。
    }

    /**
     * 统一派发意图：在 UI 交互处调用 dispatch(HomeActivityIntent.Xxx)
     */
    private fun dispatch(intent: HomeActivityIntent) = viewModel.processIntent(intent)

    /**
     * 收集 state / effect 两条流，生命周期感知、STARTED 时自动开始/停止
     */
    private fun observeUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { renderState(it) } }
                launch { viewModel.effect.collect { renderEffect(it) } }
            }
        }
    }

    private fun renderState(state: HomeActivityState) {
        when (state) {
            HomeActivityState.Init -> {
                // 初始态：可做首次数据加载，例如 dispatch(HomeActivityIntent.Refresh)
            }
            is HomeActivityState.Ready -> {
                // 根据 state.isRefreshing 切换刷新指示（如 binding.progressBar 显隐）
            }
        }
    }

    private fun renderEffect(effect: HomeActivityEffect) {
        // 一次性副作用：Toast / 导航 / 弹窗等
        when (effect) {
            is HomeActivityEffect.Toast -> {
                Toast.makeText(this, effect.msg, Toast.LENGTH_SHORT).show()
            }
            HomeActivityEffect.OpenWallpaperSettings -> {
                startActivity(Intent(this, WallpaperSettingsActivity::class.java))
            }
        }
    }

    override fun useSystemWallpaper(): Boolean = true
}
