package com.cn.board.home.function

import androidx.lifecycle.MutableLiveData

class BooleanLiveData: MutableLiveData<Boolean>() {

    override fun getValue(): Boolean {
        return super.getValue()?: false
    }

    fun default(value: Boolean): BooleanLiveData {
        setValue(value)
        return this
    }
}