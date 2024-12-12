package com.cn.mine.wan.android.app.page.main

import androidx.lifecycle.ViewModel
import com.cn.mine.wan.android.WanAndroidAPI
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(): ViewModel() {

    @Inject
    lateinit var wanAndroidAPI: WanAndroidAPI

}