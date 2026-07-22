package com.cn.app.test.binder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.cn.app.test.R
import com.cn.app.test.model.User
import com.cn.core.ui.view.recyclerview.adapter.binder.BaseItemBinder
import com.cn.core.ui.view.recyclerview.adapter.holder.BaseViewHolder

class UserBinder : BaseItemBinder<User, BaseViewHolder>() {

    companion object {
        const val KEY_NAME = "KEY_NAME"
        const val KEY_LIKES = "KEY_LIKES"
        const val KEY_FOLLOW = "KEY_FOLLOW"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return BaseViewHolder(view)
    }

    override fun convert(holder: BaseViewHolder, data: User) {
        holder.setText(R.id.tv_name, data.name)
        holder.setText(R.id.tv_likes, "${data.likes} 点赞")
        holder.setText(R.id.btn_follow, if (data.isFollowed) "已关注" else "关注")
        holder.getView<View>(R.id.btn_follow)?.setOnClickListener {
            onFollowClick(data, holder.adapterPosition)
        }
    }

    override fun convert(holder: BaseViewHolder, data: User, payloads: List<Any>) {
        payloads.forEach { payload ->
            if (payload is Bundle) {
                if (payload.containsKey(KEY_NAME)) {
                    holder.setText(R.id.tv_name, data.name)
                }
                if (payload.containsKey(KEY_LIKES)) {
                    holder.setText(R.id.tv_likes, "${data.likes} 点赞")
                }
                if (payload.containsKey(KEY_FOLLOW)) {
                    holder.setText(R.id.btn_follow, if (data.isFollowed) "已关注" else "关注")
                }
            }
        }
    }

    private fun onFollowClick(user: User, position: Int) {
        val newUser = user.copy(isFollowed = !user.isFollowed)
        adapter.data[position] = newUser
        val bundle = Bundle()
        bundle.putBoolean(KEY_FOLLOW, newUser.isFollowed)
        adapter.notifyItemChanged(position, bundle)
    }

    class DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.name == newItem.name &&
                   oldItem.likes == newItem.likes &&
                   oldItem.isFollowed == newItem.isFollowed
        }

        override fun getChangePayload(oldItem: User, newItem: User): Any? {
            val diff = Bundle()
            if (oldItem.name != newItem.name) {
                diff.putString(KEY_NAME, newItem.name)
            }
            if (oldItem.likes != newItem.likes) {
                diff.putInt(KEY_LIKES, newItem.likes)
            }
            if (oldItem.isFollowed != newItem.isFollowed) {
                diff.putBoolean(KEY_FOLLOW, newItem.isFollowed)
            }
            return if (diff.isEmpty) null else diff
        }
    }
}
