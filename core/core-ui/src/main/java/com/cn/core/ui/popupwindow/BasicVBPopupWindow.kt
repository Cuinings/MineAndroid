package com.cn.core.ui.popupwindow

import android.content.Context
import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding

/**
 * @Author: CuiNing
 * @Time: 2024/11/21 11:00
 * @Description: ViewBinding PopupWindow
 */
abstract class BasicVBPopupWindow<VB: ViewBinding>(
    private val context: Context,
    private val block: (LayoutInflater) -> VB,
): BasicPopupWindow(context) {

    protected val binding by lazy { block(LayoutInflater.from(context)) }


}