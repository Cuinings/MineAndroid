package com.cn.mine.hilt.sample.test

import android.util.Log
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 10:30
 * @Description:
 */
class HiltTest @Inject constructor() {
    
    fun doTest(simpleName: String?) {
        Log.d(HiltTest::class.simpleName, "doTest: $simpleName")
    }
}