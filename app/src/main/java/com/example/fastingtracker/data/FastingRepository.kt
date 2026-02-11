package com.example.fastingtracker.data

import kotlinx.coroutines.flow.Flow

class FastingRepository(private val fastingDao: FastingDao) {

    fun getAllSessions(): Flow<List<FastingSessionEntity>> = fastingDao.getAllSessions()

    suspend fun insertSession(start: Long, end: Long, goal: Float) {
        val duration = (end - start) / 3600000.0
        val entity = FastingSessionEntity(
            startTime = start,
            endTime = end,
            goalHours = goal,
            durationHours = duration
        )
        fastingDao.insertSession(entity)
    }

    suspend fun updateSession(session: FastingSessionEntity) {
        fastingDao.updateSession(session)
    }

    suspend fun deleteSession(session: FastingSessionEntity) {
        fastingDao.deleteSession(session)
    }
}