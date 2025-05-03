package com.cn.library.remote.msg.router.client.annotation

import com.cn.library.remote.msg.router.client.bean.RouterTarget

/**
 * @Author: CuiNing
 * @Time: 2024/10/18 16:39
 * @Description:
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(vararg val value: RouterTarget = [RouterTarget.DEFAULT])
