package com.cn.core.utils

import android.content.Context
import android.os.Environment
import java.io.*

/**
 * 文件操作工具类
 */
object FileUtil {

    /**
     * 获取应用内部存储目录
     */
    fun getFilesDir(context: Context): File {
        return context.filesDir
    }

    /**
     * 获取应用缓存目录
     */
    fun getCacheDir(context: Context): File {
        return context.cacheDir
    }

    /**
     * 获取应用外部存储目录
     */
    fun getExternalFilesDir(context: Context, type: String? = null): File? {
        return context.getExternalFilesDir(type)
    }

    /**
     * 获取应用外部缓存目录
     */
    fun getExternalCacheDir(context: Context): File? {
        return context.externalCacheDir
    }

    /**
     * 获取公共外部存储目录
     */
    fun getPublicStorageDir(type: String): File? {
        return Environment.getExternalStoragePublicDirectory(type)
    }

    /**
     * 检查文件是否存在
     */
    fun isFileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    /**
     * 检查目录是否存在
     */
    fun isDirExists(dirPath: String): Boolean {
        val dir = File(dirPath)
        return dir.exists() && dir.isDirectory
    }

    /**
     * 创建文件
     */
    fun createFile(filePath: String): Boolean {
        try {
            val file = File(filePath)
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            return file.createNewFile()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 创建目录
     */
    fun createDir(dirPath: String): Boolean {
        val dir = File(dirPath)
        return dir.mkdirs()
    }

    /**
     * 删除文件
     */
    fun deleteFile(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) file.delete() else true
    }

    /**
     * 删除目录
     */
    fun deleteDir(dirPath: String): Boolean {
        val dir = File(dirPath)
        if (!dir.exists()) return true
        if (!dir.isDirectory) return dir.delete()
        val files = dir.listFiles() ?: return true
        for (file in files) {
            if (file.isDirectory) {
                deleteDir(file.absolutePath)
            } else {
                file.delete()
            }
        }
        return dir.delete()
    }

    /**
     * 复制文件
     */
    fun copyFile(srcPath: String, destPath: String): Boolean {
        try {
            val srcFile = File(srcPath)
            val destFile = File(destPath)
            if (!srcFile.exists()) return false
            val parentDir = destFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            val inputStream = FileInputStream(srcFile)
            val outputStream = FileOutputStream(destFile)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            inputStream.close()
            outputStream.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 移动文件
     */
    fun moveFile(srcPath: String, destPath: String): Boolean {
        return copyFile(srcPath, destPath) && deleteFile(srcPath)
    }

    /**
     * 读取文件内容
     */
    fun readFileContent(filePath: String, charsetName: String = "UTF-8"): String {
        try {
            val file = File(filePath)
            if (!file.exists()) return ""
            val inputStream = FileInputStream(file)
            val reader = BufferedReader(InputStreamReader(inputStream, charsetName))
            val stringBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }
            reader.close()
            inputStream.close()
            return stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * 写入文件内容
     */
    fun writeFileContent(filePath: String, content: String, charsetName: String = "UTF-8"): Boolean {
        try {
            val file = File(filePath)
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            val outputStream = FileOutputStream(file)
            val writer = OutputStreamWriter(outputStream, charsetName)
            writer.write(content)
            writer.flush()
            writer.close()
            outputStream.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 获取文件大小
     */
    fun getFileSize(filePath: String): Long {
        val file = File(filePath)
        return if (file.exists()) file.length() else 0
    }

    /**
     * 获取目录大小
     */
    fun getDirSize(dirPath: String): Long {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return 0
        var size: Long = 0
        val files = dir.listFiles() ?: return 0
        for (file in files) {
            size += if (file.isDirectory) getDirSize(file.absolutePath) else file.length()
        }
        return size
    }

    /**
     * 获取文件扩展名
     */
    fun getFileExtension(filePath: String): String {
        val lastDotIndex = filePath.lastIndexOf(".")
        return if (lastDotIndex >= 0) filePath.substring(lastDotIndex + 1) else ""
    }

    /**
     * 获取文件名
     */
    fun getFileName(filePath: String): String {
        val lastSeparatorIndex = filePath.lastIndexOf(File.separator)
        return if (lastSeparatorIndex >= 0) filePath.substring(lastSeparatorIndex + 1) else filePath
    }

    /**
     * 获取文件名（不含扩展名）
     */
    fun getFileNameWithoutExtension(filePath: String): String {
        val fileName = getFileName(filePath)
        val lastDotIndex = fileName.lastIndexOf(".")
        return if (lastDotIndex >= 0) fileName.substring(0, lastDotIndex) else fileName
    }
}