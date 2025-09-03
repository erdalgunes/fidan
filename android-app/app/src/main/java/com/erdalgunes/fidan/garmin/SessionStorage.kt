package com.erdalgunes.fidan.garmin

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Simple storage for watch sessions using SharedPreferences.
 * MVP implementation - can be replaced with Room database later.
 */
class SessionStorage(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "SessionStorage"
        private const val PREFS_NAME = "garmin_sessions"
        private const val KEY_SESSIONS = "watch_sessions"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val MAX_STORED_SESSIONS = 100
    }
    
    /**
     * Store a completed watch session.
     * Prevents duplicate sessions based on timestamp and deviceId.
     */
    suspend fun storeWatchSession(watchSession: WatchSession): Boolean = withContext(Dispatchers.IO) {
        try {
            val sessions = getStoredSessions().toMutableList()
            
            // Check for duplicate session (same timestamp and deviceId)
            val isDuplicate = sessions.any { existing ->
                existing.timestamp == watchSession.timestamp && 
                existing.deviceId == watchSession.deviceId
            }
            
            if (isDuplicate) {
                Log.w(TAG, "Duplicate session ignored: ${watchSession.timestamp}")
                return@withContext false
            }
            
            // Add new session
            sessions.add(watchSession)
            
            // Sort by timestamp (newest first) and limit to MAX_STORED_SESSIONS
            val sortedSessions = sessions
                .sortedByDescending { it.timestamp }
                .take(MAX_STORED_SESSIONS)
            
            // Store back to preferences
            saveSessionsToPrefs(sortedSessions)
            updateLastSyncTimestamp()
            
            Log.i(TAG, "Stored watch session: ${watchSession.durationFormatted()} from ${watchSession.deviceId}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error storing watch session", e)
            false
        }
    }
    
    /**
     * Get all stored watch sessions, sorted by timestamp (newest first).
     */
    suspend fun getAllSessions(): List<WatchSession> = withContext(Dispatchers.IO) {
        getStoredSessions()
    }
    
    /**
     * Get sessions from the last N days.
     */
    suspend fun getRecentSessions(days: Int = 7): List<WatchSession> = withContext(Dispatchers.IO) {
        val cutoffTimestamp = Instant.now().minusSeconds(days * 24 * 60 * 60L).epochSecond
        getStoredSessions().filter { it.timestamp >= cutoffTimestamp }
    }
    
    /**
     * Get total count of stored sessions.
     */
    suspend fun getSessionCount(): Int = withContext(Dispatchers.IO) {
        getStoredSessions().size
    }
    
    /**
     * Clear all stored sessions (for testing).
     */
    suspend fun clearAllSessions() = withContext(Dispatchers.IO) {
        prefs.edit()
            .remove(KEY_SESSIONS)
            .remove(KEY_LAST_SYNC)
            .apply()
        Log.i(TAG, "All sessions cleared")
    }
    
    /**
     * Get timestamp of last successful sync.
     */
    fun getLastSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_SYNC, 0L)
    }
    
    private fun getStoredSessions(): List<WatchSession> {
        try {
            val jsonString = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
            val jsonArray = JSONArray(jsonString)
            val sessions = mutableListOf<WatchSession>()
            
            for (i in 0 until jsonArray.length()) {
                val sessionJson = jsonArray.getJSONObject(i)
                val session = WatchSession(
                    timestamp = sessionJson.getLong("timestamp"),
                    durationSeconds = sessionJson.getInt("durationSeconds"),
                    deviceId = sessionJson.getString("deviceId")
                )
                sessions.add(session)
            }
            
            return sessions.sortedByDescending { it.timestamp }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error reading stored sessions", e)
            return emptyList()
        }
    }
    
    private fun saveSessionsToPrefs(sessions: List<WatchSession>) {
        try {
            val jsonArray = JSONArray()
            
            for (session in sessions) {
                val sessionJson = JSONObject().apply {
                    put("timestamp", session.timestamp)
                    put("durationSeconds", session.durationSeconds)
                    put("deviceId", session.deviceId)
                }
                jsonArray.put(sessionJson)
            }
            
            prefs.edit()
                .putString(KEY_SESSIONS, jsonArray.toString())
                .apply()
                
        } catch (e: Exception) {
            Log.e(TAG, "Error saving sessions to preferences", e)
        }
    }
    
    private fun updateLastSyncTimestamp() {
        prefs.edit()
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }
}