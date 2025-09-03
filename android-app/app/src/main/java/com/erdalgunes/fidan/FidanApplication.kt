package com.erdalgunes.fidan

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.erdalgunes.fidan.garmin.GarminSyncService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FidanApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Only start Garmin sync service if we have notification permission
        // to avoid crashes on Android 13+ (API 33+)
        if (canStartForegroundService()) {
            GarminSyncService.startService(this)
        }
        // TODO: Request notification permission in main activity if not granted
    }
    
    private fun canStartForegroundService(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, 
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Below Android 13, no runtime notification permission needed
            true
        }
    }
}