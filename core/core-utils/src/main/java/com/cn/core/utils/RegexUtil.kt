package com.cn.core.utils

import java.util.regex.Pattern

/**
 * 正则匹配工具类
 * 统一管理各种正则表达式和匹配方法
 */
object RegexUtil {

    // ====================================== 邮箱相关 ======================================

    /**
     * 邮箱正则表达式
     */
    const val REGEX_EMAIL: String = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$"

    /**
     * 检查是否为邮箱地址
     */
    fun String.isEmail(): Boolean = matches(REGEX_EMAIL)

    // ====================================== 域名相关 ======================================

    /**
     * 域名正则表达式
     */
    const val REGEX_DOMAIN: String = "(?:(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,})"

    /**
     * 检查是否为域名
     */
    fun String.isDomain(): Boolean = matches(REGEX_DOMAIN)

    // ====================================== IP地址相关 ======================================

    /**
     * IPv4地址正则表达式
     */
    const val REGEX_IP_V4: String = "^(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])$"

    /**
     * IPv6地址正则表达式
     */
    const val REGEX_IP_V6: String = "^\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))(%.+)?\\s*$"

    /**
     * 检查是否为IP地址（IPv4或IPv6）
     */
    fun String.isIp(): Boolean = isIpV4() || isIpV6()

    /**
     * 检查是否为IPv4地址
     */
    fun String.isIpV4(): Boolean = matches(REGEX_IP_V4)

    /**
     * 检查是否为IPv6地址
     */
    fun String.isIpV6(): Boolean = matches(REGEX_IP_V6)

    // ====================================== 手机号码相关 ======================================

    /**
     * 手机号码正则表达式（中国大陆）
     */
    const val REGEX_MOBILE: String = "^1[3-9]\\d{9}$"

    /**
     * 检查是否为手机号码
     */
    fun String.isMobile(): Boolean = matches(REGEX_MOBILE)

    // ====================================== 身份证号相关 ======================================

    /**
     * 身份证号正则表达式（18位）
     */
    const val REGEX_ID_CARD: String = "^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$"

    /**
     * 检查是否为身份证号
     */
    fun String.isIdCard(): Boolean = matches(REGEX_ID_CARD)

    // ====================================== URL相关 ======================================

    /**
     * URL正则表达式
     */
    const val REGEX_URL: String = "^(https?:\\/\\/)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*\\/?$"

    /**
     * 检查是否为URL
     */
    fun String.isUrl(): Boolean = matches(REGEX_URL)

    // ====================================== 邮政编码相关 ======================================

    /**
     * 邮政编码正则表达式（中国大陆）
     */
    const val REGEX_POSTAL_CODE: String = "^[1-9]\\d{5}$"

    /**
     * 检查是否为邮政编码
     */
    fun String.isPostalCode(): Boolean = matches(REGEX_POSTAL_CODE)

    // ====================================== 银行卡号相关 ======================================

    /**
     * 银行卡号正则表达式（16-19位）
     */
    const val REGEX_BANK_CARD: String = "^[1-9]\\d{15,18}$"

    /**
     * 检查是否为银行卡号
     */
    fun String.isBankCard(): Boolean = matches(REGEX_BANK_CARD)

    // ====================================== 用户名相关 ======================================

    /**
     * 用户名正则表达式（4-20位字母、数字、下划线）
     */
    const val REGEX_USERNAME: String = "^[a-zA-Z0-9_]{4,20}$"

    /**
     * 检查是否为合法用户名
     */
    fun String.isUsername(): Boolean = matches(REGEX_USERNAME)

    // ====================================== 密码相关 ======================================

    /**
     * 强密码正则表达式（至少8位，包含大小写字母和数字）
     */
    const val REGEX_STRONG_PASSWORD: String = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"

    /**
     * 检查是否为强密码
     */
    fun String.isStrongPassword(): Boolean = matches(REGEX_STRONG_PASSWORD)

    /**
     * E.164正则表达式
      */
    const val REGEX_E164: String = "^\\+(?:[0-9]●?){6,14}[0-9]$"

    fun String.isValidE164() = matches(REGEX_E164)

    // ====================================== 通用方法 ======================================

    /**
     * 通用正则匹配方法
     */
    fun CharSequence.matches(regex: String): Boolean = Pattern.matches(regex, this)

    /**
     * 检查字符串是否匹配正则表达式
     */
    fun match(input: CharSequence, regex: String): Boolean = Pattern.matches(regex, input)

    /**
     * 提取匹配的内容
     */
    fun extractMatches(input: String, regex: String): List<String> {
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(input)
        val matches = mutableListOf<String>()
        while (matcher.find()) {
            matches.add(matcher.group())
        }
        return matches
    }

    /**
     * 替换匹配的内容
     */
    fun replaceMatches(input: String, regex: String, replacement: String): String {
        return input.replace(Regex(regex), replacement)
    }
}