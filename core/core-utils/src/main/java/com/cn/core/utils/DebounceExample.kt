package com.cn.core.utils

import android.widget.EditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Debounce使用示例
 */
object DebounceExample {

    /**
     * 示例1：处理搜索输入
     * @param editText 输入框
     * @param onSearch 搜索回调
     */
    fun setupSearchDebounce(editText: EditText, onSearch: (String) -> Unit) {
        editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString()
                // 使用Debounce处理输入，500ms延迟
                Debounce.debounce(500) {
                    onSearch(text)
                }
            }
            
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    /**
     * 示例2：处理按钮点击
     * @param onClick 点击回调
     */
    fun debouncedButtonClick(onClick: () -> Unit) {
        // 使用Debounce处理点击，防止快速连续点击
        Debounce.debounce(300) {
            onClick()
        }
    }

    /**
     * 示例3：处理滚动事件
     * @param onScrollEnd 滚动结束回调
     */
    fun debouncedScrollEnd(onScrollEnd: () -> Unit) {
        // 使用Debounce处理滚动结束事件
        Debounce.debounce(200) {
            onScrollEnd()
        }
    }

    /**
     * 示例4：取消Debounce任务
     * @param task 要取消的任务
     */
    fun cancelDebounceTask(task: () -> Unit) {
        // 取消指定的Debounce任务
        Debounce.cancel(task)
    }

    /**
     * 示例5：取消所有Debounce任务
     */
    fun cancelAllDebounceTasks() {
        // 取消所有Debounce任务
        Debounce.cancelAll()
    }
    
    /**
     * 示例6：使用自定义CoroutineScope
     * @param onTask 任务回调
     */
    fun useCustomCoroutineScope(onTask: () -> Unit) {
        // 创建自定义CoroutineScope
        val customScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        // 使用自定义CoroutineScope执行debounce
        Debounce.debounce(500, onTask, customScope)
    }
}
