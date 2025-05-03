package com.cn.library.remote.msg.subscriber.processor

import javax.lang.model.type.TypeMirror

/**
 * @Author: CuiNing
 * @Time: 2024/12/24 16:24
 * @Description: Subscriber util class
 */
object SubscriberUtil {

    /**
     * package name
     */
    lateinit var applicationId: String

    /**
     * classname, qualifiedName
     */
    val classQualifiedNameMap = HashMap<String, String>()

    /**
     * classname
     * methodName
     * subscribeId, TypeMirror list
     */
    val methodParameterTypeMap = HashMap<String, HashMap<String, HashMap<String, ArrayList<TypeMirror>>>>()

}