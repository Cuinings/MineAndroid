package com.cn.library.common.popupwindow

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow

/**
 * @Author: CuiNing
 * @Time: 2024/11/21 9:38
 * @Description:
 */
@SuppressLint("ObsoleteSdkInt", "ClickableViewAccessibility")
open class BasicPopupWindow: PopupWindow {

    constructor(context: Context): this(context,null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): this(context, attrs, defStyleAttr, 0)

    @SuppressLint("InlinedApi")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes) {
        TAG = this.javaClass.simpleName
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isTouchable = true
        isOutsideTouchable = true
        windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        this.setBackgroundDrawable(ColorDrawable(Color.parseColor("#00000000")))
        (if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE).let {
            softInputMode = it
        }
        this.setTouchInterceptor { _, event ->
            if (!isOutsideTouchable) {
                contentView?.dispatchTouchEvent(event)
            }
            isFocusable && !isOutsideTouchable
        }
    }

    open fun show() {
        if (!this.isShowing) {
            showAtLocation(contentView, Gravity.CENTER, 0, 0)
        }
    }

    open fun show(gravity: Int) {
        if (!this.isShowing) {
            showAtLocation(contentView, gravity , 0, 0)
        }
    }

    open fun show(gravity: Int, offsetX: Int, offsetY: Int) {
        if (!this.isShowing) {
            showAtLocation(contentView, gravity , offsetX, offsetY)
        }
    }


    open fun show(offsetX: Int, offsetY: Int) {
        if (!isShowing) {
            showAtLocation(contentView, Gravity.TOP or Gravity.LEFT, offsetX, offsetY)
        }
    }

    open fun showAtTopStart(view: View){
        showAtTopStart(view, 0)
    }

    open fun showAtTopStart(view: View, offsetY: Int){
        val location = IntArray(2)
        view.getLocationInWindow(location)
        contentView.run {
            measure(0, 0)
            showAtLocation(view, Gravity.NO_GRAVITY, location[0], location[1] - measuredHeight - offsetY)
        }
    }

    open fun showAtTopCenter(view: View){
        showAtTopCenter(view, 0)
    }

    open fun showAtTopCenter(view: View, offsetY: Int){
        val location = IntArray(2)
        view.getLocationInWindow(location)
        contentView.run {
            measure(0, 0)
            showAtLocation(view, Gravity.NO_GRAVITY, location[0] + view.width / 2 - measuredWidth / 2, location[1] - measuredHeight - offsetY)
        }
    }

    open fun showAtBottomCenter(view: View) {
        showAtBottomCenter(view, 0)
    }

    open fun showAtBottomCenter(view: View, offsetY: Int){
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        contentView.run {
            measure(0, 0)
            showAtLocation(view, Gravity.NO_GRAVITY, location[0] + view.width / 2 - measuredWidth / 2, location[1] + view.height + offsetY)
        }
    }

    open fun showAtRightCenter(view: View) {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        contentView.run {
            measure(0, 0)
            showAtLocation(view, Gravity.NO_GRAVITY, location[0] + view.width, location[1] + view.height / 2 - measuredHeight / 2)
        }
    }

    open fun showAtLeftCenter(view: View) {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        contentView.run {
            measure(0, 0)
            showAtLocation(view, Gravity.NO_GRAVITY, location[0] - measuredWidth, location[1] + view.height / 2 - measuredHeight / 2)
        }
    }

    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        if (!isShowing) {
            super.showAtLocation(parent, gravity, x, y)
        }
    }

    companion object {
        lateinit var TAG: String
    }
}