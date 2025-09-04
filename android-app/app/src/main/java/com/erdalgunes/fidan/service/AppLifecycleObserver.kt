package com.erdalgunes.fidan.service

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppLifecycleState(
    val isInForeground: Boolean = true,
    val backgroundStartTime: Long = 0L,
    val totalBackgroundTime: Long = 0L
)

@Singleton
class AppLifecycleObserver @Inject constructor(
    private val timerService: TimerService
) : DefaultLifecycleObserver {
    
    companion object {
        private const val TAG = "AppLifecycleObserver"
        private const val BACKGROUND_PENALTY_THRESHOLD_MS = 5000L // 5 seconds
    }
    
    private val _state = MutableStateFlow(AppLifecycleState())
    val state: StateFlow<AppLifecycleState> = _state.asStateFlow()
    
    private var backgroundStartTime = 0L
    
    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    override fun onStart(owner: LifecycleOwner) {
        // App comes to foreground
        Log.d(TAG, "App came to foreground")
        
        val backgroundDuration = if (backgroundStartTime > 0) {
            System.currentTimeMillis() - backgroundStartTime
        } else {
            0L
        }
        
        val currentState = _state.value
        _state.value = currentState.copy(
            isInForeground = true,
            backgroundStartTime = 0L,
            totalBackgroundTime = currentState.totalBackgroundTime + backgroundDuration
        )
        
        // Check if user spent too much time away during focus session
        if (backgroundDuration > BACKGROUND_PENALTY_THRESHOLD_MS && timerService.state.value.isRunning) {
            Log.w(TAG, "User was away for ${backgroundDuration}ms during focus session")
            handleFocusViolation(backgroundDuration)
        }
    }
    
    override fun onStop(owner: LifecycleOwner) {
        // App goes to background
        Log.d(TAG, "App went to background")
        
        backgroundStartTime = System.currentTimeMillis()
        _state.value = _state.value.copy(
            isInForeground = false,
            backgroundStartTime = backgroundStartTime
        )
        
        // If timer is running, this is a potential focus violation
        if (timerService.state.value.isRunning) {
            Log.i(TAG, "Focus session interrupted - user left app")
        }
    }
    
    private fun handleFocusViolation(backgroundDuration: Long) {
        // For now, just mark the tree as withering
        // In a more advanced implementation, we could have different penalty levels
        val violationDurationSeconds = backgroundDuration / 1000
        Log.w(TAG, "Focus violation: ${violationDurationSeconds}s away from app")
        
        // Could implement graduated penalties:
        // - 5-30 seconds: Warning
        // - 30-60 seconds: Tree starts withering
        // - 60+ seconds: Session marked as failed
        
        if (violationDurationSeconds > 30) {
            // Severe violation - mark session as failed
            timerService.stopTimer() // This will trigger treeWithering state
        }
    }
    
    fun resetBackgroundTime() {
        _state.value = _state.value.copy(totalBackgroundTime = 0L)
    }
    
    fun getTotalBackgroundTime(): Long {
        val currentState = _state.value
        return if (!currentState.isInForeground && currentState.backgroundStartTime > 0) {
            currentState.totalBackgroundTime + (System.currentTimeMillis() - currentState.backgroundStartTime)
        } else {
            currentState.totalBackgroundTime
        }
    }
}