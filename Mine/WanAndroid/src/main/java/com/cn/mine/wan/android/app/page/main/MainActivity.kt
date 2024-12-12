package com.cn.mine.wan.android.app.page.main

import android.os.Bundle
import com.cn.library.common.activity.BasicVBActivity
import com.cn.mine.wan.android.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BasicVBActivity<ActivityMainBinding>({ ActivityMainBinding.inflate(it) }) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

}