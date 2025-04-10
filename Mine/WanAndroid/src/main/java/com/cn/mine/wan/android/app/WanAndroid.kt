package com.cn.mine.wan.android.app

import android.util.Log
import com.cn.library.common.application.BasicApplication
import com.cn.mine.wan.android.common.launchIO
import com.tencent.smtt.sdk.QbSdk
import dagger.hilt.android.HiltAndroidApp

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 15:59
 * @Description:
 */
@HiltAndroidApp
class WanAndroid: BasicApplication() {

    override fun initApplication() {
        initX5Environment()
    }

    /**
     * 初始化X5环境
     */
    private fun initX5Environment() {
        launchIO {
            QbSdk.initX5Environment(this@WanAndroid, object : QbSdk.PreInitCallback {
                override fun onCoreInitFinished() {
                    // 内核初始化完成，可能为系统内核，也可能为系统内核
                    Log.d(TAG, "onCoreInitFinished: ")
                }

                /**
                 * 预初始化结束
                 * 由于X5内核体积较大，需要依赖网络动态下发，所以当内核不存在的时候，默认会回调false，此时将会使用系统内核代替
                 * @param isX5 是否使用X5内核
                 */
                override fun onViewInitFinished(isX5: Boolean) {
                    Log.d(TAG, "onViewInitFinished: $isX5")
                }
            })
        }
    }

}