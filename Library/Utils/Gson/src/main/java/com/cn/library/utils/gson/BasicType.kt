package com.cn.library.utils.gson

enum class BasicType(var value: String) {
    INT("int"),
    STRING("java.lang.String"),
    LONG("long"),
    DOUBLE("double"),
    BOOLEAN("boolean");

    companion object {
        fun isBasicType(name: String): Boolean {
            return values().any { it.value == name }
        }

        fun get(name: String) = values().first { it.value == name }
    }
}