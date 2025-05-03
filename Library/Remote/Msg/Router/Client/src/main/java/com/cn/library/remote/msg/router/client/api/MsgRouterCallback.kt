package com.cn.library.remote.msg.router.client.api

import com.cn.library.remote.andlinker.annotation.Inout
import com.cn.library.remote.andlinker.annotation.RemoteInterface
import com.cn.library.remote.msg.router.client.bean.Msg

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 15:16
 * @Description:
 */
@RemoteInterface
interface MsgRouterCallback {
    fun dispatchMsg(@Inout msg: Msg)
}