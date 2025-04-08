package com.cn.mine.wan.android.common

import com.cn.mine.wan.android.BuildConfig

inline fun <T> T.debug(action:  () -> Unit) {
    BuildConfig.DEBUG.takeIf { it }?.let { action.invoke() }
}