package com.cn.core.remote.msg.router.client.annotation

/**
 * @Author: CuiNing
 * @Time: 2024/10/21 14:33
 * @Description:
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Subscriber(val subscribeId: String = "")
