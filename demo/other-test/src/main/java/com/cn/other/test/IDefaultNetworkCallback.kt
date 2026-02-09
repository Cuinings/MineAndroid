package com.cn.other.test

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * @Author: CuiNing
 * @Time: 2024/7/23 14:47
 * @Description:
 */
@SuppressLint("MissingPermission")
class IDefaultNetworkCallback(context: Context, val scope: CoroutineScope): NetworkCallback() {

    val connectivityManager: ConnectivityManager = context.getSystemService(Application.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build(), this)
    }

    fun Network.getNetworkCapabilities(): NetworkCapabilities? = connectivityManager.getNetworkCapabilities(this)

    @SuppressLint("NewApi")
    fun NetworkCapabilities.process(boolean: Boolean) {
        scope.launch {
            when {
                hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
//                    changeNetWorkType(EmNetWorkType.TRANSPORT_ETHERNET)
//                    ethAvailable = boolean
//                    signalStrengthWiFi = 0
//                    TPLog.printDetail(IDefaultNetworkCallback::class.simpleName, "TRANSPORT_ETHERNET $boolean")
                }
                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
//                    signalStrengthWiFi = signalStrength
//                    wifiAvailable = boolean
//                    changeNetWorkType(EmNetWorkType.TRANSPORT_WIFI)
//                    TPLog.printDetail(IDefaultNetworkCallback::class.simpleName, "TRANSPORT_WIFI $boolean")
                }
                else -> {}//changeNetWorkType(EmNetWorkType.OTHER)
            }
        }
    }

    private val ipStrBuilder by lazy { StringBuilder() }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        network.getNetworkCapabilities()?.process(true)
    }

    @SuppressLint("NewApi")
    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        networkCapabilities.process(true)
    }

    override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
        super.onLinkPropertiesChanged(network, linkProperties)
        scope.launch {
            linkProperties.linkAddresses.apply {
                ipStrBuilder.clear()
                forEachIndexed { index, linkAddress ->
                    linkAddress?.toString()?.split("/")?.get(0)?.let {
                        //只保留IPV4的IP
//                        if (RegularUtil.isMatch(RegularUtil.IPV4, it)) {
//                            ipStrBuilder.append(it)
//                        }
                    }
                }
//                homeDataCache.ip = ipStrBuilder.toString()
            }
        }
    }

    //网络已断开连接
    override fun onLost(network: Network) {
        super.onLost(network)
        scope.launch {
//            changeNetWorkType(EmNetWorkType.UNKNOW)
            network.getNetworkCapabilities()?.process(false)
        }
    }

//    private fun changeNetWorkType(type: EmNetWorkType) {
//        defaultNetWorkType = type
//    }
}