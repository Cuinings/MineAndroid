package com.cn.board.proxy

import android.content.Context
import com.cn.board.meet.home.HomeActivity
import com.cn.board.meet.MeetActivity
import com.cn.board.contacts.ContactsActivity

object ModuleConfig {
    
    // 模块名称常量
    const val MODULE_HOME = "home"
    const val MODULE_MEET = "meet"
    const val MODULE_CONTACTS = "contacts"
    
    /**
     * 检查指定模块是否启用
     */
    fun isModuleEnabled(moduleName: String): Boolean {
        return when (moduleName) {
            MODULE_HOME -> BuildConfig.ENABLE_HOME
            MODULE_MEET -> BuildConfig.ENABLE_MEET
            MODULE_CONTACTS -> BuildConfig.ENABLE_CONTACTS
            else -> false
        }
    }
    
    /**
     * 获取默认首页模块的Activity类
     * 如果默认模块未启用，则返回第一个启用的模块
     */
    fun getDefaultHomeActivityClass(): Class<*> {
        // 首先尝试使用配置的默认模块
        val defaultModule = BuildConfig.DEFAULT_HOME_MODULE
        if (isModuleEnabled(defaultModule)) {
            return getModuleActivityClass(defaultModule)
        }
        
        // 如果默认模块未启用，返回第一个启用的模块
        if (BuildConfig.ENABLE_HOME) return HomeActivity::class.java
        if (BuildConfig.ENABLE_MEET) return MeetActivity::class.java
        if (BuildConfig.ENABLE_CONTACTS) return ContactsActivity::class.java
        
        // 如果所有模块都未启用，返回默认的HomeActivity
        return HomeActivity::class.java
    }
    
    /**
     * 根据模块名称获取对应的Activity类
     */
    private fun getModuleActivityClass(moduleName: String): Class<*> {
        return when (moduleName) {
            MODULE_HOME -> HomeActivity::class.java
            MODULE_MEET -> MeetActivity::class.java
            MODULE_CONTACTS -> ContactsActivity::class.java
            else -> HomeActivity::class.java
        }
    }
    
    /**
     * 获取所有启用的模块名称列表
     */
    fun getEnabledModules(): List<String> {
        val enabledModules = mutableListOf<String>()
        if (BuildConfig.ENABLE_HOME) enabledModules.add(MODULE_HOME)
        if (BuildConfig.ENABLE_MEET) enabledModules.add(MODULE_MEET)
        if (BuildConfig.ENABLE_CONTACTS) enabledModules.add(MODULE_CONTACTS)
        return enabledModules
    }
    
    /**
     * 检查是否有任何模块启用
     */
    fun hasAnyModuleEnabled(): Boolean {
        return BuildConfig.ENABLE_HOME || BuildConfig.ENABLE_MEET || BuildConfig.ENABLE_CONTACTS
    }
}
