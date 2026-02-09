package com.cn.core.remote.msg.router.client.api

import com.cn.core.remote.andlinker.annotation.Inout
import com.cn.core.remote.andlinker.annotation.RemoteInterface
import com.cn.core.remote.msg.router.client.bean.Msg

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 15:16
 * @Description:
 */
@RemoteInterface
interface MsgRouterCallback {
    fun dispatchMsg(@Inout msg: Msg)
}