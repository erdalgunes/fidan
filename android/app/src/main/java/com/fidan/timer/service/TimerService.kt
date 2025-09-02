package com.fidan.timer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fidan.timer.MainActivity
import com.fidan.timer.R

class TimerService : Service() {
    companion object {
        const val CHANNEL_ID = "TimerServiceChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_TIMER_COMPLETE = "com.fidan.timer.TIMER_COMPLETE"
        private const val TAG = "TimerService"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TIMER_COMPLETE -> {
                showTimerCompleteNotification()
                playCompletionSound()
                vibrateDevice()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_description)
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun showTimerCompleteNotification() {
        try {
            val notificationIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.timer_complete))
                .setContentText(getString(R.string.tree_grown))
                .setSmallIcon(R.drawable.ic_tree)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(
                    R.drawable.ic_tree,
                    "Start New Session",
                    pendingIntent
                )
                .build()

            if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                startForeground(NOTIFICATION_ID, notification)
                
                // Auto-dismiss after 10 seconds
                android.os.Handler(mainLooper).postDelayed({
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }, 10000)
            } else {
                // Fallback if notifications are disabled
                stopSelf()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to show notification", e)
            stopSelf()
        }
    }

    private fun playCompletionSound() {
        try {
            // Use system notification sound as fallback if custom sound not available
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)
            
            // Try to play custom sound if available
            mediaPlayer = MediaPlayer.create(this, R.raw.completion_chime)
            mediaPlayer?.let { player ->
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                player.setOnCompletionListener { mp ->
                    mp.release()
                    mediaPlayer = null
                }
                player.start()
            }
        } catch (e: Exception) {
            // Gracefully handle missing sound resources
            e.printStackTrace()
        }
    }

    private fun vibrateDevice() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (vibrator.hasVibrator()) {
                val pattern = longArrayOf(0, 300, 150, 300, 150, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(pattern, -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to vibrate device", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}