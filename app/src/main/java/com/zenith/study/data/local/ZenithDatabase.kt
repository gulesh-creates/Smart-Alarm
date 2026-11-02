package com.zenith.study.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zenith.study.data.local.dao.AlarmDao
import com.zenith.study.data.local.dao.FocusSessionDao
import com.zenith.study.data.local.dao.TodoDao
import com.zenith.study.data.local.entity.AlarmEntity
import com.zenith.study.data.local.entity.FocusSessionEntity
import com.zenith.study.data.local.entity.TodoItemEntity

@Database(
    entities = [AlarmEntity::class, FocusSessionEntity::class, TodoItemEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ZenithDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var INSTANCE: ZenithDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN showEmergencyDismiss INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE alarms ADD COLUMN toneUri TEXT")
                db.execSQL("ALTER TABLE alarms ADD COLUMN volume REAL NOT NULL DEFAULT 1.0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `todo_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `alarmId` INTEGER NOT NULL,
                        `occurrenceDateEpochDay` INTEGER NOT NULL,
                        `taskText` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): ZenithDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZenithDatabase::class.java,
                    "zenith_study.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
