package com.cn.core.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @Author: CuiNing
 * @Time: 2025/6/6 12:29
 * @Description:
 */
/**
 * 上游
 */
fun <T> Flow<T>.flowOnLifecycle(
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
fun <T> T.delayByScope(
    coroutineScope: CoroutineScope,
    delay: Long,
    action: suspend (T) -> Unit
) = coroutineScope.launch {
    delay(delay)
    action.invoke(this@delayByScope)
}

/**
 * 下游
 */
fun <T> Flow<T>.collectByScope(
    coroutineScope: CoroutineScope,
    action: suspend (T) -> Unit
) = coroutineScope.launch { collect { action.invoke(it) } }

inline fun <T> LifecycleOwner.collectOnStarted(
    flow: Flow<T>,
    crossinline action: suspend (T) -> Unit,
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest { action(it) }
        }
    }
}

inline fun <T> LifecycleOwner.collectOnCreated(
    flow: Flow<T>,
    crossinline action: suspend (T) -> Unit,
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.CREATED) {
            flow.collectLatest { action(it) }
        }
    }
}