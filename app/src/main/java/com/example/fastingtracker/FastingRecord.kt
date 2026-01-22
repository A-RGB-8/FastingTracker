package com.example.fastingtracker

import java.util.Date

data class FastingRecord(
    val startTimeMillis: Long,
    val endTimeMillis: Long
) {
    // Derived property to calculate duration in hours
    val durationHours: Double
        get() = (endTimeMillis - startTimeMillis) / 3600000.0
}