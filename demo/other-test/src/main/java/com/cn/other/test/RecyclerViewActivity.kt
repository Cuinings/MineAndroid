package com.cn.other.test

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.cn.core.ui.activity.BasicDBActivity
import com.cn.core.ui.view.recyclerview.adapter.BaseBinderAdapter
import com.cn.core.ui.view.recyclerview.adapter.binder.QuickDataBindingItemBinder
import com.cn.other.test.databinding.ActivityRecyclerviewBinding
import com.cn.other.test.databinding.ActivityRecyclerviewItemBinding

/**
 * @author: cn
 * @time: 2026/2/2 14:18
 * @history
 * @description:
 */
class RecyclerViewActivity: BasicDBActivity<ActivityRecyclerviewBinding>({
    ActivityRecyclerviewBinding.inflate(it)
})  {

    private val mAdapter = BaseBinderAdapter()

    val data = mutableListOf<Bean>().apply {
        for (i in 0 until 10) {
            add(Bean(
                id = 0,
                name = "name:i:$i",
                online = i % 2 == 0
            ))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(binding) {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(this@RecyclerViewActivity)
            }.adapter = mAdapter.apply {
                addItemBinder(BeanBinder())
            }
        }
    }

    override fun onBindLayout() {
    }

    inner class BeanBinder: QuickDataBindingItemBinder<Bean, ActivityRecyclerviewItemBinding>() {
        override fun onCreateDataBinding(
            layoutInflater: LayoutInflater,
            parent: ViewGroup,
            viewType: Int
        ): ActivityRecyclerviewItemBinding = ActivityRecyclerviewItemBinding.inflate(layoutInflater, parent, false)

        override fun convert(
            holder: BinderDataBindingHolder<ActivityRecyclerviewItemBinding>,
            data: Bean
        ) {
            with(holder.dataBinding) {
                name.text = data.name
                role.text = data.roleText()
            }
        }

        override fun convert(
            holder: BinderDataBindingHolder<ActivityRecyclerviewItemBinding>,
            data: Bean,
            payloads: List<Any>
        ) {
            super.convert(holder, data, payloads)
            with(holder.dataBinding) {
                payloads.forEach {
                    it as Bundle
                    when(it) {
                        "name" -> name.text = data.name
                    }
                }
            }
        }
    }

    class ParticipantBeanDiff: DiffUtil.ItemCallback<Bean>() {

        override fun getChangePayload(oldItem: Bean, newItem: Bean): Any? {
            return Bundle().apply {
                if (oldItem.name != newItem.name) putString("name", newItem.name)
                if (oldItem.chairman != newItem.chairman) putBoolean("chairman", newItem.chairman)
                if (oldItem.vip != newItem.vip) putBoolean("vip", newItem.vip)
                if (oldItem.speaker != newItem.speaker) putBoolean("speaker", newItem.speaker)
                if (oldItem.assSender != newItem.assSender) putBoolean("assSender", newItem.assSender)
            }
        }
        override fun areItemsTheSame(oldItem: Bean, newItem: Bean): Boolean {
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Bean, newItem: Bean): Boolean {
            return  oldItem.name == newItem.name
                    oldItem.chairman == newItem.chairman
                    && oldItem.speaker == newItem.speaker
                    && oldItem.assSender == newItem.assSender
                    && oldItem.vip == newItem.vip
        }
    }

    data class Bean(
        val id: Int = 0,
        val name: String = "",
        val chairman: Boolean = false,
        val vip: Boolean = false,
        val speaker: Boolean = false,
        val assSender: Boolean = false,
        val online: Boolean = false,
    )
    fun Bean.roleText(): String {
        val stringBuffer = StringBuffer()
        chairman.takeIf { it }?.let {
            stringBuffer.apply {
                takeIf { it.isNotEmpty() }?.append(" | ")
            }.append("管理员")
        }
        vip.takeIf { it }?.let {
            stringBuffer.apply {
                takeIf { it.isNotEmpty() }?.append(" | ")
            }.append("VIP")
        }
        speaker.takeIf { it }?.let {
            stringBuffer.apply {
                takeIf { it.isNotEmpty() }?.append(" | ")
            }.append("发言人")
        }
        assSender.takeIf { it }?.let {
            stringBuffer.apply {
                takeIf { it.isNotEmpty() }?.append(" | ")
            }.append("共享者")
        }
        return stringBuffer.toString()
    }


}