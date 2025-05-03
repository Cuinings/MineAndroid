package com.cn.library.remote.msg.router.client.callback

import com.cn.library.remote.andlinker.annotation.Inout
import com.cn.library.remote.msg.router.client.bean.Msg

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 14:48
 * @Description:
 */
interface MsgCallback {
    fun onRcvMsg(@Inout msg: Msg, target: String)
    fun onSubScribe(): MutableList<String>
}