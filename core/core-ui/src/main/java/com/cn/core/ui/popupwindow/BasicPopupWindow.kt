package com.cn.core.ui.popupwindow

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
import androidx.core.graphics.drawable.toDrawable

/**
 * @Author: CuiNing
 * @Time: 2024/11/21 9:38
 * @Description: Basic PopupWindow
 */
@SuppressLint("ObsoleteSdkInt", "ClickableViewAccessibility")
open class BasicPopupWindow: PopupWindow {

    constructor(context: Context): this(context,null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): this(context, attrs, defStyleAttr, 0)

    @SuppressLint("InlinedApi")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes) {
        tag = this.javaClass.simpleName
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isTouchable = true
        isOutsideTouchable = true
        this.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 根据Android版本设置合适的softInputMode，新版本优先使用ADJUST_RESIZE
        softInputMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        } else {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }

        // 简化触摸拦截逻辑
        this.setTouchInterceptor { _, event ->
            // 当不允许外部触摸时，将事件传递给contentView
            if (!isOutsideTouchable && contentView != null) {
                contentView.dispatchTouchEvent(event)
            }
            // 只有当isFocusable为true且不允许外部触摸时，才拦截事件
            isFocusable && !isOutsideTouchable
        }
    }

    // 使用属性替代companion object中的lateinit var
    open var tag: String = ""

    // contentView非空检查，避免空指针异常
    open fun show() {
        contentView?.let {
            if (!isShowing) {
                showAtLocation(it, Gravity.CENTER, 0, 0)
            }
        }
    }

    open fun show(gravity: Int) {
        contentView?.let {
            if (!isShowing) {
                showAtLocation(it, gravity, 0, 0)
            }
        }
    }

    open fun show(gravity: Int, offsetX: Int, offsetY: Int) {
        contentView?.let {
            if (!isShowing) {
                showAtLocation(it, gravity, offsetX, offsetY)
            }
        }
    }

    open fun show(offsetX: Int, offsetY: Int) {
        contentView?.let {
            if (!isShowing) {
                showAtLocation(it, Gravity.TOP or Gravity.LEFT, offsetX, offsetY)
            }
        }
    }

    // 优化showAtTopStart方法，添加contentView非空检查
    open fun showAtTopStart(view: View){
        showAtTopStart(view, 0)
    }

    open fun showAtTopStart(view: View, offsetY: Int){
        contentView?.let {contentView ->
            val location = IntArray(2)
            view.getLocationInWindow(location)

            contentView.run {
                measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                showAtLocation(view, Gravity.NO_GRAVITY, location[0], location[1] - measuredHeight - offsetY)
            }
        }
    }

    open fun showAtTopCenter(view: View){
        showAtTopCenter(view, 0)
    }

    open fun showAtTopCenter(view: View, offsetY: Int){
        contentView?.let {contentView ->
            val location = IntArray(2)
            view.getLocationInWindow(location)

            contentView.run {
                measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                showAtLocation(view, Gravity.NO_GRAVITY, location[0] + view.width / 2 - measuredWidth / 2, location[1] - measuredHeight - offsetY)
            }
        }
    }

    open fun showAtBottomCenter(view: View) {
        showAtBottomCenter(view, 0)
    }

    open fun showAtBottomCenter(view: View, offsetY: Int){
        contentView?.let {contentView ->
            val location = IntArray(2)
            view.getLocationOnScreen(location)

            contentView.run {
                measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                showAtLocation(view, Gravity.NO_GRAVITY, location[0] + view.width / 2 - measuredWidth / 2, location[1] + view.height + offsetY)
            }
        }
    }

    open fun showAtRightCenter(view: View) {
        contentView?.let {contentView ->
            val location = IntArray(2)
            view.getLocationOnScreen(location)

            contentView.run {
                measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                showAtLocation(view, Gravity.NO_GRAVITY, location[0] + view.width, location[1] + view.height / 2 - measuredHeight / 2)
            }
        }
    }

    open fun showAtLeftCenter(view: View) {
        contentView?.let {contentView ->
            val location = IntArray(2)
            view.getLocationOnScreen(location)

            contentView.run {
                measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                showAtLocation(view, Gravity.NO_GRAVITY, location[0] - measuredWidth, location[1] + view.height / 2 - measuredHeight / 2)
            }
        }
    }

    // 优化showAtLocation方法，添加parent非空检查
    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        parent?.let {
            if (!isShowing) {
                super.showAtLocation(it, gravity, x, y)
            }
        }
    }

    // 简化dismiss方法实现
    override fun dismiss() {
        if (allowDismiss) {
            super.dismiss()
        }
    }

    open var allowDismiss = true
}