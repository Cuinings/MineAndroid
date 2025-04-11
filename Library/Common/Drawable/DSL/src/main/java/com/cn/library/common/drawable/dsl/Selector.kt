package com.cn.library.common.drawable.dsl

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable

/**
 * @Author: CuiNing
 * @Time: 2024/6/17 13:22
 * @Description:
 */
class SelectorBuilder: DrawableBuilder {

    var normal: GradientDrawable? = null
    var focus: GradientDrawable? = null
    var pressed: GradientDrawable? = null
    var selected: GradientDrawable? = null

    override fun build(): StateListDrawable = StateListDrawable().apply {
        focus?.let { addState(intArrayOf(android.R.attr.state_focused), focus) }
        pressed?.let { addState(intArrayOf(android.R.attr.state_pressed), pressed) }
        selected?.let { addState(intArrayOf(android.R.attr.state_selected), selected) }
        normal?.let { addState(intArrayOf(), normal) }
    }

    companion object {
        val  TAG = SelectorBuilder::class.simpleName
    }

}