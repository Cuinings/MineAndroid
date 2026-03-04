package com.cn.core.utils

import java.net.InetAddress
import java.net.UnknownHostException

/**
 * IP地址转换工具类
 */
object IPUtil {

    /**
     * 检查是否为IP地址（IPv4或IPv6）
     */
    fun String.isIp(): Boolean = this.isIp()

    /**
     * 检查是否为IPv4地址
     */
    fun String.isIpV4(): Boolean = this.isIpV4()

    /**
     * 检查是否为IPv6地址
     */
    fun String.isIpV6(): Boolean = this.isIpV6()

    /**
     * 将长整型IP地址转换为字符串
     */
    fun Long.ipToString(): String = "${this shr 24 and 0xFFL}.${this shr 16 and 0xFFL}.${this shr 8 and 0xFFL}.${this and 0xFFL}"

    /**
     * 将字符串IP地址转换为长整型
     */
    fun String.ipToLong(): Long = this.split(".").takeIf { it.size == 4 }?.foldIndexed(0L) { index, acc, part -> acc or (part.toLong() shl (3 - index) * 8) }?:throw IllegalStateException("$this, IP Format Error")

    /**
     * 将长整型IP地址转换为字符串（大端序）
     */
    fun Long.ipToStringBig(): String = "${this and 0xFFL}.${this shr 8 and 0xFFL}.${this shr 16 and 0xFFL}.${this shr 24 and 0xFFL}"

    /**
     * 将字符串IP地址转换为长整型（大端序）
     */
    fun String.ipToLongBig(): Long {
        var ip = 0x0L
        if (this.isEmpty()) {
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
}