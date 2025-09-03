package com.erdalgunes.fidan.garmin

import java.time.Instant

/**
 * Represents a focus session completed on the Garmin watch.
 * This will be stored in the Android app's database.
 */
data class WatchSession(
    val timestamp: Long, // Unix timestamp when session completed
    val durationSeconds: Int, // Session duration (typically 1500 for 25 minutes)
    val deviceId: String, // Unique identifier for the watch
    val completedAt: Instant = Instant.ofEpochSecond(timestamp),
    val source: String = "watch" // Identifier showing this came from watch
) {
    
    /**
     * Check if this is a valid 25-minute focus session.
     */
    fun isValidFocusSession(): Boolean {
        return durationSeconds >= 1400 && durationSeconds <= 1600 // 23-27 minutes tolerance
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