package com.example.fastingtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingDao {
    // Corrected: Explicitly using OnConflictStrategy.REPLACE
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FastingSessionEntity)

    @Update
    suspend fun updateSession(session: FastingSessionEntity)

    @Delete
    suspend fun deleteSession(session: FastingSessionEntity)

    @Query("SELECT * FROM fasting_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FastingSessionEntity>>
}