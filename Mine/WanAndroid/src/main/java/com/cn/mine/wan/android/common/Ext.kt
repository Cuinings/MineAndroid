package com.cn.mine.wan.android.common

import com.cn.mine.wan.android.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

inline fun <T> T.debug(action:  () -> Unit) {
    BuildConfig.DEBUG.takeIf { it }?.let { action.invoke() }
}

val mScope = CoroutineScope(SupervisorJob())

inline fun <T> T.launchMain(crossinline action: T.() -> T) {
    mScope.launch(Dispatchers.Main) { action.invoke(this@launchMain) }
}
inline fun <T> T.launchIO(crossinline action: T.() -> Unit) {
    mScope.launch(Dispatchers.IO) { action.invoke(this@launchIO) }
}