package com.erdalgunes.fidan.garmin

/**
 * Represents different message types from Garmin watch.
 * Based on DataSync.mc constants from the watch app.
 */
sealed class GarminMessage {
    
    /**
     * Session completed on watch - 25 minute focus session finished.
     * This is the primary message type we handle for MVP.
     */
    data class SessionComplete(
        val timestamp: Long,
        val duration: Int, // Duration in seconds
        val deviceId: String
    ) : GarminMessage()
    
    /**
     * Session started on watch (future enhancement).
     */
    data class SessionStart(
        val timestamp: Long,
        val deviceId: String
    ) : GarminMessage()
    
    /**
     * Session stopped on watch (future enhancement).
     */
    data class SessionStop(
        val timestamp: Long,
        val duration: Int,
        val deviceId: String
    ) : GarminMessage()
    
    companion object {
        const val MSG_TYPE_SESSION_START = 1
        const val MSG_TYPE_SESSION_STOP = 2
        const val MSG_TYPE_SESSION_COMPLETE = 3
        const val MSG_TYPE_SYNC_REQUEST = 4
        const val MSG_TYPE_SETTINGS_UPDATE = 5
    }
}