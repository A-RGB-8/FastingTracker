package com.example.fastingtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FastingSessionEntity::class], version = 1)
abstract class FastingDatabase : RoomDatabase() {
    abstract fun fastingDao(): FastingDao

    companion object {
        @Volatile
        private var instance: FastingDatabase? = null

        fun getInstance(context: Context): FastingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FastingDatabase::class.java,
                    "fasting_database"
                ).build().also { instance = it }
            }
    }
}
