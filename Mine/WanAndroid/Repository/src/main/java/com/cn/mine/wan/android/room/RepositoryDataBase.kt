package com.cn.mine.wan.android.room

import androidx.room.Room
import androidx.room.RoomDatabase
import com.cn.mine.wan.android.repository.RepositoryContextExt.context

/**
 * @Author: CuiNing
 * @Time: 2025/6/14 17:03
 * @Description:
 */

abstract class RepositoryDataBase constructor(): RoomDatabase() {

    companion object {

        @Volatile
        private var INSTANCE: RepositoryDataBase? = null

        fun initDataBase(): RepositoryDataBase {
            return INSTANCE?: synchronized(this) {
                val instance = Room.databaseBuilder(context, RepositoryDataBase::class.java, "db_repository")
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

}