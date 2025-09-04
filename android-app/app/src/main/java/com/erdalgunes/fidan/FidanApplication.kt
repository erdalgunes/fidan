package com.erdalgunes.fidan

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.erdalgunes.fidan.garmin.GarminSyncService
import com.erdalgunes.fidan.service.AppLifecycleObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FidanApplication : Application() {
    
    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize app lifecycle observer for idle detection
        // This is injected and will automatically start observing
        appLifecycleObserver.toString() // Force initialization
        
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