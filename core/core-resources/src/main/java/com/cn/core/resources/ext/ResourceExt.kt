package com.cn.core.resources.ext

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * @author: cn
 * @time: 2026/2/9 15:38
 * @history
 * @description:
 */
fun @receiver:DimenRes Int.asFloat(resources: Resources): Float = resources.getDimension(this@asFloat)
fun @receiver:DimenRes Int.asInt(resources: Resources): Int = resources.getDimensionPixelOffset(this@asInt)
fun @receiver:StringRes Int.asString(resources: Resources): String = resources.getString(this@asString)
@SuppressLint("UseCompatLoadingForDrawables")
fun @receiver:DrawableRes Int.asDrawable(resources: Resources): Drawable = resources.getDrawable(this@asDrawable, null)