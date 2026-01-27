package com.example.fastingtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "fasting_sessions")
data class FastingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startTime: Long, // milliseconds since epoch
    val endTime: Long,   // milliseconds since epoch
    val goalHours: Float,
    val createdAt: Long  // timestamp when record was created
) {
    val durationHours: Double
        get() = (endTime - startTime) / 3600000.0
}
