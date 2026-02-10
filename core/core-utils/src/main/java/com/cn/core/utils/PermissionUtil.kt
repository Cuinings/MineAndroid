package com.cn.core.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * 权限工具类
 */
object PermissionUtil {

    /**
     * 检查单个权限是否已授予
     */
    fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查多个权限是否已授予
     */
    fun checkPermissions(context: Context, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (!checkPermission(context, permission)) {
                return false
            }
        }
        return true
    }

    /**
     * 请求单个权限
     */
    fun requestPermission(activity: Activity, permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    /**
     * 请求多个权限
     */
    fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * 请求单个权限（Fragment）
     */
    fun requestPermission(fragment: Fragment, permission: String, requestCode: Int) {
        fragment.requestPermissions(arrayOf(permission), requestCode)
    }

    /**
     * 请求多个权限（Fragment）
     */
    fun requestPermissions(fragment: Fragment, permissions: Array<String>, requestCode: Int) {
        fragment.requestPermissions(permissions, requestCode)
    }

    /**
     * 检查是否需要显示权限请求说明
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * 检查是否需要显示多个权限请求说明
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (shouldShowRequestPermissionRationale(activity, permission)) {
                return true
            }
        }
        return false
    }

    /**
     * 检查是否需要显示权限请求说明（Fragment）
     */
    fun shouldShowRequestPermissionRationale(fragment: Fragment, permission: String): Boolean {
        return fragment.shouldShowRequestPermissionRationale(permission)
    }

    /**
     * 检查是否需要显示多个权限请求说明（Fragment）
     */
    fun shouldShowRequestPermissionRationale(fragment: Fragment, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (shouldShowRequestPermissionRationale(fragment, permission)) {
                return true
            }
        }
        return false
    }

    /**
     * 处理权限请求结果
     */
    fun onRequestPermissionsResult(grantResults: IntArray): Boolean {
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * 获取权限名称
     */
    fun getPermissionName(permission: String): String {
        return when (permission) {
            android.Manifest.permission.READ_CALENDAR -> "读取日历"
            android.Manifest.permission.WRITE_CALENDAR -> "写入日历"
            android.Manifest.permission.CAMERA -> "相机"
            android.Manifest.permission.READ_CONTACTS -> "读取联系人"
            android.Manifest.permission.WRITE_CONTACTS -> "写入联系人"
            android.Manifest.permission.GET_ACCOUNTS -> "获取账户"
            android.Manifest.permission.ACCESS_FINE_LOCATION -> "精确位置"
            android.Manifest.permission.ACCESS_COARSE_LOCATION -> "粗略位置"
            android.Manifest.permission.RECORD_AUDIO -> "录音"
            android.Manifest.permission.READ_PHONE_STATE -> "读取电话状态"
            android.Manifest.permission.CALL_PHONE -> "拨打电话"
            android.Manifest.permission.READ_CALL_LOG -> "读取通话记录"
            android.Manifest.permission.WRITE_CALL_LOG -> "写入通话记录"
            android.Manifest.permission.ADD_VOICEMAIL -> "添加语音邮件"
            android.Manifest.permission.USE_SIP -> "使用SIP"
            android.Manifest.permission.PROCESS_OUTGOING_CALLS -> "处理拨出电话"
            android.Manifest.permission.BODY_SENSORS -> "身体传感器"
            android.Manifest.permission.SEND_SMS -> "发送短信"
            android.Manifest.permission.RECEIVE_SMS -> "接收短信"
            android.Manifest.permission.READ_SMS -> "读取短信"
            android.Manifest.permission.RECEIVE_WAP_PUSH -> "接收WAP推送"
            android.Manifest.permission.RECEIVE_MMS -> "接收彩信"
            android.Manifest.permission.READ_EXTERNAL_STORAGE -> "读取存储"
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE -> "写入存储"
            android.Manifest.permission.ACCESS_NETWORK_STATE -> "访问网络状态"
            android.Manifest.permission.INTERNET -> "访问互联网"
            android.Manifest.permission.ACCESS_WIFI_STATE -> "访问WiFi状态"
            android.Manifest.permission.CHANGE_WIFI_STATE -> "更改WiFi状态"
            android.Manifest.permission.BLUETOOTH -> "蓝牙"
            android.Manifest.permission.BLUETOOTH_ADMIN -> "蓝牙管理"
            android.Manifest.permission.WAKE_LOCK -> "唤醒锁"
            android.Manifest.permission.VIBRATE -> "振动"
            android.Manifest.permission.RECEIVE_BOOT_COMPLETED -> "接收开机完成"
            else -> permission
        }
    }
}