package com.cn.launcher

import android.graphics.drawable.Drawable
import java.io.Serializable

class AppInfo(
    val appName: String,
    val packageName: String,
    val activityName: String,
    @Transient val appIcon: Drawable
) : Serializable {

    // 通知数量
    var notificationCount: Int = 0
    
    // 是否隐藏
    var isHidden: Boolean = false
    
    // 应用类别
    var category: String = "其他"
}
