package com.cn.core.remote

import android.content.Context

/**
 * @Author: CuiNing
 * @Time: 2024/10/11 15:00
 * @Description:
 */
abstract class RemoteManager(protected val context: Context) {

    abstract fun registerAnyList(): MutableList<Any>

}