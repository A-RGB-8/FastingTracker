package com.example.fastingtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingDao {
    @Insert
    suspend fun insertSession(session: FastingSessionEntity): Long

    @Delete
    suspend fun deleteSession(session: FastingSessionEntity)

    @Query("SELECT * FROM fasting_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<FastingSessionEntity>>

    @Query("SELECT * FROM fasting_sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): FastingSessionEntity?

    @Query("SELECT COUNT(*) FROM fasting_sessions")
    suspend fun getSessionCount(): Int

    @Query("SELECT AVG(endTime - startTime) / 3600000.0 FROM fasting_sessions")
    suspend fun getAverageDurationHours(): Double?

    @Query("SELECT * FROM fasting_sessions ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastSession(): FastingSessionEntity?
}
