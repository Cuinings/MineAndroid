package com.cn.core.remote.msg.subscriber.annotation

/**
 * @Author: CuiNing
 * @Time: 2024/12/26 10:36
 * @Description:
 */
interface BasicSubscriber {
    fun getSubscribers():  ArrayList<String>
    fun dispatch(id: String, json: String)
}