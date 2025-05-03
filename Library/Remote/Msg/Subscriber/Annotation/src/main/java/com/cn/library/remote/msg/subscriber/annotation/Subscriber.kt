package com.cn.library.remote.msg.subscriber.annotation

/**
 * @Author: CuiNing
 * @Time: 2024/10/21 14:33
 * @Description:
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Subscriber(val subscribeId: String = "")
