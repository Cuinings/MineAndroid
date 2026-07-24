package com.cn.board.home.function

import androidx.lifecycle.MutableLiveData

class IntLiveData : MutableLiveData<Int>() {

    override fun getValue(): Int {
        return super.getValue() ?: 0
    }

    fun default(value: Int): IntLiveData {
        setValue(value)
        return this
    }
}