package com.cn.sample.test.main

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cn.library.common.recyclerview.adapter.BaseBinderAdapter
import com.cn.library.common.recyclerview.adapter.binder.QuickDataBindingItemBinder
import com.cn.sample.test.databinding.ItemMainEntranceBinding

/**
 * @Author: CuiNing
 * @Time: 2025/11/6 16:02
 * @Description:
 */
class MainEntranceListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): RecyclerView(context, attrs, defStyleAttr) {

    private val mAdapter = BaseBinderAdapter()

    init {
        layoutManager = GridLayoutManager(context, 2)
        adapter = mAdapter.apply {

        }
    }

    inner class MainEntranceBinder: QuickDataBindingItemBinder<String, ItemMainEntranceBinding>() {

        override fun onCreateDataBinding(
            layoutInflater: LayoutInflater,
            parent: ViewGroup,
            viewType: Int,
        ): ItemMainEntranceBinding = ItemMainEntranceBinding.inflate(layoutInflater, parent, false)

        override fun convert(
            holder: BinderDataBindingHolder<ItemMainEntranceBinding>,
            data: String,
        ) = with(holder.dataBinding) {
            content.text = data
        }

        override fun onClick(
            holder: BinderDataBindingHolder<ItemMainEntranceBinding>,
            view: View,
            data: String,
            position: Int
        ) {
            super.onClick(holder, view, data, position)
        }
    }
}