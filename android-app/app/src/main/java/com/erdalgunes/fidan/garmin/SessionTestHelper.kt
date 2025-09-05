package com.erdalgunes.fidan.garmin

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper class for testing session storage functionality.
 * Can be used to simulate watch sessions for development.
 */
object SessionTestHelper {
    
    private const val TAG = "SessionTestHelper"
    
    /**
     * Add a test session to storage for verification.
     */
    fun addTestSession(context: Context) {
        val scope = CoroutineScope(Dispatchers.IO)
        val storage = SessionStorage(context)
        
        scope.launch {
            val testSession = WatchSession(
                timestamp = System.currentTimeMillis() / 1000,
                durationSeconds = 1500, // 25 minutes
                deviceId = "Test-Device-${System.currentTimeMillis()}"
            )
            
            val stored = storage.storeWatchSession(testSession)
            Log.d(TAG, "Test session stored: $stored - ${testSession.durationFormatted()}")
        }
    }
    
    /**
     * Log all stored sessions for debugging.
     */
    fun logAllSessions(context: Context) {
        val scope = CoroutineScope(Dispatchers.IO)
        val storage = SessionStorage(context)
        
        scope.launch {
            val sessions = storage.getAllSessions()
            Log.d(TAG, "Total sessions stored: ${sessions.size}")
            
            sessions.forEach { session ->
                Log.d(TAG, "Session: ${session.durationFormatted()} from ${session.deviceId} at ${session.completedAt}")
            }
        }
    }
    
    /**
     * Clear all test data.
     */
    fun clearAllSessions(context: Context) {
        val scope = CoroutineScope(Dispatchers.IO)
        val storage = SessionStorage(context)
        
        scope.launch {
            storage.clearAllSessions()
            Log.d(TAG, "All sessions cleared")
        }
    }
}