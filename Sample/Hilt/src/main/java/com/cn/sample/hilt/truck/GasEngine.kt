package com.cn.sample.hilt.truck

import android.util.Log
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 10:59
 * @Description:
 */
class GasEngine @Inject constructor(): Engine {

    override fun start() {
        Log.d(GasEngine::class.simpleName, "GasEngine start: ")
    }

    override fun shutDown() {
        Log.d(GasEngine::class.simpleName, "GasEngine shutDown: ")
    }
}