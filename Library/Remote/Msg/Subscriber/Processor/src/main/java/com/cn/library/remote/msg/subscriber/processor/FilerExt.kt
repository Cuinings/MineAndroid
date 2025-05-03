package com.cn.library.remote.msg.subscriber.processor

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.Filer

/**
 * @Author: CuiNing
 * @Time: 2024/12/25 14:21
 * @Description:
 */
object FilerExt {

    /**
     * 生成java文件
     */
    fun Filer.generate(qualifiedName: String, typeSpec: TypeSpec) {
        JavaFile.builder(qualifiedName, typeSpec).build().writeTo(this)
    }

}