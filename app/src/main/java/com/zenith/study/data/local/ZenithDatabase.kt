package com.zenith.study.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zenith.study.data.local.dao.AlarmDao
import com.zenith.study.data.local.dao.FocusSessionDao
import com.zenith.study.data.local.entity.AlarmEntity
import com.zenith.study.data.local.entity.FocusSessionEntity

@Database(
    entities = [AlarmEntity::class, FocusSessionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ZenithDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        @Volatile
        private var INSTANCE: ZenithDatabase? = null

        fun getInstance(context: Context): ZenithDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZenithDatabase::class.java,
                    "zenith_study.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

