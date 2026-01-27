package com.example.fastingtracker.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("fasting_tracker_prefs", Context.MODE_PRIVATE)

    fun getLastFastDuration(): String {
        return sharedPreferences.getString("last_fast_duration", "0") ?: "0"
    }

    fun setLastFastDuration(duration: String) {
        sharedPreferences.edit().putString("last_fast_duration", duration).apply()
    }

    fun getDefaultFastingHours(): Float {
        return sharedPreferences.getFloat("default_fasting_hours", 16f)
    }

    fun setDefaultFastingHours(hours: Float) {
        sharedPreferences.edit().putFloat("default_fasting_hours", hours).apply()
    }

    fun getCurrentSessionStartTime(): Long {
        return sharedPreferences.getLong("current_session_start_time", 0L)
    }

    fun setCurrentSessionStartTime(timeMillis: Long) {
        sharedPreferences.edit().putLong("current_session_start_time", timeMillis).apply()
    }

    fun getCurrentSessionGoal(): Float {
        return sharedPreferences.getFloat("current_session_goal", 0f)
    }

    fun setCurrentSessionGoal(hours: Float) {
        sharedPreferences.edit().putFloat("current_session_goal", hours).apply()
    }

    fun clearCurrentSession() {
        sharedPreferences.edit().apply {
            putLong("current_session_start_time", 0L)
            putFloat("current_session_goal", 0f)
        }.apply()
    }
}
