package com.example.fastingtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FastingSessionEntity::class], version = 2, exportSchema = false)
abstract class FastingDatabase : RoomDatabase() {
    abstract fun fastingDao(): FastingDao

    companion object {
        @Volatile private var instance: FastingDatabase? = null
        fun getInstance(context: android.content.Context): FastingDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FastingDatabase::class.java, "fasting_db"
                )
                .fallbackToDestructiveMigration() // This prevents the crash when schema changes
                .build().also { instance = it }
            }
        }
    }
}
