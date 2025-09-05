package com.erdalgunes.fidan.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.erdalgunes.fidan.MainActivity
import com.erdalgunes.fidan.R
import com.erdalgunes.fidan.TimerManager
import com.erdalgunes.fidan.repository.TimerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * Foreground service for timer functionality following SOLID principles.
 * Ensures timer continues running when app is in background.
 * DRY: Reuses TimerManager logic instead of duplicating timer code.
 * KISS: Simple service that delegates to TimerManager.
 */
@AndroidEntryPoint
class TimerService : Service() {
    
    @Inject
    lateinit var timerRepository: TimerRepository
    
    private var timerManager: TimerManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        const val CHANNEL_ID = "FidanTimerChannel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "com.erdalgunes.fidan.START_TIMER"
        const val ACTION_STOP = "com.erdalgunes.fidan.STOP_TIMER"
        const val ACTION_PAUSE = "com.erdalgunes.fidan.PAUSE_TIMER"
        const val ACTION_RESUME = "com.erdalgunes.fidan.RESUME_TIMER"
        
        fun startTimer(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }
        
        fun stopTimer(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Initialize timer manager with proper callback
        timerManager = TimerManager(
            callback = object : com.erdalgunes.fidan.TimerCallback {
                override fun onSessionCompleted() {
                    updateNotification("🌳 Session completed! Tree grown successfully!")
                    stopForeground(false)
                }
                
                override fun onSessionStopped(wasRunning: Boolean, timeElapsed: Long) {
                    if (wasRunning) {
                        updateNotification("Session stopped early")
                    }
                    stopSelf()
                }
                
                override fun onError(error: String, isRecoverable: Boolean) {
                    updateNotification("Error: $error")
                    if (!isRecoverable) {
                        stopSelf()
                    }
                }
            },
            timerRepository = timerRepository,
            parentScope = serviceScope
        )
        
        // Observe timer state changes
        serviceScope.launch {
            timerManager?.state?.collectLatest { state ->
                if (state.isRunning) {
                    val minutes = (state.timeLeftMillis / 1000) / 60
                    val seconds = (state.timeLeftMillis / 1000) % 60
                    updateNotification("Focus time: ${String.format("%02d:%02d", minutes, seconds)}")
                }
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification("Starting focus session..."))
                timerManager?.startTimer()
            }
            ACTION_STOP -> {
                timerManager?.stopTimer()
                stopSelf()
            }
            ACTION_PAUSE -> {
                // TODO: Implement pause functionality
            }
            ACTION_RESUME -> {
                // TODO: Implement resume functionality
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        timerManager?.cleanup()
        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Fidan Timer"
            val descriptionText = "Shows focus timer progress"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🌱 Fidan Focus")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}