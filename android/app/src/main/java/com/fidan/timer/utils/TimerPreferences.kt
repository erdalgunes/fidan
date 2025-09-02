package com.fidan.timer.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerPreferences private constructor(context: Context) {
    companion object {
        private const val PREFS_NAME = "timer_prefs"
        private const val KEY_SESSION_START_TIME = "session_start_time"
        private const val KEY_SESSION_COUNT = "session_count"
        private const val KEY_TOTAL_FOCUS_TIME = "total_focus_time"
        
        @Volatile
        private var INSTANCE: TimerPreferences? = null
        
        fun getInstance(context: Context): TimerPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TimerPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _sessionStats = MutableStateFlow(
        SessionStats(
            sessionCount = prefs.getInt(KEY_SESSION_COUNT, 0),
            totalFocusTimeMinutes = prefs.getLong(KEY_TOTAL_FOCUS_TIME, 0)
        )
    )
    val sessionStats: StateFlow<SessionStats> = _sessionStats.asStateFlow()

    fun saveSessionStartTime(startTime: Long) {
        prefs.edit().putLong(KEY_SESSION_START_TIME, startTime).apply()
    }

    fun getSessionStartTime(): Long {
        return prefs.getLong(KEY_SESSION_START_TIME, 0)
    }

    fun clearSessionStartTime() {
        prefs.edit().remove(KEY_SESSION_START_TIME).apply()
    }

    fun recordCompletedSession(durationMinutes: Int = 25) {
        val currentStats = _sessionStats.value
        val newStats = currentStats.copy(
            sessionCount = currentStats.sessionCount + 1,
            totalFocusTimeMinutes = currentStats.totalFocusTimeMinutes + durationMinutes
        )
        
        prefs.edit()
            .putInt(KEY_SESSION_COUNT, newStats.sessionCount)
            .putLong(KEY_TOTAL_FOCUS_TIME, newStats.totalFocusTimeMinutes)
            .apply()
            
        _sessionStats.value = newStats
    }
}

data class SessionStats(
    val sessionCount: Int,
    val totalFocusTimeMinutes: Long
) {
    val totalFocusTimeHours: Double
        get() = totalFocusTimeMinutes / 60.0
}