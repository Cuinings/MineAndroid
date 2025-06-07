package com.cn.library.utils.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * @Author: CuiNing
 * @Time: 2025/6/7 16:09
 * @Description:
 */
@SuppressLint("MissingPermission")
fun Context.isConnected(): Flow<Boolean> = callbackFlow {
    ContextCompat.getSystemService(this@isConnected, ConnectivityManager::class.java)?.let { cm ->
        val callback = object: ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySendBlocking(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
            override fun onLost(network: Network) {
                trySendBlocking(false)
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }
}

@SuppressLint("MissingPermission")
suspend fun Context.isConnected(action: suspend (Boolean) -> Unit) = this@isConnected.isConnected().collect { action.invoke(it) }

