package com.erdalgunes.fidan

import android.app.Application
import com.erdalgunes.fidan.garmin.GarminSyncService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FidanApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Start Garmin sync service for watch communication
        GarminSyncService.startService(this)
    }
}