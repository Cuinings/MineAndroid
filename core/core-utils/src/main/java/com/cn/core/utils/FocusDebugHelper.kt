package com.cn.core.utils

import android.app.Activity
import android.view.View
import android.view.ViewTreeObserver
import java.util.WeakHashMap

/**
 * 焦点调试工具类
 *
 * 监听 Activity 全局焦点跳转并打印详细信息，方便排查焦点问题。
 *
 * 用法：
 * - onCreate 中调用 [install]
 * - onDestroy 中调用 [uninstall]
 */
object FocusDebugHelper {

    private const val TAG = "FocusDebug"

    /** 持有 listener 引用防止 GC */
    private val listenerMap = WeakHashMap<View, ViewTreeObserver.OnGlobalFocusChangeListener>()

    /**
     * 为 Activity 的 decorView 安装全局焦点监听
     */
    fun install(activity: Activity) {
        val rootView = activity.window.decorView
        val listener = ViewTreeObserver.OnGlobalFocusChangeListener { oldFocus, newFocus ->
            LogUtil.d(TAG, "Focus: ${formatView(oldFocus)} → ${formatView(newFocus)}")
        }
        rootView.viewTreeObserver.addOnGlobalFocusChangeListener(listener)
        listenerMap[rootView] = listener
        LogUtil.d(TAG, "FocusDebugHelper installed on ${activity.javaClass.simpleName}")
    }

    /**
     * 卸载全局焦点监听
     */
    fun uninstall(activity: Activity) {
        val rootView = activity.window.decorView
        val listener = listenerMap.remove(rootView)
        if (listener != null && rootView.viewTreeObserver.isAlive) {
            rootView.viewTreeObserver.removeOnGlobalFocusChangeListener(listener)
        }
        LogUtil.d(TAG, "FocusDebugHelper uninstalled from ${activity.javaClass.simpleName}")
    }

    /**
     * 格式化 View 信息为可读字符串
     */
    private fun formatView(view: View?): String {
        if (view == null) return "null"
        val sb = StringBuilder()
        sb.append(view.javaClass.simpleName)
        sb.append("(id=")
        if (view.id != View.NO_ID) {
            try {
                sb.append(view.resources.getResourceEntryName(view.id))
            } catch (_: Exception) {
                sb.append("0x${Integer.toHexString(view.id)}")
            }
        } else {
            sb.append("NO_ID")
        }
        sb.append(", focusable=").append(view.isFocusable)
        sb.append(", focused=").append(view.isFocused)
        view.contentDescription?.let { sb.append(", desc=\"$it\"") }
        sb.append(")")
        return sb.toString()
    }
}
