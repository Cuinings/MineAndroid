package com.cn.library.utils.throttle

object Throttle {
    private val timerMap = HashMap<String, Long>()
    fun throttle(duration: Long = 500L, callBack: (() -> Unit)? = null, task:() -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (((currentTime - (timerMap[task.javaClass.toString()] ?:0))) > duration) {
            task.invoke()
            timerMap[task.javaClass.toString()] = System.currentTimeMillis()
        } else {
            callBack?.invoke()
        }
    }

    fun throttle(duration: Long = 500L, callBack: (() -> Unit)? = null, last: Long, task:() -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - last > duration) {
            task.invoke()
        } else {
            callBack?.invoke()
        }
    }
}