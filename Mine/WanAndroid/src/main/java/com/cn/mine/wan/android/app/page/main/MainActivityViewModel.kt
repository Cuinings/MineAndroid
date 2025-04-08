package com.cn.mine.wan.android.app.page.main

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cn.mine.wan.android.net.WanAndroidAPI
import com.cn.mine.wan.android.data.entity.ArticleEntity
import com.cn.mine.wan.android.data.entity.CommonData
import com.cn.mine.wan.android.data.entity.CommonPageData
import com.cn.mine.wan.android.data.entity.result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(): ViewModel() {

    @Inject
    lateinit var wanAndroidAPI: WanAndroidAPI

    fun doSomething() {
        Log.d(MainActivityViewModel::class.simpleName, "doSomething: wanAndroidAPI -> $wanAndroidAPI")
    }

    init { viewModelScope.launch {
        Log.d(MainActivityViewModel::class.simpleName, "init: ")
    } }


}