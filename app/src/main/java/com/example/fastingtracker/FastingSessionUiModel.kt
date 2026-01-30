package com.example.fastingtracker.data

import java.time.LocalDateTime

data class FastingSessionUiModel(
    val id: Int,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val goalHours: Float,
    val durationHours: Double
)