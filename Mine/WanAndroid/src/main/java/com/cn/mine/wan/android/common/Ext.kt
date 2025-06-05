package com.cn.mine.wan.android.common

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.cn.mine.wan.android.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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


inline fun <T> Flow<T>.flowOnLifecycle(
    lifecycle: Lifecycle,
    minActivityState: Lifecycle.State = Lifecycle.State.CREATED
): Flow<T> = callbackFlow {
    lifecycle.repeatOnLifecycle(minActivityState) {
        this@flowOnLifecycle.collect {
            send(it)
        }
    }
    close()
}