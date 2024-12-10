package com.cn.library.common.debounce

import java.util.Timer
import kotlin.concurrent.timerTask

object Debounce {

    private val timerMap = HashMap<String, Timer>()
    fun debounce(duration: Long = 500L, task: () -> Unit) {
        task.javaClass.toString().let { timerMap.run {
            this[it]?.cancel()
            put(it, Timer().apply { schedule(timerTask { task.invoke() }, duration) })
        } }
    }

    fun debounce(duration: Long = 500L, task: () -> Unit, timer: Timer) {
        timer.cancel()
        timer.schedule(timerTask {
            task.invoke()
        }, duration)
    }
}