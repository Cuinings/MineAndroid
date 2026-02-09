package com.cn.other.test.spinner

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.cn.other.test.R
import androidx.core.content.withStyledAttributes

import java.lang.ref.WeakReference

class Spinner : LinearLayout {
    private var tv_value: TextView? = null
    private var bt_dropdown: ImageView? = null
    private var ll_content: LinearLayout? = null
    private val selfContext: WeakReference<Context>
    private var mItems: MutableList<String> = mutableListOf()
    private var listener: WeakReference<OnItemSelectedListener>? = null
    private var mItemClickListener: WeakReference<OnItemClickListener>? = null
    private var mOnSpinnerDismissListener: WeakReference<OnSpinnerDismissListener>? = null

    private var mSpinerPopWindow: SpinnerPopWindow? = null
    private var mAdapter: SpinnerAdapter? = null
    private var currSelectItem = 0
    var myView: View? = null
    var isUpdateClick: Boolean = false
    var isDisallowClick: Boolean = false //标识是否弹出下拉框
    private var mOnShowClickListener: WeakReference<OnShowClickListener>? = null


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        selfContext = WeakReference(context)
        initAttrs(attrs)
        init()
    }

    private fun getSelfCtx(): Context? = selfContext.get()

    private fun getListener(): OnItemSelectedListener? = listener?.get()

    private fun getItemClickListener(): OnItemClickListener? = mItemClickListener?.get()

    private fun getOnSpinnerDismissListener(): OnSpinnerDismissListener? = mOnSpinnerDismissListener?.get()

    private fun getOnShowClickListener(): OnShowClickListener? = mOnShowClickListener?.get()

    private fun initAttrs(attrs: AttributeSet?) {
        if (attrs == null) {
            return
        }
        getSelfCtx()?.withStyledAttributes(attrs, R.styleable.Spinner) {
            isUpdateClick = getBoolean(R.styleable.Spinner_updateClick, true)
        }
    }

    @SuppressLint("InflateParams")
    private fun init() {
        val mInflater = LayoutInflater.from(getSelfCtx())
        myView = mInflater.inflate(R.layout.spinner, null, false)
        addView(myView)


        tv_value = myView?.findViewById<View?>(R.id.tvValue) as TextView?
        bt_dropdown = myView?.findViewById<View?>(R.id.icArrow) as ImageView?
        ll_content = myView?.findViewById<View?>(R.id.llContent) as LinearLayout
        tv_value?.setOnClickListener(mOnClick)
        bt_dropdown?.setOnClickListener(mOnClick)
        tv_value?.setTextColor(createColorStateList())
        ll_content?.setOnClickListener(mOnClick)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        tv_value?.setEnabled(enabled)
        bt_dropdown?.setEnabled(enabled)
        ll_content?.setEnabled(enabled)
        if (enabled) {
            tv_value?.setTextColor(createColorStateList())
        }
        tv_value?.setTextColor(resources.getColor(R.color.text_step_33, null))
    }

    private fun createColorStateList(): ColorStateList {
        val colors = intArrayOf(
            resources.getColor(R.color.light, null),
            resources.getColor(R.color.text_step_cc, null)
        )
        val states = arrayOfNulls<IntArray>(2)
        states[0] = intArrayOf(android.R.attr.state_pressed)
        states[1] = intArrayOf()
        val colorList = ColorStateList(states, colors)
        return colorList
    }

    val mOnClick: OnClickListener = object : OnClickListener {
        override fun onClick(v: View?) {
            if (!isDisallowClick) {
                bt_dropdown?.setImageResource(R.drawable.spinner_drop_down_selected)
                startPopWindow()
            } else {
                getOnShowClickListener()?.onShowSpinWindow()
            }
        }
    }

    fun setData(list: MutableList<String>) {
        mItems = list
    }

    fun setCurrSelectItem(currSelectItem: Int) {
        if (mItems.size <= currSelectItem) this.currSelectItem = 0
        else this.currSelectItem = currSelectItem
        if (mItems.isNotEmpty()) {
            tv_value?.text = mItems[this.currSelectItem]
        }
        if (isUpdateClick) getListener()?.onItemSelected(this.currSelectItem)
    }

    fun getCurrSelectItem(): Int {
        return currSelectItem
    }

    val currentItem: String?
        get() {
            if (mItems.isEmpty() || currSelectItem >= mItems.size) return ""
            return mItems[currSelectItem]
        }

    fun getCurrentItem(index: Int): String? {
        if (mItems.isEmpty() || index >= mItems.size) return ""
        return mItems[index]
    }

    fun setTextViewColor(textColor: Int) {
        tv_value?.setTextColor(textColor)
    }

    fun setTextViewImgFocus() {
        bt_dropdown?.setImageResource(R.drawable.spinner_drop_down_focus)
    }

    fun setTextViewImgDisFocus() {
        bt_dropdown?.setImageResource(R.drawable.spinner_drop_down_normal)
    }

    fun setDropDownImg(hasFocus: Boolean) {
        bt_dropdown?.setImageResource(if (hasFocus) R.drawable.spinner_drop_down_focus else R.drawable.spinner_drop_down_normal)
    }

    fun setSpinnerValueFocus(hasFocus: Boolean) {
        getContext()?.let {
            tv_value?.setTextColor(
                it.resources.getColor(if (hasFocus) R.color.white else R.color.text_step_cc)
            )
        }
        bt_dropdown?.setImageResource(if (hasFocus) R.drawable.spinner_drop_down_focus else R.drawable.spinner_drop_down_normal)
    }

    /**
     * 直接设置显示文本，用于显示不在列表中的自定义值
     * 此方法会将 currSelectItem 设为 -1，表示无选中项
     * 当用户打开下拉列表时，不会有任何项显示为选中状态
     *
     * @param text 要显示的文本
     */
    fun setValueText(text: String?) {
        if (tv_value != null && text != null) {
            tv_value?.text = text
            // 设置为 -1 表示当前显示的是自定义值，不对应列表中的任何项
            currSelectItem = -1
        }
    }

    fun resetData() {
        if (tv_value != null) {
            tv_value?.text = ""
        }
        mItems.clear()
    }

    fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        this.listener = listener?.let { WeakReference(it) }
    }

    fun setItemClickListener(listener: OnItemClickListener?) {
        this.mItemClickListener = listener?.let { WeakReference(it) }
    }

    fun setShowSpinnerWindow(listener: OnShowClickListener) {
        this.mOnShowClickListener = WeakReference(listener)
    }

    fun setOnSpinnerDismissListener(onSpinnerDismissListener: OnSpinnerDismissListener?) {
        this.mOnSpinnerDismissListener = onSpinnerDismissListener?.let { WeakReference(it) }
    }

    fun startPopWindow() {
        if (null != tv_value) {
            tv_value?.setTextColor(resources.getColor(R.color.light, null))
        }
        if (null != bt_dropdown) {
            bt_dropdown?.setSelected(true)
        }
        getContext()?.let {
            mAdapter = SpinnerAdapter(it, mItems)
            mAdapter?.refreshData(currSelectItem)

            mSpinerPopWindow = SpinnerPopWindow(it)
        }
        mAdapter?.let { mSpinerPopWindow?.setAdapter(it) }
        mSpinerPopWindow?.setItemListener(object : OnItemSelectedListener {
            override fun onItemSelected(pos: Int) {
                tv_value?.text = mItems[pos]
                currSelectItem = pos
                getListener()?.onItemSelected(pos)
                getItemClickListener()?.onItemClick()
            }
        })
        mSpinerPopWindow?.setOnDismissListener(PopupWindow.OnDismissListener {
            tv_value?.setTextColor(resources.getColor(R.color.text_step_cc))
            bt_dropdown?.setSelected(false)
            bt_dropdown?.setImageResource(R.drawable.spinner_drop_down_normal)
            getOnSpinnerDismissListener()?.onSpinnerDismiss()
        })
        showSpinWindow()
        mSpinerPopWindow?.setRequestFocus()
        // 滚动到当前选中项
        mSpinerPopWindow?.scrollToPosition(currSelectItem)
    }

    // 在 Spinner.java 的 showSpinWindow() 方法中
    @SuppressLint("RtlHardcoded")
    private fun showSpinWindow() {
        mSpinerPopWindow?.showAsDropDown(myView, -420, 0, Gravity.RIGHT or Gravity.BOTTOM)
        post {
            if (mItems.size >= 6) {
                mSpinerPopWindow?.height = resources.getDimensionPixelSize(R.dimen.dp150)
            }
        }
    }

    fun dismissSpinWindow() {
        if (mSpinerPopWindow != null) {
            mSpinerPopWindow?.dismiss()
        }
    }

    interface OnItemSelectedListener {
        fun onItemSelected(pos: Int)
    }

    interface OnDismissListener {
        fun dismiss()
    }

        interface OnSpinnerDismissListener {
        fun onSpinnerDismiss()
    }

    /**
     * 该接口判断是否是用户点击的回调
     */
    interface OnItemClickListener {
        fun onItemClick()
    }

    interface OnShowClickListener {
        fun onShowSpinWindow()
    }
}
