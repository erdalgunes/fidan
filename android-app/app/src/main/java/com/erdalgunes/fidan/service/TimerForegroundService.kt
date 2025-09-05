package com.erdalgunes.fidan.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.erdalgunes.fidan.R
import com.erdalgunes.fidan.CircuitMainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : Service() {
    
    companion object {
        private const val TAG = "TimerForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "timer_channel"
        private const val CHANNEL_NAME = "Focus Timer"
        
        const val ACTION_START_TIMER = "start_timer"
        const val ACTION_STOP_TIMER = "stop_timer"
        const val ACTION_PAUSE_TIMER = "pause_timer"
        
        fun startService(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START_TIMER
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_STOP_TIMER
            }
            context.startService(intent)
        }
    }
    
    @Inject
    lateinit var timerService: TimerService
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isServiceStarted = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TimerForegroundService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                startForegroundTimer()
            }
            ACTION_STOP_TIMER -> {
                stopForegroundTimer()
            }
            ACTION_PAUSE_TIMER -> {
                timerService.pauseTimer()
            }
        }
        
        return START_NOT_STICKY // Don't restart if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Log.d(TAG, "TimerForegroundService destroyed")
        serviceScope.cancel()
        super.onDestroy()
    }
    
    private fun startForegroundTimer() {
        if (isServiceStarted) return
        
        Log.d(TAG, "Starting foreground timer service")
        isServiceStarted = true
        
        // Start the timer business logic
        timerService.startTimer()
        
        // Start foreground service with initial notification
        val notification = createNotification("25:00", "Focus session starting...")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        )
        
        // Observe timer state and update notification
        serviceScope.launch {
            timerService.state.collectLatest { timerState ->
                val minutes = (timerState.timeLeftMillis / 1000) / 60
                val seconds = (timerState.timeLeftMillis / 1000) % 60
                val timeText = "%02d:%02d".format(minutes, seconds)
                
                val statusText = when {
                    timerState.sessionCompleted -> "Session completed!"
                    timerState.treeWithering -> "Focus lost - session interrupted"
                    timerState.isRunning -> "Focus session active"
                    timerState.isPaused -> "Timer paused"
                    else -> "Ready to focus"
                }
                
                val updatedNotification = createNotification(timeText, statusText)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, updatedNotification)
                
                // Stop service when session completes or fails
                if (timerState.sessionCompleted || timerState.treeWithering) {
                    delay(3000) // Show completion message for 3 seconds
                    stopForegroundTimer()
                }
            }
        }
    }
    
    private fun stopForegroundTimer() {
        if (!isServiceStarted) return
        
        Log.d(TAG, "Stopping foreground timer service")
        isServiceStarted = false
        
        timerService.stopTimer()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // Low importance for timer notifications
            ).apply {
                description = "Focus timer notifications"
                setShowBadge(false)
                setSound(null, null) // Silent notifications
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(timeText: String, statusText: String): Notification {
        val openAppIntent = Intent(this, CircuitMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val stopIntent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_STOP_TIMER
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Timer - $timeText")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_timer) // Make sure this icon exists
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_timer, "Stop", stopPendingIntent)
            .setOngoing(true) // Can't be dismissed while running
            .setSilent(true) // No sound
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}