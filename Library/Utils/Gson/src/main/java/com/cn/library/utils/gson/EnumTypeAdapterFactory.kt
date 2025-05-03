package com.cn.library.utils.gson

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlin.collections.iterator

class EnumTypeAdapterFactory: TypeAdapterFactory {

    companion object {
        @JvmStatic
        fun create(): EnumTypeAdapterFactory {
            return EnumTypeAdapterFactory()
        }
    }

    /**
     * 枚举与json相互转换
     * 1.@SerializedName标记的枚举，@SerializedName中携带的参数既为枚举的值，类型为String
     * 2.@SerializedValueInt标记的枚举，@SerializedValueInt中携带的参数既为枚举的值，类型为Int
     * 3.有参枚举，转换时取枚举中最后一个参数，作为枚举的值，类型根据参数类型而定，限定为int,long,double,string,boolean (废弃，后续禁止使用此类方式)
     * 4.无参枚举，转换时取枚举的ordinal作为枚举值，类型为Int
     */
    override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!type.rawType.isEnum) {
            return null
        }
        val maps = mutableMapOf<T, ValueType>()
        type.rawType.enumConstants.filterNotNull().forEach {
            val tt: T = it as T
            //@SerializedName标记的处理
            val serializedName = tt.javaClass.getField((it as Enum<*>).name).getAnnotation(SerializedName::class.java)
            if (serializedName != null) {
                maps[tt] = ValueType(serializedName.value, BasicType.STRING)
                return@forEach
            }

            //新增@SerializedValueInt注解用于枚举转特定Int类型的Json
            val jsonIntValue = tt.javaClass.getField((it as Enum<*>).name).getAnnotation(SerializedValueInt::class.java)
            if (jsonIntValue != null) {
                maps[tt] = ValueType(jsonIntValue.value, BasicType.INT)
                return@forEach
            }

            //有参枚举处理
            val field = tt.javaClass.declaredFields.firstOrNull { it2 ->
                BasicType.isBasicType(it2.type.name)
            }

            if (field != null) {
                field.isAccessible = true
                val basicType = BasicType.get(field.type.name)
                val value: Any = when (basicType) {
                    BasicType.INT -> field.getInt(tt)
                    BasicType.STRING -> field.get(tt) as String
                    BasicType.LONG -> field.getLong(tt)
                    BasicType.DOUBLE -> field.getDouble(tt)
                    BasicType.BOOLEAN -> field.getBoolean(tt)
                }
                maps[tt] = ValueType(value, basicType)
                return@forEach
            }
            //无参枚举处理
            val ordinal = tt.javaClass.superclass.getDeclaredMethod("ordinal")
            ordinal.isAccessible = true
            maps[tt] = ValueType(ordinal.invoke(it) as Int, BasicType.INT)
        }

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) {
                if (value == null) {
                    out.nullValue()
                } else {
                    val valueType = maps[value]!!
                    when (valueType.type) {
                        BasicType.INT -> out.value(valueType.value as Int)
                        BasicType.STRING -> out.value(valueType.value as String)
                        BasicType.LONG -> out.value(valueType.value as Long)
                        BasicType.DOUBLE -> out.value(valueType.value as Double)
                        BasicType.BOOLEAN -> out.value(valueType.value as Boolean)
                    }
                }
            }

            override fun read(reader: JsonReader): T? {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    return null
                } else {
                    val source = reader.nextString()
                    var tt: T? = null
                    for((value, type) in maps){
                        if (type.value.toString() == source) {
                            tt = value
                            break
                        }
                    }
                    return tt
                }
            }

        }
    }
    data class ValueType(var value: Any, var type: BasicType)
}