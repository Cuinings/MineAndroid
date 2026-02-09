package com.cn.core.remote

import android.content.Context
import android.util.Log
import com.cn.core.remote.RemoteApiManager.mBinderUnitCache

/**
 * @Author: CuiNing
 * @Time: 2024/10/14 16:13
 * @Description:
 */
class RemoteBuilder(context: Context): LinkerBuilder(context) {

    override fun build(): BinderUnit = context.packageName?.let {
        Log.d(TAG, "build action: $action")
        mBinderUnitCache[action]?.let { binderUnit ->
            createInstanceList.forEach { binderUnit.createRemoteApiInstance(it) }
            registerObjList.forEach { binderUnit.registerCallback(it) }
            mBinderUnitCache[action] = binderUnit
            binderUnit
        }?:super.build().apply { mBinderUnitCache[action] = this }
    }?:throw IllegalArgumentException("Context can not be NULL")

    companion object {
        val TAG = RemoteBuilder::class.simpleName
    }

}