package com.fidan.timer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fidan.timer.service.TimerService
import com.fidan.timer.ui.TimerScreen
import com.fidan.timer.ui.theme.FidanTheme
import com.fidan.timer.viewmodel.TimerViewModel

class MainActivity : ComponentActivity() {
    private lateinit var timerViewModel: TimerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            FidanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    timerViewModel = viewModel()
                    TimerScreen(timerViewModel = timerViewModel)
                }
            }
        }

        // Restore timer if app was killed while running
        restoreTimerIfNeeded()
    }

    private fun restoreTimerIfNeeded() {
        val prefs = getSharedPreferences("timer_prefs", MODE_PRIVATE)
        val sessionStartTime = prefs.getLong("session_start_time", 0)
        
        if (sessionStartTime > 0) {
            timerViewModel.restoreTimer(sessionStartTime)
        }
    }

    override fun onPause() {
        super.onPause()
        // Store session start time for recovery
        if (timerViewModel.timerState.value.isRunning) {
            val prefs = getSharedPreferences("timer_prefs", MODE_PRIVATE)
            prefs.edit()
                .putLong("session_start_time", timerViewModel.getSessionStartTime())
                .apply()
        }
    }

    override fun onResume() {
        super.onResume()
        // Clear stored session if timer is not running
        if (!timerViewModel.timerState.value.isRunning) {
            val prefs = getSharedPreferences("timer_prefs", MODE_PRIVATE)
            prefs.edit().remove("session_start_time").apply()
        }
    }

    fun triggerTimerComplete() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_TIMER_COMPLETE
        }
        startService(intent)
    }
}