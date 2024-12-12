package com.cn.mine.hilt.sample.truck

import android.util.Log
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 10:59
 * @Description:
 */
class ElectricEngine @Inject constructor(): Engine {

    override fun start() {
        Log.d(ElectricEngine::class.simpleName, "ElectricEngine start: ")
    }

    override fun shutDown() {
        Log.d(ElectricEngine::class.simpleName, " ElectricEngineshutDown: ")
    }
}