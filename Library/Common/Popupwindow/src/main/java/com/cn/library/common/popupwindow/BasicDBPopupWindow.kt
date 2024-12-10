package com.cn.library.common.popupwindow

import android.content.Context
import android.view.LayoutInflater
import androidx.databinding.ViewDataBinding
import androidx.viewbinding.ViewBinding

/**
 * @Author: CuiNing
 * @Time: 2024/11/21 11:00
 * @Description:
 */
abstract class BasicDBPopupWindow<VB: ViewDataBinding>(
    private val context: Context,
    private val block: (LayoutInflater) -> VB,
): BasicPopupWindow(context) {

    protected val binding by lazy { block(LayoutInflater.from(context)) }


}