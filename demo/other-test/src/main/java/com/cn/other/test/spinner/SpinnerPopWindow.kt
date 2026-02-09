package com.cn.other.test.spinner

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cn.other.test.R
import androidx.core.graphics.drawable.toDrawable
import java.lang.ref.WeakReference

/**
 * Created by wt on 2021/1/4.
 */
internal class SpinnerPopWindow(context: Context?) : PopupWindow(
    context
), PopupWindow.OnDismissListener {
    private val mContext: WeakReference<Context?>
    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: SpinnerAdapter? = null
    private var mItemSelectListener: WeakReference<Spinner.OnItemSelectedListener>? = null
    private var dismissListener: WeakReference<Spinner.OnDismissListener>? = null

    init {
        mContext = WeakReference(context)
        init()
    }

    private fun getContext(): Context? = mContext.get()

    private fun getItemSelectListener(): Spinner.OnItemSelectedListener? = mItemSelectListener?.get()

    private fun getDismissListener(): Spinner.OnDismissListener? = dismissListener?.get()

    fun setItemListener(listener: Spinner.OnItemSelectedListener?) {
        mItemSelectListener = listener?.let { WeakReference(it) }
    }

    fun setDismissListener(dismiss: Spinner.OnDismissListener?) {
        dismissListener = dismiss?.let { WeakReference(it) }
    }

    private fun init() {
        getContext()?.let {
            val view = LayoutInflater.from(it).inflate(R.layout.window_spinner, null)
            setContentView(view)
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT

            val dw = 0x00.toDrawable()
            setBackgroundDrawable(dw)
            mRecyclerView = view.findViewById<RecyclerView?>(R.id.recyclerView)
            mRecyclerView?.isVerticalScrollBarEnabled = true
            val linearLayoutManager = LinearLayoutManager(it)
            mRecyclerView?.layoutManager = linearLayoutManager
            //设置边距
//        mRecyclerView.addItemDecoration(new SpaceItemDecoration(20));
            isFocusable = true
        }
    }

    fun setAdapter(adapter: SpinnerAdapter) {
        mAdapter = adapter
        mRecyclerView?.adapter = mAdapter
        mAdapter!!.setOnItemClickListener(object : SpinnerAdapter.OnItemClickListener {
            override fun setOnItemClick(position: Int) {
                dismiss()
                getItemSelectListener()?.onItemSelected(position)
            }

            override fun onFocusChange(
                imageView: ImageView,
                textView: TextView?,
                hasFocus: Boolean,
                isSelected: Boolean
            ) {
                if (hasFocus) {
                    imageView.setImageResource(R.drawable.spinner_focus_selected)
                } else {
                    imageView.setImageResource(R.drawable.spinner_selected)
                }
            }
        })
    }

    fun setRequestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mRecyclerView?.setFocusedByDefault(true)
        }
        mRecyclerView?.setFocusable(true)
        mRecyclerView?.requestFocus()
        mRecyclerView?.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS)
    }

    override fun setHeight(height: Int) {
        super.setHeight(height)
        Log.d(SpinnerPopWindow::class.simpleName, "setHeight:$height")
        val params = mRecyclerView?.layoutParams
        val paddingTop = mRecyclerView?.paddingTop?:0
        val paddingBottom = mRecyclerView?.paddingBottom?:0
        val newHeight = height - paddingTop - paddingBottom
        params?.height = newHeight
        mRecyclerView?.setLayoutParams(params)
        // 强制请求重新布局
        mRecyclerView?.postInvalidate()
        /*with(contentView) {
            layoutParams = layoutParams.apply { this.height = height }
        }*/
        contentView?.postInvalidate()
    }

    /**
     * 滚动到指定位置，使选中项在列表中可见
     * @param position 目标位置索引，-1 表示不滚动
     */
    fun scrollToPosition(position: Int) {
        if (position >= 0 && mRecyclerView != null) {
            // 使用 post 确保在布局完成后执行
            mRecyclerView?.post(Runnable {
                val layoutManager =
                    mRecyclerView?.layoutManager as LinearLayoutManager?
                if (layoutManager != null) {
                    // 将选中项滚动到列表中间位置
                    val offset = mRecyclerView?.height?.div(2)?:0
                    layoutManager.scrollToPositionWithOffset(position, offset)
                }
            })
        }
    }

    override fun onDismiss() {
        getDismissListener()?.dismiss()
    }
}
