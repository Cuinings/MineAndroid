package com.cn.mine.wan.android.app.page.h5

import android.os.Bundle
import com.cn.library.common.activity.BasicVBActivity
import com.cn.mine.wan.android.app.databinding.ActivityH5Binding
import com.cn.mine.wan.android.app.databinding.ActivityH5Binding.inflate

/**
 * @Author: CuiNing
 * @Time: 2025/6/20 23:01
 * @Description:
 */
class H5Activity: BasicVBActivity<ActivityH5Binding>({ inflate(it) }) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra("url")?.let {
            binding.root.loadUrl(it)
        }
    }

}