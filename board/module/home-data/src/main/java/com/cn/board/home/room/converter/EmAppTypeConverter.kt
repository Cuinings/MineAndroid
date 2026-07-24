package com.cn.board.home.room.converter

import androidx.room.TypeConverter
import com.cn.board.home.entity.EmAppType

class EmAppTypeConverter {

    @TypeConverter
    fun toAppType(value: Int): EmAppType = enumValues<EmAppType>()[value]

    @TypeConverter
    fun fromAppType(value: EmAppType): Int = value.ordinal
}
