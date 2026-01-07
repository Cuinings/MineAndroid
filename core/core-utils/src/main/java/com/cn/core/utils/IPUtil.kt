package com.cn.core.utils

import java.net.InetAddress
import java.net.UnknownHostException
import java.util.regex.Pattern

/**
 * IP地址
 * 正则匹配
 * 转换
 */
object IPUtil {

    private const val REGEX_IP_V4 = "^(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])$"

    private const val REGEX_IP_V6 = "^\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))(%.+)?\\s*$"

    fun String.isIp(): Boolean = this.isIpV4() || this.isIpV6()

    fun String.isIpV4(): Boolean = this.matches(REGEX_IP_V4)

    fun String.isIpV6(): Boolean = this.matches(REGEX_IP_V6)

    fun Long.ipToString(): String = "${this shr 24 and 0xFFL}.${this shr 16 and 0xFFL}.${this shr 8 and 0xFFL}.${this and 0xFFL}"
    fun String.ipToLong(): Long = this.split(".").takeIf { it.size == 4 }?.foldIndexed(0L) { index, acc, part -> acc or (part.toLong() shl (3 - index) * 8) }?:throw IllegalStateException("$this, IP Format Error")

    fun Long.ipToStringBig(): String = "${this and 0xFFL}.${this shr 8 and 0xFFL}.${this shr 16 and 0xFFL}.${this shr 24 and 0xFFL}"
    fun String.ipToLongBig(): Long {
        var ip = 0x0L
        if (this == null || this.isEmpty()) {
            return ip
        }
        try {
            val ipByte = InetAddress.getByName(this).address
            ip = ip or (ipByte[0].toLong() and 0xFFL)
            ip = ip or ((ipByte[1].toLong() and 0xFFL) shl 8)
            ip = ip or ((ipByte[2].toLong() and 0xFFL) shl 16)
            ip = ip or ((ipByte[3].toLong() and 0xFFL) shl 24)
        } catch (e: UnknownHostException) {
        } finally {
            return ip
        }
    }

    fun CharSequence.matches(regex: String): Boolean = Pattern.matches(regex, this@matches)
}