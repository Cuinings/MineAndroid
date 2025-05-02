package com.cn.sample.test.natural

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cn.library.common.recyclerview.adapter.BaseNodeAdapter
import com.cn.library.common.recyclerview.adapter.entity.node.BaseNode
import com.cn.library.common.recyclerview.adapter.holder.BaseViewHolder
import com.cn.library.common.recyclerview.adapter.provider.BaseNodeProvider
import com.cn.sample.test.R

class ExpandList: RecyclerView {

    private val mAdapter = NaturalListAdapter()

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    @SuppressLint("ResourceType", "NewApi")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        layoutManager = LinearLayoutManager(context)
        adapter = mAdapter.apply {
            addChildClickViewIds(R.id.llExpand)
        }
        data = ExpandData.naturalCommonData.child
    }

    var data: MutableList<BaseNode>? = arrayListOf()
        set(value) {
            field = value
            mAdapter.setList(value)
        }


    inner class NaturalListItemProvider(override val itemViewType: Int) : BaseNodeProvider() {

        init {
            addChildClickViewIds(R.id.llExpand, R.id.ivCheck)
        }

        override val layoutId: Int get() = R.layout.item_command_dispatcher_natural

        override fun convert(helper: BaseViewHolder, item: BaseNode) {
            item as ExpandNode
            resources.getDimensionPixelSize(R.dimen.dp14).let { helper.itemView.setPadding(it * item.level, 0, 0, 0) }
            helper.setText(R.id.naturalName, item.name)
            helper.setImageResource(R.id.ivExpand, if (item.isExpanded) R.drawable.selector_icon_expand_on else R.drawable.selector_icon_expand_off)
            helper.setImageResource(R.id.ivCheck, if (item.selected) R.drawable.selector_choice_on else R.drawable.selector_choice_off)
            helper.setImageResource(R.id.ivDevice, if (item.online) R.drawable.selector_icon_ipc_online else R.drawable.selector_icon_ipc_offline)
            helper.setGone(R.id.ivDevice, item.group)
            helper.setGone(R.id.llExpand, !item.group)
        }

        override fun onClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) {
            super.onClick(helper, view, data, position)
//            mAdapter.expandOrCollapse(position, true, true)
        }

        override fun onChildClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) {
            super.onChildClick(helper, view, data, position)
            when(view.id) {
                R.id.llExpand -> {
//                    data as ExpandNode
//                    data.isExpanded = !data.isExpanded
                    mAdapter.expandOrCollapse(position, true, true)
                }
            }
        }
    }

    inner class NaturalListAdapter: BaseNodeAdapter() {

        init {
            addFullSpanNodeProvider(NaturalListItemProvider(0))
        }

        override fun getItemType(data: List<BaseNode>, position: Int): Int {
            return 0
        }

    }
}
