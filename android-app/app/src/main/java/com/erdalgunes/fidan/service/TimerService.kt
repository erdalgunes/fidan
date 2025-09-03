package com.erdalgunes.fidan.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class TimerServiceState(
    val timeLeftMillis: Long = 25 * 60 * 1000L, // 25 minutes
    val isRunning: Boolean = false,
    val sessionCompleted: Boolean = false,
    val treeWithering: Boolean = false,
    val isPaused: Boolean = false
)

@Singleton
class TimerService @Inject constructor() {
    
    private val _state = MutableStateFlow(TimerServiceState())
    val state: StateFlow<TimerServiceState> = _state.asStateFlow()
    
    private var timerJob: Job? = null
    private var pausedTime: Long = 0L
    private var sessionStartTime: Long = 0L
    private val sessionDurationMs = 25 * 60 * 1000L // 25 minutes
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    fun startTimer() {
        if (_state.value.sessionCompleted) {
            resetTimer()
        }
        
        if (_state.value.isPaused) {
            resumeTimer()
        } else {
            startNewTimer()
        }
    }
    
    private fun startNewTimer() {
        sessionStartTime = System.currentTimeMillis()
        _state.value = _state.value.copy(
            isRunning = true,
            isPaused = false,
            sessionCompleted = false,
            treeWithering = false,
            timeLeftMillis = sessionDurationMs
        )
        startCountdown()
    }
    
    private fun resumeTimer() {
        sessionStartTime = System.currentTimeMillis() - (sessionDurationMs - _state.value.timeLeftMillis)
        _state.value = _state.value.copy(isRunning = true, isPaused = false)
        startCountdown()
    }
    
    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_state.value.isRunning && _state.value.timeLeftMillis > 0) {
                delay(1000)
                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - sessionStartTime
                val remaining = (sessionDurationMs - elapsed).coerceAtLeast(0)
                
                _state.value = _state.value.copy(timeLeftMillis = remaining)
                
                if (remaining == 0L) {
                    completeSession()
                    break
                }
            }
        }
    }
    
    fun stopTimer() {
        timerJob?.cancel()
        val wasRunning = _state.value.isRunning
        _state.value = _state.value.copy(
            isRunning = false,
            isPaused = true
        )
        
        // If stopped early, mark as tree withering
        if (wasRunning && _state.value.timeLeftMillis > 0) {
            _state.value = _state.value.copy(treeWithering = true)
        }
    }
    
    fun pauseTimer() {
        timerJob?.cancel()
        pausedTime = _state.value.timeLeftMillis
        _state.value = _state.value.copy(isRunning = false, isPaused = true)
    }
    
    private fun completeSession() {
        timerJob?.cancel()
        _state.value = _state.value.copy(
            isRunning = false,
            sessionCompleted = true,
            timeLeftMillis = 0L,
            treeWithering = false,
            isPaused = false
        )
    }
    
    fun resetTimer() {
        timerJob?.cancel()
        _state.value = TimerServiceState()
        pausedTime = 0L
        sessionStartTime = 0L
    }
    
    fun getStatusMessage(): String {
        return when {
            _state.value.sessionCompleted -> "Session completed! 🎉"
            _state.value.treeWithering -> "Tree withering... 🥀"
            _state.value.isRunning -> "Focus time active 🌱"
            _state.value.isPaused -> "Timer paused ⏸️"
            else -> "Ready to focus"
        }
    }
    
    fun getTimeElapsed(): Long {
        return sessionDurationMs - _state.value.timeLeftMillis
    }
    
    fun cleanup() {
        timerJob?.cancel()
        scope.cancel()
    }
}