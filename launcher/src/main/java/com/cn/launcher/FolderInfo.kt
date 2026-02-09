package com.cn.launcher

import android.graphics.drawable.Drawable

class FolderInfo(
    var folderName: String,
    val apps: MutableList<AppInfo>
) {

    // 文件夹图标
    var folderIcon: Drawable? = null
    
    // 排序方式：0-按名称排序，1-按安装时间排序，2-按使用频率排序
    var sortType: Int = 0
    
    // 重命名文件夹
    fun rename(newName: String) {
        folderName = newName
    }
    
    // 设置文件夹图标
    fun setIcon(icon: Drawable) {
        folderIcon = icon
    }
    
    // 排序应用
    fun sortApps() {
        when (sortType) {
            0 -> apps.sortBy { it.appName }
            1 -> apps.sortBy { it.packageName } // 简化处理，实际应该按安装时间
            2 -> apps.sortBy { it.packageName } // 简化处理，实际应该按使用频率
        }
    }
    
    // 添加应用到文件夹
    fun addApp(appInfo: AppInfo) {
        if (!apps.contains(appInfo)) {
            apps.add(appInfo)
            sortApps()
        }
    }
    
    // 从文件夹中移除应用
    fun removeApp(appInfo: AppInfo) {
        apps.remove(appInfo)
    }
}
