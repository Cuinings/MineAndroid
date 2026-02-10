package com.cn.board.meet.home

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.cn.board.meet.home.databinding.FragmentAppListBinding
import com.cn.core.ui.fragment.BasicVmVBFragment
import com.cn.board.database.AppInfo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppListFragment : BasicVmVBFragment<AppListViewModel, FragmentAppListBinding>(
    blockViewModel = { AppListViewModel() },
    blockBinding = { FragmentAppListBinding.inflate(it) }
) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化ViewModel的数据库
        viewModel.initDatabase(requireContext())
        
        setupClickListener()
        observeState()
        observeEffect()

        // 加载应用数据
        viewModel.processIntent(AppListViewModel.AppListIntent.LoadApps(requireContext()))
    }

    private fun setupClickListener() {
        binding.homeFragmentAppListManager.setOnClickListener {
            viewModel.processIntent(AppListViewModel.AppListIntent.ToggleExpanded)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collectLatest {
                updateUI(it)
            }
        }
    }

    private fun observeEffect() {
        lifecycleScope.launch {
            viewModel.effect.collectLatest {
                handleEffect(it)
            }
        }
    }

    private fun handleEffect(effect: AppListViewModel.AppListEffect) {
        when (effect) {
            is AppListViewModel.AppListEffect.AppsLoaded -> {
                // 处理应用加载完成的效果
                // 例如，可以添加日志或其他一次性操作
            }
            is AppListViewModel.AppListEffect.ExpandedStateChanged -> {
                // 处理展开/收起状态变化的效果
                // 例如，可以添加动画或其他一次性操作
            }
            is AppListViewModel.AppListEffect.AppsLoadError -> {
                // 处理应用加载错误的效果
                // 例如，可以显示错误提示
            }
            is AppListViewModel.AppListEffect.SearchPerformed -> {
                // 处理搜索执行的效果
                // 例如，可以添加搜索日志
            }
            is AppListViewModel.AppListEffect.AppSelected -> {
                // 处理应用选中的效果
                // 例如，可以显示选中提示
            }
            is AppListViewModel.AppListEffect.SelectionCleared -> {
                // 处理选择清除的效果
                // 例如，可以显示清除提示
            }
        }
    }

    private fun updateUI(state: AppListViewModel.AppListState) {
        // 更新加载状态
        if (state.isLoading) {
            binding.homeFragmentAppListLoading.visibility = View.VISIBLE
            binding.homeFragmentAppListLoadError.visibility = View.GONE
            binding.homeFragmentAppList.visibility = View.GONE
        } else if (state.error != null) {
            // 更新错误状态
            binding.homeFragmentAppListLoading.visibility = View.GONE
            binding.homeFragmentAppListLoadError.visibility = View.VISIBLE
            binding.homeFragmentAppListLoadError.text = state.error
            binding.homeFragmentAppList.visibility = View.GONE
        } else {
            // 正常状态
            binding.homeFragmentAppListLoading.visibility = View.GONE
            binding.homeFragmentAppListLoadError.visibility = View.GONE
            binding.homeFragmentAppList.visibility = View.VISIBLE
            
            // 更新应用列表
            binding.homeFragmentAppList.setApps(state.appList)
            
            // 更新选中状态
            binding.homeFragmentAppList.setSelectedApp(state.selectedApp)
        }

        // 更新展开/收起状态
        val layoutParams = binding.container.layoutParams as ViewGroup.LayoutParams
        if (state.isExpanded) {
            layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp474)
        } else {
            layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp280)
        }
        binding.container.layoutParams = layoutParams
    }


}
