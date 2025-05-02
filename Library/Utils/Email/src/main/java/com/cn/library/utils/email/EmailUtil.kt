package com.cn.library.utils.email

import java.util.regex.Pattern

object EmailUtil {

    const val REGEX_EMAIL: String = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$"

    fun String.isEmail(): Boolean = this.matches(REGEX_EMAIL)

    fun CharSequence.matches(regex: String): Boolean = Pattern.matches(regex, this@matches)
}