package com.example.fastingtracker.data

import kotlinx.coroutines.flow.Flow

class FastingRepository(private val fastingDao: FastingDao) {

    fun getAllSessions(): Flow<List<FastingSessionEntity>> =
        fastingDao.getAllSessions()

    suspend fun insertSession(
        startTimeMillis: Long,
        endTimeMillis: Long,
        goalHours: Float
    ): Long {
        val session = FastingSessionEntity(
            startTime = startTimeMillis,
            endTime = endTimeMillis,
            goalHours = goalHours,
            createdAt = System.currentTimeMillis()
        )
        return fastingDao.insertSession(session)
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
