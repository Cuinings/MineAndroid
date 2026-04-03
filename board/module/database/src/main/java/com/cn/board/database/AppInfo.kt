package com.cn.board.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * @author: cn
 * @time: 2026/2/9 17:12
 * @history
 * @description:应用信息数据类
 */
// 应用信息数据类
@Entity(
    tableName = "app_info",
    indices = [
        Index(value = ["packageName"], unique = true),
        Index(value = ["lastUsedTime"], unique = false),
        Index(value = ["isSystemApp"], unique = false),
        Index(value = ["usageCount"], unique = false),
        Index(value = ["sortOrder"], unique = false),
        Index(value = ["appFlag"], unique = false)
    ]
)
data class AppInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val isSystemApp: Boolean = false,
    val packageName: String = "",
    val lastUsedTime: Long = 0,
    val usageCount: Int = 0,
    val sortOrder: Int = 0,
    val appFlag: Int = 0
)
