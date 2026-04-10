package com.cn.tosetting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SettingAdapter(
    private val items: List<Setting>,
    private val onItemClick: (Setting) -> Unit
) : RecyclerView.Adapter<SettingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.iv_icon)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvDescription: TextView = view.findViewById(R.id.tv_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_setting, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val setting = items[position]
        holder.ivIcon.setImageResource(setting.iconResId)
        holder.tvTitle.text = setting.title
        holder.tvDescription.text = setting.description
        holder.itemView.setOnClickListener {
            onItemClick(setting)
        }
    }

    override fun getItemCount() = items.size
}
