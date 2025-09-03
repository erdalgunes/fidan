package com.erdalgunes.fidan.garmin

import com.erdalgunes.fidan.R

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

/**
 * Simplified Garmin sync service for MVP implementation.
 * Handles basic session storage and notifications.
 * Connect IQ integration will be added in phase 2.
 */
class GarminSyncService : Service() {
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private lateinit var sessionStorage: SessionStorage
    private var testJob: Job? = null
    
    companion object {
        private const val TAG = "GarminSyncService"
        private const val NOTIFICATION_ID = 1001
        private const val SYNC_NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "garmin_sync"
        private const val SYNC_CHANNEL_ID = "garmin_sync_notifications"
        
        fun startService(context: Context) {
            val intent = Intent(context, GarminSyncService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, GarminSyncService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "GarminSyncService created")
        
        try {
            sessionStorage = SessionStorage(this)
            createNotificationChannel()
            
            // TODO: Initialize Connect IQ SDK in future phase
            // For MVP, we'll implement a test session simulator
            simulateTestSession()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GarminSyncService", e)
            stopSelf()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        testJob?.cancel()
        serviceJob.cancel()
        super.onDestroy()
        Log.d(TAG, "GarminSyncService destroyed")
    }
    
    /**
     * Simulate a test session for MVP demonstration.
     * This will be replaced with real Garmin Connect IQ integration.
     */
    private fun simulateTestSession() {
        testJob = serviceScope.launch {
            // Wait 10 seconds then simulate receiving a session from watch
            delay(10000)
            
            val testSession = WatchSession(
                timestamp = System.currentTimeMillis() / 1000,
                durationSeconds = 1500, // 25 minutes
                deviceId = "Test-Garmin-Watch"
            )
            
            Log.i(TAG, "Simulating test session from watch")
            handleSessionComplete(testSession)
        }
    }
    
    /**
     * Handle a completed session from Garmin watch.
     * This is the core functionality for MVP.
     */
    suspend fun handleSessionComplete(watchSession: WatchSession) {
        try {
            if (watchSession.isValidFocusSession()) {
                Log.i(TAG, "Valid session completed: ${watchSession.durationFormatted()}")
                
                // Store session in local storage
                val stored = sessionStorage.storeWatchSession(watchSession)
                if (stored) {
                    Log.i(TAG, "Session stored successfully")
                    showSyncNotification(watchSession)
                } else {
                    Log.w(TAG, "Session not stored (possibly duplicate)")
                }
                
                Log.i(TAG, "Session sync completed for device: ${watchSession.deviceId}")
            } else {
                Log.w(TAG, "Invalid session duration: ${watchSession.durationFormatted()}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling session complete", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // Service notification channel (low importance, no sound)
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Garmin Sync Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background sync with Garmin watch"
                setShowBadge(false)
                setSound(null, null)
            }
            
            // Sync notification channel (default importance, with sound)
            val syncChannel = NotificationChannel(
                SYNC_CHANNEL_ID,
                "Watch Session Sync",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when watch sessions are synced"
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(syncChannel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fidan - Garmin Sync")
            .setContentText("Ready to sync watch sessions...")
            .setSmallIcon(R.drawable.ic_fidan_notification)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
    
    private fun showSyncNotification(watchSession: WatchSession) {
        try {
            // Check for notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    this, 
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                
                if (!hasPermission) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping sync notification")
                    return
                }
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            val notification = NotificationCompat.Builder(this, SYNC_CHANNEL_ID)
                .setContentTitle("Watch Session Synced")
                .setContentText("${watchSession.durationFormatted()} focus session from ${watchSession.deviceId}")
                .setSmallIcon(R.drawable.ic_fidan_notification)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            
            notificationManager.notify(SYNC_NOTIFICATION_ID, notification)
            Log.d(TAG, "Sync notification shown for session: ${watchSession.durationFormatted()}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing sync notification", e)
        }
    }
}