package com.erdalgunes.fidan.garmin

import java.util.Date

/**
 * Represents a focus session completed on the Garmin watch.
 * This will be stored in the Android app's database.
 */
data class WatchSession(
    val timestamp: Long, // Unix timestamp when session completed
    val durationSeconds: Int, // Session duration (typically 1500 for 25 minutes)
    val deviceId: String, // Unique identifier for the watch
    val completedAt: Date = Date(timestamp * 1000), // Convert seconds to milliseconds
    val source: String = "watch" // Identifier showing this came from watch
) {
    
    /**
     * Check if this is a valid focus session.
     * Allows for 20-30 minute sessions with generous tolerance.
     */
    fun isValidFocusSession(): Boolean {
        return durationSeconds >= 1200 && durationSeconds <= 1800 // 20-30 minutes tolerance
    }
    
    /**
     * Convert duration to human-readable format.
     */
    fun durationFormatted(): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return "${minutes}:${seconds.toString().padStart(2, '0')}"
    }
}