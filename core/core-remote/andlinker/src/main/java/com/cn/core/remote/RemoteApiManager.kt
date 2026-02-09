package com.cn.core.remote

/**
 * @Author: CuiNing
 * @Time: 2024/10/11 15:59
 * @Description:
 */
object RemoteApiManager {

    internal var mBinderUnitCache: HashMap<String, BinderUnit> = HashMap()

    fun unbind(action: String?) = mBinderUnitCache.remove(action)?.unbind()

    fun unbindAll() = mBinderUnitCache.onEachIndexed { _, entry -> entry.value.unbind() }.clear()

    fun <T> createRemoteApiInterface(action: String?, api: Class<T>) {
        action?.let { mBinderUnitCache[action]?.createRemoteApiInstance(api) }
    }

    fun registerCallback(action: String?, any: Any) {
        mBinderUnitCache[action]?.registerCallback(any)
    }

    fun unregisterCallback(action: String?, any: Any) {
        mBinderUnitCache[action]?.unRegisterCallback(any)
    }

    fun <T> getApiFromCache(api: Class<T>): T? {
        var result: T? = null
        mBinderUnitCache.forEach { (_, binderUnit) ->
            binderUnit.getRmtApiFromCache(api)?.let { result = it }
        }
        return result
    }

}