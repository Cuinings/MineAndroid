package com.cn.board.database

import androidx.room.TypeConverter

/**
 * Room TypeConverter — 将 NodeType 枚举转为 String 存储。
 * 优先使用 name() 而非 ordinal，便于数据库可读和迁移安全。
 */
class CommonNodeTypeConverters {

    @TypeConverter
    fun fromNodeType(value: NodeType): String = value.name

    @TypeConverter
    fun toNodeType(value: String): NodeType =
        NodeType.valueOf(value)
}
