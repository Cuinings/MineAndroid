package com.cn.board.meet.home

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.cn.board.meet.home.databinding.FragmentAppListBinding
import com.cn.core.ui.fragment.BasicVmVBFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppListFragment : BasicVmVBFragment<AppListViewModel, FragmentAppListBinding>(
    blockViewModel = { AppListViewModel() },
    blockBinding = { FragmentAppListBinding.inflate(it) }
) {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel.init(requireContext())
        
        setupClickListener()
        observeState()
        observeEffect()
        viewModel.processIntent(AppListViewModel.AppListIntent.LoadApps(requireContext()))
    }
    
    private fun setupClickListener() {
        binding.homeFragmentAppListManager.setOnClickListener {
            viewModel.processIntent(AppListViewModel.AppListIntent.ToggleExpanded)
        }
        
        binding.homeFragmentAppList.setAppClickListener(object : AppList.AppAdapter.AppClickListener {
            override fun onAppClick(appInfo: com.cn.board.database.AppInfo) {
                Log.d("AppListFragment", "应用被点击: ${appInfo.packageName}")
                
                viewModel.processIntent(AppListViewModel.AppListIntent.SelectApp(appInfo))
                
                launchApp(appInfo)
            }
        })
    }
    
    private fun launchApp(appInfo: com.cn.board.database.AppInfo) {
        try {
            val intent = requireContext().packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Log.e("AppListFragment", "无法启动应用: ${appInfo.packageName}")
            }
        } catch (e: Exception) {
            Log.e("AppListFragment", "启动应用失败: ${appInfo.packageName}", e)
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
        
        binding.homeFragmentAppList.setAppSortListener {
            viewModel.processIntent(AppListViewModel.AppListIntent.UpdateAppSortOrder(it))
        }
    }
    
    private fun handleEffect(effect: AppListViewModel.AppListEffect) {
        when (effect) {
            is AppListViewModel.AppListEffect.AppsLoaded -> {
            }
            is AppListViewModel.AppListEffect.ExpandedStateChanged -> {
            }
            is AppListViewModel.AppListEffect.AppsLoadError -> {
            }
            is AppListViewModel.AppListEffect.SearchPerformed -> {
            }
            is AppListViewModel.AppListEffect.AppSelected -> {
            }
            is AppListViewModel.AppListEffect.SelectionCleared -> {
            }
            is AppListViewModel.AppListEffect.SortOrderUpdated -> {
                Log.d("AppListFragment", "排序更新完成")
            }
            is AppListViewModel.AppListEffect.SortOrderUpdateError -> {
                Log.e("AppListFragment", "排序更新错误: ${effect.message}")
            }
        }
    }
    
    private fun updateUI(state: AppListViewModel.AppListState) {
        if (state.isLoading) {
            binding.homeFragmentAppListLoading.visibility = View.VISIBLE
            binding.homeFragmentAppListLoadError.visibility = View.GONE
            binding.homeFragmentAppList.visibility = View.GONE
        } else if (state.error != null) {
            binding.homeFragmentAppListLoading.visibility = View.GONE
            binding.homeFragmentAppListLoadError.visibility = View.VISIBLE
            binding.homeFragmentAppListLoadError.text = state.error
            binding.homeFragmentAppList.visibility = View.GONE
        } else {
            binding.homeFragmentAppListLoading.visibility = View.GONE
            binding.homeFragmentAppListLoadError.visibility = View.GONE
            binding.homeFragmentAppList.visibility = View.VISIBLE
            
            binding.homeFragmentAppList.setApps(state.appList)
            binding.homeFragmentAppList.setSelectedApp(state.selectedApp)
        }
        
        val layoutParams = binding.container.layoutParams as ViewGroup.LayoutParams
        if (state.isExpanded) {
            layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp474)
        } else {
            layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp280)
        }
        binding.container.layoutParams = layoutParams
    }
    
    override fun onDestroyView() {
        binding.homeFragmentAppList.clear()
        super.onDestroyView()
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
}