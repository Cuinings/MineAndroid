package com.cn.core.utils

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Toast工具类
 */
object ToastUtil {

    private var toast: Toast? = null

    /**
     * 显示短时间Toast
     */
    fun showShort(context: Context, message: String) {
        show(context, message, Toast.LENGTH_SHORT)
    }

    /**
     * 显示长时间Toast
     */
    fun showLong(context: Context, message: String) {
        show(context, message, Toast.LENGTH_LONG)
    }

    /**
     * 显示自定义时长Toast
     */
    private fun show(context: Context, message: String, duration: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            cancel()
            toast = Toast.makeText(context.applicationContext, message, duration)
            toast?.show()
        }
    }

    /**
     * 取消Toast
     */
    fun cancel() {
        toast?.cancel()
        toast = null
    }

    /**
     * 显示短时间Toast（带资源ID）
     */
    fun showShort(context: Context, resId: Int) {
        showShort(context, context.getString(resId))
    }

    /**
     * 显示长时间Toast（带资源ID）
     */
    fun showLong(context: Context, resId: Int) {
        showLong(context, context.getString(resId))
    }
}