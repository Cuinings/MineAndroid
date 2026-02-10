package com.cn.core.utils

import android.annotation.SuppressLint
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * 加密工具类
 */
object EncryptUtil {

    /**
     * MD5加密
     */
    fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val bytes = md.digest(input.toByteArray())
            bytesToHex(bytes)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            input
        }
    }

    /**
     * SHA-1加密
     */
    fun sha1(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            val bytes = md.digest(input.toByteArray())
            bytesToHex(bytes)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            input
        }
    }

    /**
     * SHA-256加密
     */
    fun sha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray())
            bytesToHex(bytes)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            input
        }
    }

    /**
     * Base64编码
     */
    @SuppressLint("NewApi")
    fun base64Encode(input: String): String {
        return try {
            Base64.getEncoder().encodeToString(input.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            input
        }
    }

    /**
     * Base64解码
     */
    @SuppressLint("NewApi")
    fun base64Decode(input: String): String {
        return try {
            String(Base64.getDecoder().decode(input))
        } catch (e: Exception) {
            e.printStackTrace()
            input
        }
    }

    /**
     * AES加密
     */
    @SuppressLint("NewApi", "GetInstance")
    fun aesEncrypt(input: String, key: String): String {
        return try {
            val secretKey = SecretKeySpec(key.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(input.toByteArray())
            Base64.getEncoder().encodeToString(encryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            input
        }
    }

    /**
     * AES解密
     */
    @SuppressLint("NewApi", "GetInstance")
    fun aesDecrypt(input: String, key: String): String {
        return try {
            val secretKey = SecretKeySpec(key.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = Base64.getDecoder().decode(input)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            input
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (byte in bytes) {
            val hex = Integer.toHexString(0xFF and byte.toInt())
            if (hex.length == 1) {
                sb.append('0')
            }
            sb.append(hex)
        }
        return sb.toString()
    }
}