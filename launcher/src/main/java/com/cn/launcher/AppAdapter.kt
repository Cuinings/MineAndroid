package com.cn.launcher

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class AppAdapter(
    context: Context,
    private val appList: List<AppInfo>
) : ArrayAdapter<AppInfo>(context, 0, appList) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_app, parent, false)

        val appInfo = appList[position]

        val appIcon = view.findViewById<ImageView>(R.id.app_icon)
        val appName = view.findViewById<TextView>(R.id.app_name)

        // 应用自定义设置
        applyCustomSettings(appIcon, appName)

        appIcon.setImageDrawable(appInfo.appIcon)
        appName.text = appInfo.appName

        return view
    }

    private fun applyCustomSettings(icon: ImageView, name: TextView) {
        // 获取自定义设置值
        val iconSize = sharedPreferences.getInt("icon_size", 50)
        val fontSize = sharedPreferences.getInt("font_size", 12)

        // 应用图标大小
        val layoutParams = icon.layoutParams
        layoutParams.width = iconSize
        layoutParams.height = iconSize
        icon.layoutParams = layoutParams

        // 应用字体大小
        name.textSize = fontSize.toFloat()
    }
}
