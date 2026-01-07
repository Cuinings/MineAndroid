package com.cn.core.ui.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * @Author: CuiNing
 * @Time: 2024/10/14 15:24
 * @Description:
 */
abstract class BasicService: Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}