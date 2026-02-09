package com.cn.core.remote

import android.app.ActivityManager
import android.content.Context
import android.os.Process

/**
 * @Author: CuiNing
 * @Time: 2024/10/17 19:05
 * @Description:
 */
object Check {

    fun Context.isServerApp(packageService: String): Boolean {
        var result = false
        (this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses?.forEach {
            if (it.pid == Process.myPid() && it.processName == packageService) result = true
        }
        return result
    }

    fun Context.processName(): String {
        var result = ""
        (this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses?.forEach {
            if (it.pid == Process.myPid()) result = it.processName
        }
        return result
    }

}