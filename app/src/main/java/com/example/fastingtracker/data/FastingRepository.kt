package com.example.fastingtracker.data

import kotlinx.coroutines.flow.Flow

class FastingRepository(private val fastingDao: FastingDao) {

    fun getAllSessions(): Flow<List<FastingSessionEntity>> =
        fastingDao.getAllSessions()

    // Inside FastingRepository.kt
    suspend fun insertSession(startTime: Long, endTime: Long, goalHours: Float) {
        val durationHours = (endTime - startTime) / 3600000.0
        val session = FastingSessionEntity(
            startTime = startTime,
            endTime = endTime,
            goalHours = goalHours,
            durationHours = durationHours
        )
        fastingDao.insert(session)
    }

    suspend fun deleteSession(session: FastingSessionEntity) {
        fastingDao.deleteSession(session)
    }

    suspend fun getSessionCount(): Int =
        fastingDao.getSessionCount()

    suspend fun getAverageDurationHours(): Double? =
        fastingDao.getAverageDurationHours()

    suspend fun getLastSession(): FastingSessionEntity? =
        fastingDao.getLastSession()
}
