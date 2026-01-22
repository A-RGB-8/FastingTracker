package com.example.fastingtracker

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

// We change to AndroidViewModel to get access to "context" for storage
class FastingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("fasting_prefs", Context.MODE_PRIVATE)

    private val _isFasting = MutableStateFlow(prefs.getBoolean("is_fasting", false))
    val isFasting: StateFlow<Boolean> = _isFasting

    private val _timerDisplay = MutableStateFlow("00:00:00")
    val timerDisplay: StateFlow<String> = _timerDisplay

    private var startTimeMillis: Long = prefs.getLong("start_time", 0L)

    init {
        if (_isFasting.value) startTimer()
    }

    fun startFast(customTime: Long? = null) {
        startTimeMillis = customTime ?: System.currentTimeMillis()
        _isFasting.value = true

        // Save to Persistent Storage
        prefs.edit().putLong("start_time", startTimeMillis).putBoolean("is_fasting", true).apply()
        startTimer()
    }

    fun endFast(): String {
        val endTime = System.currentTimeMillis()
        val diff = endTime - startTimeMillis
        val result = formatMillis(diff)

        _isFasting.value = false
        _timerDisplay.value = "00:00:00"

        // Save only the last result to persistence
        val historyEntry = "Last fast: $result"
        prefs.edit()
            .putBoolean("is_fasting", false)
            .putString("last_result", historyEntry)
            .apply()

        return result
    }

    // Add this to retrieve the last saved result when the app starts
    fun getLastResult(): String {
        return prefs.getString("last_result", "No previous data") ?: "No previous data"
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (_isFasting.value) {
                val diff = System.currentTimeMillis() - startTimeMillis
                if (diff > 0) {
                    _timerDisplay.value = formatMillis(diff)
                } else {
                    _timerDisplay.value = "Starting soon..."
                }
                delay(1000)
            }
        }
    }

    private fun formatMillis(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun startFastAtTime(timeString: String) {
        val calendar = Calendar.getInstance()
        val parts = timeString.split(":")
        if (parts.size == 2) {
            calendar.set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: calendar.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
            calendar.set(Calendar.SECOND, 0)

            // If the entered time is in the future, assume it was for yesterday
            if (calendar.timeInMillis > System.currentTimeMillis()) {
                calendar.add(Calendar.DATE, -1)
            }

            startFast(calendar.timeInMillis)
        }
    }

    fun endFastAtTime(timeString: String): String {
        val calendar = Calendar.getInstance()
        val parts = timeString.split(":")
        if (parts.size == 2) {
            calendar.set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: calendar.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
            calendar.set(Calendar.SECOND, 0)

            // Safety check: if end time is set in the future, assume it was earlier today or yesterday
            if (calendar.timeInMillis > System.currentTimeMillis()) {
                calendar.add(Calendar.DATE, -1)
            }

            // Calculate duration based on manual end time
            val diff = calendar.timeInMillis - startTimeMillis
            val result = formatMillis(diff)

            // Reset state
            _isFasting.value = false
            _timerDisplay.value = "00:00:00"
            prefs.edit().putBoolean("is_fasting", false).putString("last_result", "Last fast: $result").apply()

            return result
        }
        return "00:00:00"
    }

    fun getDefaultHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
}