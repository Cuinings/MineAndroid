package com.cn.sample.hilt.truck

import android.util.Log
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 11:00
 * @Description:
 */
class Truck @Inject constructor(private val driver: Driver) {

    @UseGasEngine
    @Inject
    lateinit var engine: Engine
    
    fun deliver() {
        engine.start()
        Log.d(Truck::class.simpleName, "deliver: $driver")
        engine.shutDown()
    }
}