package com.cn.core.resources.ext

import android.content.res.Resources
import androidx.annotation.DimenRes

/**
 * @author: cn
 * @time: 2026/2/9 15:38
 * @history
 * @description:
 */
fun @receiver:DimenRes Int.asFloat(resources: Resources): Float = resources.getDimension(this@asFloat)
fun @receiver:DimenRes Int.asInt(resources: Resources): Int = resources.getDimensionPixelOffset(this@asInt)