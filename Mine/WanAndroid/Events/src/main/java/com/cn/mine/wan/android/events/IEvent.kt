package com.cn.mine.wan.android.events

import kotlinx.coroutines.CoroutineScope

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 20:03
 * @Description:
 */
interface IEvent<in Params, out Result> {
    fun execute(scope: CoroutineScope, param: Params?, action: (Result) -> Unit)
}