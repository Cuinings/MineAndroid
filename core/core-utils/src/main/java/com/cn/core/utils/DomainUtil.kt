package com.cn.core.utils

import java.util.regex.Pattern

/**
 * 域名正则匹配
 */
object DomainUtil {

    private const val REGEX_DOMAIN = "(?:(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,})"

    fun String.isDomain(): Boolean = this.matches(REGEX_DOMAIN)

    private fun CharSequence.matches(regex: String): Boolean = Pattern.matches(regex, this@matches)

}