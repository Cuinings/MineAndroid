package com.cn.library.utils.gson

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

@Suppress("UNCHECKED_CAST")
class EnumTypeAdapterFactory1: TypeAdapterFactory {

    companion object {
        @JvmStatic
        fun create(): EnumTypeAdapterFactory1 {
            return EnumTypeAdapterFactory1()
        }
    }

    override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!type.rawType.isEnum) {
            return null
        }
        val maps = mutableMapOf<T, ValueType>()
        type.rawType?.enumConstants?.filterNotNull()?.forEach {
            val tt: T = it as T
            val serializedName = tt.javaClass.getField((it as Enum<*>).name).getAnnotation(SerializedName::class.java)

            if (serializedName != null) {
                maps[tt] = ValueType(serializedName.value, BasicType.STRING)
                return@forEach
            }
//            tt.javaClass.getDeclaredField("value")
//            tt.javaClass.getDeclaredField("nVal")
            val field = tt.javaClass.declaredFields.firstOrNull()?.apply {
                BasicType.isBasicType(this.type.name)
            }
            println("create: ${field?.name?:"NULL"}, ${field?.type?.name?:"NULL"}")
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
            } else {
                val ordinal = tt.javaClass.superclass.getDeclaredMethod("ordinal")
                ordinal.isAccessible = true
                maps[tt] = ValueType(ordinal.invoke(it) as Int, BasicType.INT)
            }
        }

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) {
                if (value == null) {
                    out.nullValue()
                } else {
                    maps[value]?.let {
//                        Log.d("cuining", "write: ${it.type}, ${it}, $value, $value")
                        when (it.type) {
                            BasicType.INT -> out.value(it.value as Int)
                            BasicType.STRING -> out.value(it.value as String)
                            BasicType.LONG -> out.value(it.value as Long)
                            BasicType.DOUBLE -> out.value(it.value as Double)
                            BasicType.BOOLEAN -> out.value(it.value as Boolean)
                        }
                    }
                }
            }

            override fun read(reader: JsonReader): T? {
//                Log.d("cuining", "read: $reader")
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    return null
                } else {
                    val source = reader.nextString()
                    var tt: T? = null
                    maps.forEach { (value, type) ->
//                        Log.d("cuining", "read: ${value}, $type, $source")
                        if (type.value.toString() == source) {
                            tt = value
                            return@forEach
                        }
                    }
                    return tt
                }
            }

        }
    }
    data class ValueType(var value: Any, var type: BasicType)
}