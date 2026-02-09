package com.cn.core.resources.drawable.dsl

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable

/**
 * @Author:         cn
 * @Date:           2024/9/9 16:11
 * @Description:
 */
inline fun selectorDrawable(builder: SelectorBuilder.() -> Unit): StateListDrawable {
    return SelectorBuilder().also(builder).build()
}

inline fun shapeDrawable(builder: ShapeBuilder.() -> Unit): GradientDrawable {
    return ShapeBuilder().also(builder).build()
}
