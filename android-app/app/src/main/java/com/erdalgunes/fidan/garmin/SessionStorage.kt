package com.erdalgunes.fidan.garmin

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
// Note: Using System.currentTimeMillis() instead of Java Time API for Android compatibility

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
        val cutoffTimestamp = (System.currentTimeMillis() / 1000) - (days * 24 * 60 * 60L)
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
            
            if (jsonString.isBlank()) {
                return emptyList()
            }
            
            val jsonArray = JSONArray(jsonString)
            val sessions = mutableListOf<WatchSession>()
            
            for (i in 0 until jsonArray.length()) {
                try {
                    val sessionJson = jsonArray.getJSONObject(i)
                    
                    // Validate required fields exist
                    if (!sessionJson.has("timestamp") || 
                        !sessionJson.has("durationSeconds") || 
                        !sessionJson.has("deviceId")) {
                        Log.w(TAG, "Skipping session with missing required fields at index $i")
                        continue
                    }
                    
                    val session = WatchSession(
                        timestamp = sessionJson.getLong("timestamp"),
                        durationSeconds = sessionJson.getInt("durationSeconds"),
                        deviceId = sessionJson.getString("deviceId")
                    )
                    sessions.add(session)
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping corrupted session at index $i", e)
                    continue
                }
            }
            
            return sessions.sortedByDescending { it.timestamp }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error reading stored sessions, clearing corrupted data", e)
            // Clear corrupted data
            prefs.edit().remove(KEY_SESSIONS).apply()
            return emptyList()
        }
    }
    
    private fun saveSessionsToPrefs(sessions: List<WatchSession>) {
        try {
            val jsonArray = JSONArray()
            
            for (session in sessions) {
                try {
                    val sessionJson = JSONObject().apply {
                        put("timestamp", session.timestamp)
                        put("durationSeconds", session.durationSeconds)
                        put("deviceId", session.deviceId)
                    }
                    jsonArray.put(sessionJson)
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping session due to serialization error: ${session.deviceId}", e)
                    continue
                }
            }
            
            val jsonString = jsonArray.toString()
            if (jsonString.length > 1000000) { // 1MB limit
                Log.w(TAG, "Session data too large, truncating to latest sessions")
                val truncatedSessions = sessions.take(MAX_STORED_SESSIONS / 2)
                return saveSessionsToPrefs(truncatedSessions)
            }
            
            prefs.edit()
                .putString(KEY_SESSIONS, jsonString)
                .apply()
                
        } catch (e: Exception) {
            Log.e(TAG, "Error saving sessions to preferences", e)
            throw e // Re-throw so caller knows save failed
        }
    }
    
    private fun updateLastSyncTimestamp() {
        prefs.edit()
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }
}