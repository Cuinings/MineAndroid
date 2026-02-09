package com.cn.core.ui.view.recyclerview.adapter.animation

import android.animation.Animator
import android.view.View

interface BaseAnimation {
    fun animators(view: View): Array<Animator>
}