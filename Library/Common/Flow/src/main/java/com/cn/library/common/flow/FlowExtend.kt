package com.cn.library.common.flow

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

/**
 * 下游
 */
fun <T> Flow<T>.collectByScope(
    coroutineScope: CoroutineScope,
    action: suspend (T) -> Unit
) = coroutineScope.launch { collect { action.invoke(it) } }

fun <T> Flow<T>.collectByLifecycleScope(
    lifecycleScope: LifecycleCoroutineScope,
    action: suspend (T) -> Unit
) = collectByScope(lifecycleScope, action)