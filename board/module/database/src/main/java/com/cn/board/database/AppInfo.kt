package com.cn.board.database

import androidx.room.ColumnInfo
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
        Index(value = ["clazz"], unique = true),
        Index(value = ["packageName"], unique = true),
        Index(value = ["versionCode"], unique = true),
        Index(value = ["versionName"], unique = true),
        Index(value = ["appType"], unique = true),
        Index(value = ["name"], unique = true),
        Index(value = ["main"], unique = true),
        Index(value = ["mainIndex"], unique = true),
        Index(value = ["offlineMain"], unique = true),
        Index(value = ["offlineMainIndex"], unique = true),
    ]
)
data class AppInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var clazz: String = "",
    var packageName: String = "",
    var versionCode: String = "",
    var versionName: String = "",
    var appType: EmAppType = EmAppType.Third,
    var name: String? = null,
    var main: Int = 0,//0默认、1首页显示
    @ColumnInfo(defaultValue = "0") var mainIndex: Int = -1,
    var offlineMain: Int = 0,
    @ColumnInfo(defaultValue = "0") var offlineMainIndex: Int = -1,
)
