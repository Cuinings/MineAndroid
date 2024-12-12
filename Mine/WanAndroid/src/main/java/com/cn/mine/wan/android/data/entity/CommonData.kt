package com.cn.mine.wan.android.data.entity

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 16:25
 * @Description:
 */
data class CommonData<T>(
    var data: T? = null,
    var errorCode: Int = 0,//-1001登录失败需要重新登录   0成功
    var errorMsg: String = ""
)
