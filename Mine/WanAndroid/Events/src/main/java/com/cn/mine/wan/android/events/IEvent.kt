package com.cn.mine.wan.android.events

import kotlinx.coroutines.flow.Flow

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 20:03
 * @Description:
 */
interface IEvent<in Params, out Result> {
    fun execute(param: Params?): Flow<Result>
}