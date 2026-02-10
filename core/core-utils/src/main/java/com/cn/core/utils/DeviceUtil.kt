@file:Suppress("DEPRECATION")

package com.cn.core.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

/**
 * 设备信息工具类
 */
object DeviceUtil {

    /**
     * 获取设备制造商
     */
    fun getManufacturer(): String {
        return Build.MANUFACTURER
    }

    /**
     * 获取设备型号
     */
    fun getModel(): String {
        return Build.MODEL
    }

    /**
     * 获取设备品牌
     */
    fun getBrand(): String {
        return Build.BRAND
    }

    /**
     * 获取设备系统版本
     */
    fun getSystemVersion(): String {
        return Build.VERSION.RELEASE
    }

    /**
     * 获取设备系统SDK版本
     */
    fun getSystemSdkVersion(): Int {
        return Build.VERSION.SDK_INT
    }

    /**
     * 获取设备ID
     */
    @SuppressLint("HardwareIds")
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    fun getDeviceId(context: Context): String {
        try {
            val telephonyManager = ContextCompat.getSystemService(context, TelephonyManager::class.java)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager?.imei ?: Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } else {
                @Suppress("DEPRECATION")
                telephonyManager?.deviceId ?: Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }
    }

    /**
     * 获取Android ID
     */
    @SuppressLint("HardwareIds")
    fun getAndroidId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 获取IMEI
     */
    @SuppressLint("HardwareIds")
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    fun getImei(context: Context): String {
        try {
            val telephonyManager = ContextCompat.getSystemService(context, TelephonyManager::class.java)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager?.imei ?: ""
            } else {
                @Suppress("DEPRECATION")
                telephonyManager?.deviceId ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * 获取IMSI
     */
    @SuppressLint("HardwareIds")
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    fun getImsi(context: Context): String {
        try {
            val telephonyManager = ContextCompat.getSystemService(context, TelephonyManager::class.java)
            @Suppress("DEPRECATION")
            return telephonyManager?.subscriberId ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * 获取手机号码
     */
    @SuppressLint("HardwareIds")
    @RequiresPermission(anyOf = [Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.READ_PHONE_STATE])
    fun getPhoneNumber(context: Context): String {
        try {
            val telephonyManager = ContextCompat.getSystemService(context, TelephonyManager::class.java)
            @Suppress("DEPRECATION")
            return telephonyManager?.line1Number ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * 获取网络运营商名称
     */
    fun getNetworkOperatorName(context: Context): String {
        try {
            val telephonyManager = ContextCompat.getSystemService(context, TelephonyManager::class.java)
            return telephonyManager?.networkOperatorName ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * 获取网络类型
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun getNetworkType(context: Context): Int {
        try {
            val telephonyManager = ContextCompat.getSystemService(context, TelephonyManager::class.java)
            return telephonyManager?.networkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
        } catch (e: Exception) {
            e.printStackTrace()
            return TelephonyManager.NETWORK_TYPE_UNKNOWN
        }
    }

    /**
     * 获取SIM卡状态
     */
    fun getSimState(context: Context): Int {
        try {
            val telephonyManager = ContextCompat.getSystemService(context, TelephonyManager::class.java)
            return telephonyManager?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN
        } catch (e: Exception) {
            e.printStackTrace()
            return TelephonyManager.SIM_STATE_UNKNOWN
        }
    }

    /**
     * 获取SIM卡序列号
     */
    @SuppressLint("HardwareIds")
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    fun getSimSerialNumber(context: Context): String {
        try {
            val telephonyManager = ContextCompat.getSystemService(context, TelephonyManager::class.java)
            @Suppress("DEPRECATION")
            return telephonyManager?.simSerialNumber ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * 获取设备指纹
     */
    fun getDeviceFingerprint(): String {
        return Build.FINGERPRINT
    }

    /**
     * 获取设备硬件信息
     */
    fun getHardware(): String {
        return Build.HARDWARE
    }

    /**
     * 获取设备Bootloader
     */
    fun getBootloader(): String {
        return Build.BOOTLOADER
    }

    /**
     * 获取设备Radio版本
     */
    @SuppressLint("ObsoleteSdkInt")
    fun getRadioVersion(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Build.getRadioVersion()
        } else {
            @Suppress("DEPRECATION")
            Build.RADIO
        }
    }

    /**
     * 获取设备主机名
     */
    fun getHost(): String {
        return Build.HOST
    }

    /**
     * 获取设备ID
     */
    fun getDevice(): String {
        return Build.DEVICE
    }

    /**
     * 获取设备产品名
     */
    fun getProduct(): String {
        return Build.PRODUCT
    }

    /**
     * 获取设备板名
     */
    fun getBoard(): String {
        return Build.BOARD
    }

    /**
     * 获取设备CPU架构
     */
    @SuppressLint("ObsoleteSdkInt")
    fun getCpuAbi(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS[0]
        } else {
            @Suppress("DEPRECATION")
            Build.CPU_ABI
        }
    }

    /**
     * 获取设备CPU架构列表
     */
    @SuppressLint("ObsoleteSdkInt")
    fun getCpuAbis(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS
        } else {
            @Suppress("DEPRECATION")
            arrayOf(Build.CPU_ABI, Build.CPU_ABI2)
        }
    }

    /**
     * 检查设备是否为模拟器
     */
    fun isEmulator(): Boolean {
        return Build.FINGERPRINT.contains("generic") ||
                Build.FINGERPRINT.contains("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == Build.PRODUCT
    }
}