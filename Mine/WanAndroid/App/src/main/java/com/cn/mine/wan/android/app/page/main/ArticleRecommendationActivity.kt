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
import com.cn.library.common.flow.collectByScope
import com.cn.library.remote.msg.subscriber.annotation.Subscriber
import com.cn.mine.wan.android.app.databinding.ActivityMainBinding
import com.cn.mine.wan.android.app.databinding.ActivityMainBinding.inflate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class ArticleRecommendationActivity  : BasicVBActivity<ActivityMainBinding>({ inflate(it) }) {

    private val viewModel by viewModels<MainActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.uiStateFlow.map { it.articleUIState }.collectByScope (lifecycleScope) { articleUIState ->
            Log.d(TAG, "uiStateFlow collect: ${articleUIState.javaClass.simpleName}")
            when(articleUIState) {
                ArticleUIState.INIT -> ActivityCompat.requestPermissions(this@ArticleRecommendationActivity, arrayOf(Manifest.permission.INTERNET), 100)
                is ArticleUIState.Article -> binding.articleView.addArticle(articleUIState.articles)
                is ArticleUIState.ArticleFinish -> {
                    binding.articleView.takeIf { it.isRefreshing }?.isRefreshing = false
                    articleUIState.msg?.takeIf{ it.isNotBlank() }?.let { Toast.makeText(this@ArticleRecommendationActivity, it, Toast.LENGTH_SHORT).show() }

                }
            }
        }
        binding.articleView.run {
            onRefreshCallBack = object: ArticleListView.OnRefreshCallBack {
                override fun onRefresh() {
                    viewModel.sendUIIntent(MainActivityUIEvent.GetArticle(0))
                }
            }
            onLoadMoreCallBack = object : ArticleListView.OnLoadMoreCallBack {
                override fun onLoadMore(pageNum: Int) {
                    viewModel.sendUIIntent(MainActivityUIEvent.GetArticle(pageNum))
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "requestCode:$requestCode, permissions:$permissions, grantResults:${grantResults.size}, ${grantResults[0]}")
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
            viewModel.sendUIIntent(MainActivityUIEvent.GetBanner)
            viewModel.sendUIIntent(MainActivityUIEvent.GetArticle(0))
        }
    }

    @Subscriber("Test")
    fun subscriber() {}

}