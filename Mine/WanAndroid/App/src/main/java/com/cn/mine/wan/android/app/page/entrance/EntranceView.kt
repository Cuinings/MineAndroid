package com.cn.mine.wan.android.app.page.entrance

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cn.library.common.recyclerview.adapter.BaseBinderAdapter
import com.cn.library.common.recyclerview.adapter.binder.QuickDataBindingItemBinder
import com.cn.mine.wan.android.app.databinding.ItemEntranceBinding
import com.cn.mine.wan.android.entity.EntranceEntity

/**
 * @Author: CuiNing
 * @Time: 2025/6/10 11:51
 * @Description: 入口view
 */
class EntranceView: RecyclerView {

    private var mAdapter = EntranceAdapter()

    var onClickListener: OnClickListener? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
    @SuppressLint("ResourceType")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        initializer()
    }

    private fun initializer() {
        layoutManager = GridLayoutManager(context, 3)
        adapter = mAdapter.apply {
            addItemBinder(EntranceItem())
            setList(data)
        }
    }

    var data: MutableList<EntranceEntity> = arrayListOf()
        set(value) {
            field = value
            mAdapter.setList(field)
        }

    inner class EntranceAdapter: BaseBinderAdapter(arrayListOf())

    inner class EntranceItem: QuickDataBindingItemBinder<EntranceEntity, ItemEntranceBinding>() {

        override fun onCreateDataBinding(layoutInflater: LayoutInflater, parent: ViewGroup, viewType: Int): ItemEntranceBinding =
            ItemEntranceBinding.inflate(layoutInflater, parent, false)

        override fun convert(holder: BinderDataBindingHolder<ItemEntranceBinding>, data: EntranceEntity) {
            holder.dataBinding.entrance = data
        }

        override fun onClick(holder: BinderDataBindingHolder<ItemEntranceBinding>, view: View, data: EntranceEntity, position: Int) {
            super.onClick(holder, view, data, position)
            onClickListener?.onClick(data)
        }

    }

    interface OnClickListener {
        fun onClick(entrance: EntranceEntity)
    }
}