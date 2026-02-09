package com.cn.core.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * @author: cn
 * @time: 2026/1/30 9:19
 * @history
 * @description:
 */

// 1. 移除@SuppressLint，改为在函数内部进行权限检查
fun Context.networkAvailable(): Flow<Boolean> = callbackFlow {
    val connectivityManager = ContextCompat.getSystemService(this@networkAvailable, ConnectivityManager::class.java)
    if (connectivityManager == null) {
        trySend(false)
        close()
        return@callbackFlow
    }

    // 权限检查：没有权限则发送 false 并关闭 Flow
    if (ContextCompat.checkSelfPermission(this@networkAvailable, android.Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
        trySend(false) // 或无权限时可根据需求选择不发送/发送错误信号
        close()
        return@callbackFlow
    }

    // 2. 创建 NetworkCallback 以实现持续监听
    val callback = object : ConnectivityManager.NetworkCallback() {
        // 当网络可用时
        override fun onAvailable(network: Network) {
            trySend(true)
        }
        // 当网络丢失时
        override fun onLost(network: Network) {
            trySend(false)
        }
        // 可选：当网络能力变化时，进行更精确的判断
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val isInternetValid = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            trySend(isInternetValid)
        }
    }

    // 3. 注册回调，并设置初始状态
    try {
        // 先获取一次当前状态作为初始值
        val currentNetwork = connectivityManager.activeNetwork
        val currentCapabilities = connectivityManager.getNetworkCapabilities(currentNetwork)
        val initialState = currentCapabilities?.let { caps ->
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false
        trySend(initialState)

        // 注册回调以监听后续变化
        connectivityManager.registerDefaultNetworkCallback(callback)
    } catch (e: Exception) {
        // 4. 添加异常捕获：注册回调失败时发送false并关闭
        trySend(false)
        close()
        return@callbackFlow
    }

    // 5. 确保在 Flow 收集结束时取消注册回调，避免内存泄漏
    awaitClose {
        Log.d("NetworkUtil", "awaitClose")
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            // 处理取消注册时可能出现的异常（如回调未注册）
            Log.d("NetworkUtil", "Unregister callback failed: ${e.message}")
        }
    }
}



fun Context.isNetworkAvailable(): Flow<Boolean> = callbackFlow {
    val connectivityManager = ContextCompat.getSystemService(this@isNetworkAvailable, ConnectivityManager::class.java)
    if (connectivityManager == null) {
        trySend(false)
        close()
        return@callbackFlow
    }
    val activeNetwork = connectivityManager.activeNetwork
    val currentCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
    val initialState = currentCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    trySend(initialState)
    awaitClose {
        Log.d("NetworkUtil", "awaitClose")
    }
}