package com.cn.core.utils.gson

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


/**
 * @Author: CuiNing
 * @Time: 2024/10/18 11:31
 * @Description:
 */
object GsonUtil {

    private val gson by lazy {
        Gson().newBuilder().apply { registerTypeAdapterFactory(EnumTypeAdapterFactory.create()) }.create()
    }

    fun Any.asStringBuffer(): StringBuffer = StringBuffer(this.asJson())

    fun Any.asJson(): String = gson.toJson(this)

    fun <T> String.fromJson(clazz: Class<T>): T = gson.fromJson(this, clazz)

    fun <T> String.formJson(type: Type): T = gson.fromJson(this, type)

    fun <T> String.fromJson(t: T): T {
        return this.formJson(object : TypeToken<T>() {}.type)
    }

}