package com.cn.mine.wan.android.app.page.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.cn.library.common.activity.BasicVBActivity
import com.cn.mine.wan.android.app.page.main.view.ArticleView
import com.cn.mine.wan.android.data.entity.result
import com.cn.mine.wan.android.databinding.ActivityMainBinding
import com.cn.mine.wan.android.databinding.ActivityMainBinding.inflate
import com.cn.mine.wan.android.net.WanAndroidAPI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BasicVBActivity<ActivityMainBinding>({ inflate(it) }) {

    private val viewModel by viewModels<MainActivityViewModel>()

    @Inject
    lateinit var wanAndroidAPI: WanAndroidAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: wanAndroidAPI -> $wanAndroidAPI")
        lifecycleScope.launch { viewModel.uiStateFlow.map { it.articleUIState }.collect { articleState ->
            Log.d(TAG, "uiStateFlow collect: $articleState")
            when(articleState) {
                ArticleUIState.INIT -> ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.INTERNET), 100)
                is ArticleUIState.Article -> binding.articleView.addArticle(articleState.articles)
                is ArticleUIState.ArticleFinish -> {
                    binding.articleView.takeIf { it.isRefreshing }?.isRefreshing = false
                    articleState.msg?.takeIf{ it.isNotBlank() }?.let { Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show() }

                }
            }
        } }
        binding.articleView.run {
            onRefreshCallBack = object: ArticleView.OnRefreshCallBack {
                override fun onRefresh() {
                    viewModel.sendUIIntent(MainActivityUIEvent.GetArticle(0))
                }
            }
            onLoadMoreCallBack = object : ArticleView.OnLoadMoreCallBack {
                override fun onLoadMore(pageNum: Int) {
                    viewModel.sendUIIntent(MainActivityUIEvent.GetArticle(pageNum))
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "requestCode:$requestCode, permissions:$permissions, grantResults:${grantResults.size}, ${grantResults[0]}")
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            binding.articleView.isRefreshing = true
            viewModel.sendUIIntent(MainActivityUIEvent.GetArticle(0))
        }
    }

}