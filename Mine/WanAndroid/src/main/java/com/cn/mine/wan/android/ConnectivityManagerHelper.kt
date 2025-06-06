package com.cn.mine.wan.android

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.ContextCompat
import com.cn.library.common.application.ApplicationContextExt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * @Author: CuiNing
 * @Time: 2025/6/6 12:13
 * @Description:
 */
object ConnectivityManagerHelper {

    const val TAG: String = "ConnectivityManagerHelper"

    val isConnected: Flow<Boolean> = callbackFlow {
        Log.d(TAG, "isConnected: ")
        ContextCompat.getSystemService(ApplicationContextExt.context, ConnectivityManager::class.java)?.let { cm ->
            val callback = object: ConnectivityManager.NetworkCallback() {

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).let {
                        Log.d(TAG, "onCapabilitiesChanged: $it")
//                        super.onCapabilitiesChanged(network, networkCapabilities)
                        trySendBlocking(it)
                    }
                }

                override fun onLost(network: Network) {
//                        super.onLost(network)
                    Log.d(TAG, "onLost: ")
                    trySendBlocking(false)
                }

            }
            cm.registerDefaultNetworkCallback(callback)
            awaitClose { cm.unregisterNetworkCallback(callback) }
        }
    }
}