package com.cn.wan.android.domain

import java.util.regex.Pattern

object DomainUtil {

    private const val REGEX_DOMAIN = "(?:(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,})"

    fun String.isDomain(): Boolean = this.matches(REGEX_DOMAIN)

    fun CharSequence.matches(regex: String): Boolean = Pattern.matches(regex, this@matches)

}