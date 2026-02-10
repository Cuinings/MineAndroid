package com.cn.board.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author: cn
 * @time: 2026/2/9 17:12
 * @history
 * @description:应用信息数据类
 */
// 应用信息数据类
@Entity(tableName = "app_info")
data class AppInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val iconRes: Int = android.R.drawable.sym_def_app_icon,
    val isSystemApp: Boolean = false,
    val packageName: String = "",
    val lastUsedTime: Long = 0,
    val usageCount: Int = 0
)
