package com.cn.mine.wan.android.data.wan.android

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 16:25
 * @Description:
 */
data class CommonEntity<T>(
    var data: T,
    var errorCode: Int = 0,//-1001登录失败需要重新登录   0成功
    var errorMsg: String = ""
)

fun <T> CommonEntity<T>.result(success: (T) -> Unit, finishAction: (String) -> Unit) {
    if (errorCode == 0) success.invoke(data)
    finishAction.invoke(errorMsg)
}


