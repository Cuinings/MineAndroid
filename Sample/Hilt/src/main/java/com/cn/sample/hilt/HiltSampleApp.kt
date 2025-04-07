package com.cn.mine.hilt.sample

import android.app.Application
import com.cn.sample.hilt.test.HiltTest
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 10:26
 * @Description:
 */
@HiltAndroidApp
class HiltSampleApp: Application() {

    @Inject
    lateinit var hiltTest: HiltTest

    override fun onCreate() {
        super.onCreate()
        hiltTest.doTest(HiltAndroidApp::class.simpleName)
    }

}