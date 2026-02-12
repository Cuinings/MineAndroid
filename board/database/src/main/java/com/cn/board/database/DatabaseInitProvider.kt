package com.cn.board.database

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log

/**
 * 无侵入式数据库初始化 Provider
 * 利用 ContentProvider 的自动初始化机制，在应用启动时自动初始化数据库
 */
class DatabaseInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        // 初始化数据库
        context?.let {
            Log.d("DatabaseInit", "开始自动初始化数据库")
            DatabaseManager.initDatabase(it)
            Log.d("DatabaseInit", "数据库初始化完成")
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
