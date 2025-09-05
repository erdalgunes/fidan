package com.erdalgunes.fidan

import android.app.Application
import com.erdalgunes.fidan.garmin.GarminSyncService
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Fidan app
 * @HiltAndroidApp triggers Hilt's code generation and creates application-level dependency container
 * Following SOLID principles - particularly Dependency Inversion Principle
 */
@HiltAndroidApp
class FidanApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Start Garmin sync service for watch communication
        GarminSyncService.startService(this)
    }
}