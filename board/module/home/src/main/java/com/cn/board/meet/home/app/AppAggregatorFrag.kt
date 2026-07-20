package com.cn.board.meet.home.app

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.cn.board.meet.home.HomeModel
import com.cn.board.meet.home.HomeModel.appListFlow
import com.cn.board.meet.home.HomeModel.mainAppListFlow
import com.cn.board.meet.home.databinding.FragmentAppAggregatorBinding
import com.cn.core.ui.collectOnStarted
import com.cn.core.ui.fragment.BasicVmDBFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author: cn
 * @time: 14/7/2026 下午 3:10
 * @history
 * @description: 首页应用聚合 Fragment，展示 AppStreamAggregator 扫描/对齐后的应用列表
 */
class AppAggregatorFrag: BasicVmDBFragment<AppAggregatorFragModel, FragmentAppAggregatorBinding>(
    { viewModelProvider -> viewModelProvider[AppAggregatorFragModel::class.java] },
    { FragmentAppAggregatorBinding.inflate(it) }
) {

    override fun onBindViewModel() {
    }

    override fun onBindData() {
        // 绑定点击事件变量，避免布局里的 onClick 在 click 为 null 时 NPE
        binding.click = AAppAggregatorFragClick()
    }

    override fun onObserver() {
        // 订阅 HomeModel 持有的聚合结果流：init() 完成后由 HomeModel 写入。
        // StateFlow 会向新订阅者重放最新值，因此本 Fragment 即使晚于 init() 创建，
        // 也能立即拿到已聚合好的列表；STARTED 时收集、STOPPED 时自动取消，避免泄漏。
        viewLifecycleOwner.collectOnStarted(appListFlow) { list ->
            binding.mainActivityFragAppManagerListLayout.submit(list.toMutableList())
        }
        viewLifecycleOwner.collectOnStarted(mainAppListFlow) { list ->
            binding.mainActivityFragAppManagerEditListLayout.submit(list.toMutableList())
        }

        // 拖拽结束后把新顺序持久化到 DB，并同步回 appListFlow，保证顺序不丢失
        binding.mainActivityFragAppManagerListLayout.onOrderChangedListener = object : AppAggregatorListLayout.OnOrderChangedListener {
            override fun onOrderChanged(order: List<SoftEntity>) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    HomeModel.updateAppOrder(order)
                }
            }
        }

        // 管理模式下点击应用：切换 main（在线）/ offlineMain（离线）标记，并刷新列表与编辑列表
        binding.mainActivityFragAppManagerListLayout.onItemSelectedListener = object : AppAggregatorListLayout.OnItemSelectedListener {
            override fun onItemSelected(entity: SoftEntity, online: Boolean) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    HomeModel.toggleMainFlag(entity, online)
                }
            }
        }
    }

    inner class AAppAggregatorFragClick {

        fun manager(view: View) {
            with(binding) {
                val value = !mainActivityFragAppManagerFinish.isVisible
                mainActivityFragAppManagerListLayout.enableManager(value)
                if (value) {
                    mainActivityFragAppManagerAppListTitle.visibility = View.GONE
                    mainActivityFragAppManagerRight.visibility = View.GONE
                    mainActivityFragAppManagerTitle.visibility = View.VISIBLE
                    mainActivityFragAppManagerFinish.visibility = View.VISIBLE
                    mainActivityFragAppManagerEditListLayout.visibility = View.VISIBLE
                } else {
                    mainActivityFragAppManagerTitle.visibility = View.GONE
                    mainActivityFragAppManagerFinish.visibility = View.GONE
                    mainActivityFragAppManagerEditListLayout.visibility = View.GONE
                    mainActivityFragAppManagerAppListTitle.visibility = View.VISIBLE
                    mainActivityFragAppManagerRight.visibility = View.VISIBLE
                }
            }
        }
    }
}
