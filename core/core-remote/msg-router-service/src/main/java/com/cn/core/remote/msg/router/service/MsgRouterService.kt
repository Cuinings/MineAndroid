package com.cn.core.remote.msg.router.service

import com.cn.core.remote.RemoteService
import com.cn.core.remote.msg.router.service.manager.MsgRouterManager

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 14:39
 * @Description:
 */
class MsgRouterService: RemoteService<MsgRouterManager>({ MsgRouterManager::class.java })