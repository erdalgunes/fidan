package com.fidan.timer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fidan.timer.service.TimerService
import com.fidan.timer.ui.TimerScreen
import com.fidan.timer.ui.theme.FidanTheme
import com.fidan.timer.utils.TimerPreferences
import com.fidan.timer.viewmodel.TimerViewModel
import com.fidan.timer.viewmodel.TimerEvent

class MainActivity : ComponentActivity() {
    private lateinit var timerViewModel: TimerViewModel
    private lateinit var timerPreferences: TimerPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        timerPreferences = TimerPreferences.getInstance(this)
        
        setContent {
            FidanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    timerViewModel = viewModel()
                    val timerEvents by timerViewModel.timerEvents.collectAsState()
                    
                    // Handle timer completion for service notification
                    when (timerEvents) {
                        is TimerEvent.SessionCompleted -> {
                            triggerTimerComplete()
                            timerPreferences.recordCompletedSession()
                            timerPreferences.clearSessionStartTime()
                        }
                        else -> {}
                    }
                    
                    TimerScreen(timerViewModel = timerViewModel)
                }
            }
        }

        // Restore timer if app was killed while running
        restoreTimerIfNeeded()
    }

    private fun restoreTimerIfNeeded() {
        val sessionStartTime = timerPreferences.getSessionStartTime()
        
        if (sessionStartTime > 0) {
            timerViewModel.restoreTimer(sessionStartTime)
        }
    }

    override fun onPause() {
        super.onPause()
        // Store session start time for recovery
        if (timerViewModel.timerState.value.isRunning) {
            timerPreferences.saveSessionStartTime(timerViewModel.getSessionStartTime())
        }
    }

    override fun onResume() {
        super.onResume()
        // Clear stored session if timer is not running
        if (!timerViewModel.timerState.value.isRunning) {
            timerPreferences.clearSessionStartTime()
        }
    }

    private fun triggerTimerComplete() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_TIMER_COMPLETE
        }
        startService(intent)
    }
}