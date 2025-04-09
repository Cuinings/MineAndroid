package com.cn.mine.wan.android.app.page.main

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.cn.library.common.activity.BasicActivity.Companion.TAG
import com.cn.mine.wan.android.data.entity.result
import com.cn.mine.wan.android.net.WanAndroidAPI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(): ViewModel() {

    @Inject
    lateinit var wanAndroidAPI: WanAndroidAPI

    init { viewModelScope.launch {
        Log.d(MainActivityViewModel::class.simpleName, "init: ")
    } }

    fun onRefresh() {
        viewModelScope.launch {
            wanAndroidAPI.article(0).result({
                Log.d(MainActivityViewModel::class.simpleName, "article: $it")
//                binding.articleView.addArticle(it)
            }) {
//                Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
//                binding.articleView.isRefreshing = false
            }
            Log.d(TAG, "onCreate: onRefresh")
        }
    }


}