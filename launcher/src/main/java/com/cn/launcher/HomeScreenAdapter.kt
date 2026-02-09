package com.cn.launcher

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class HomeScreenAdapter(private val context: Context, private val items: MutableList<Any>) : BaseAdapter() {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false)
            holder = ViewHolder(
                view.findViewById(R.id.app_icon),
                view.findViewById(R.id.app_name)
            )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        // 应用自定义设置
        applyCustomSettings(holder)

        val item = items[position]
        when (item) {
            is AppInfo -> {
                holder.icon.setImageDrawable(item.appIcon)
                holder.name.text = item.appName
                // 显示通知徽章
                showNotificationBadge(holder, item)
            }
            is FolderInfo -> {
                // 使用自定义文件夹图标或默认图标
                if (item.folderIcon != null) {
                    holder.icon.setImageDrawable(item.folderIcon)
                } else {
                    holder.icon.setImageResource(android.R.drawable.ic_menu_gallery)
                }
                holder.name.text = item.folderName
                // 为文件夹添加长按事件
                setupFolderLongClick(view, item)
            }
            is WidgetInfo -> {
                // 使用默认小部件图标
                holder.icon.setImageResource(android.R.drawable.ic_dialog_info)
                holder.name.text = item.widgetName
                // 为小部件添加长按事件
                setupWidgetLongClick(view, item)
            }
            is ShortcutInfo -> {
                // 使用默认快捷方式图标
                holder.icon.setImageResource(android.R.drawable.ic_menu_send)
                holder.name.text = item.shortcutName
            }
        }

        return view
    }

    private fun setupWidgetLongClick(view: View, widgetInfo: WidgetInfo) {
        view.setOnLongClickListener {
            // 小部件长按事件
            // 这里可以显示小部件操作菜单
            // 模拟小部件操作
            Toast.makeText(context, "小部件长按: ${widgetInfo.widgetName}", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupFolderLongClick(view: View, folderInfo: FolderInfo) {
        view.setOnLongClickListener {
            // 文件夹长按事件
            // 这里可以显示文件夹操作菜单
            // 模拟文件夹操作
            Toast.makeText(context, "文件夹长按: ${folderInfo.folderName}", Toast.LENGTH_SHORT).show()
            // 简化处理，只是显示文件夹操作选项
            Toast.makeText(context, "可执行操作: 重命名、设置图标、排序应用", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun applyCustomSettings(holder: ViewHolder) {
        // 获取自定义设置值
        val iconSize = sharedPreferences.getInt("icon_size", 50)
        val fontSize = sharedPreferences.getInt("font_size", 12)

        // 应用图标大小
        val layoutParams = holder.icon.layoutParams
        layoutParams.width = iconSize
        layoutParams.height = iconSize
        holder.icon.layoutParams = layoutParams

        // 应用字体大小
        holder.name.textSize = fontSize.toFloat()
    }

    private fun showNotificationBadge(holder: ViewHolder, appInfo: AppInfo) {
        // 这里可以实现通知徽章的显示逻辑
        // 简单起见，我们只在控制台输出通知数量
        if (appInfo.notificationCount > 0) {
            println("App ${appInfo.appName} has ${appInfo.notificationCount} notifications")
            // 实际应用中，这里应该在图标上显示一个徽章
        }
    }

    private data class ViewHolder(val icon: ImageView, val name: TextView)
}
