package com.cn.mine.wan.android

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.ContextCompat
import com.cn.library.common.application.ApplicationContextExt
import com.cn.library.common.application.BasicApplication
import com.cn.mine.wan.android.common.launchIO
import com.tencent.smtt.sdk.QbSdk
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * @Author: CuiNing
 * @Time: 2024/12/12 15:59
 * @Description:
 */
@HiltAndroidApp
class WanAndroid: BasicApplication() {

    override fun initApplication() {
        initX5Environment()
        SubscriberCallback()
        isConnected.let { boolean ->

        }
    }

    /**
     * 初始化X5环境
     */
    private fun initX5Environment() {
        launchIO {
            QbSdk.initX5Environment(this@WanAndroid, object : QbSdk.PreInitCallback {
                override fun onCoreInitFinished() {
                    // 内核初始化完成，可能为系统内核，也可能为系统内核
                    Log.d(TAG, "onCoreInitFinished: ")
                }

                /**
                 * 预初始化结束
                 * 由于X5内核体积较大，需要依赖网络动态下发，所以当内核不存在的时候，默认会回调false，此时将会使用系统内核代替
                 * @param isX5 是否使用X5内核
                 */
                override fun onViewInitFinished(isX5: Boolean) {
                    Log.d(TAG, "onViewInitFinished: $isX5")
                }
            })
        }
    }

    val isConnected: () -> Flow<Boolean> = {
        callbackFlow {
            ContextCompat.getSystemService(ApplicationContextExt.context, ConnectivityManager::class.java)?.let { cm ->
                val callback = object: ConnectivityManager.NetworkCallback() {

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        super.onCapabilitiesChanged(network, networkCapabilities)
                        trySendBlocking(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        trySendBlocking(false)
                    }

                }
                cm.registerDefaultNetworkCallback(callback)
                awaitClose { cm.unregisterNetworkCallback(callback) }
            }
        }
    }

    /*fun isConnected(context: Context): Flow<Boolean> = callbackFlow {
        ContextCompat.getSystemService(context, ConnectivityManager::class.java)?.let { cm ->
            val callback = object: ConnectivityManager.NetworkCallback() {

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    trySendBlocking(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    trySendBlocking(false)
                }

            }
            cm.registerDefaultNetworkCallback(callback)
            awaitClose { cm.unregisterNetworkCallback(callback) }
        }
    }*/

}