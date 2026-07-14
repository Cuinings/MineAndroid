package com.cn.board.database

import androidx.room.ColumnInfo
import androidx.room.Entity
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
    // 注意：本表不对任何业务字段加 UNIQUE 索引。
    // - main/mainIndex/offlineMain*/appType/name 等是分类或排序标志，多个 App 共享同一取值；
    // - 包扫描类 App 的 clazz 默认为空串，多个 App 的 clazz 均为 ""；
    // 两者都会因 UNIQUE 触发「UNIQUE constraint failed」崩溃。
    // 行级唯一性由 @PrimaryKey(autoGenerate = true) 的 id 保证，因此这里不建任何唯一索引。
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
