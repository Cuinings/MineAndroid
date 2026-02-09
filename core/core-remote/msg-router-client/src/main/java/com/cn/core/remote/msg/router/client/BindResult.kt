package com.cn.core.remote.msg.router.client

import android.util.Log

/**
 * @Author: CuiNing
 * @Time: 2024/10/14 17:07
 * @Description:
 */
object BindResult {

    var bindMsgRouterResult = false
        set(value) { if (field != value) {
            field = value
            Log.d(BindResult::class.simpleName, "bindMsgRouterResult:$field")
        } }
}