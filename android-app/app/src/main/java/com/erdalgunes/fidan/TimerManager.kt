package com.erdalgunes.fidan

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TimerState(
    val timeLeftMillis: Long = 25 * 60 * 1000L,
    val isRunning: Boolean = false,
    val sessionCompleted: Boolean = false
)

interface TimerCallback {
    fun onSessionCompleted()
    fun onSessionStopped(wasRunning: Boolean, timeElapsed: Long)
}

class TimerManager(
    private val callback: TimerCallback,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()
    
    private var timerJob: Job? = null
    private val sessionDurationMillis = 25 * 60 * 1000L
    
    fun startTimer() {
        if (_state.value.isRunning) return
        
        _state.value = _state.value.copy(isRunning = true, sessionCompleted = false)
        
        timerJob = coroutineScope.launch {
            while (isActive && _state.value.isRunning) {
                delay(1000)
                val currentState = _state.value
                if (currentState.timeLeftMillis > 0) {
                    val newTimeLeft = currentState.timeLeftMillis - 1000
                    if (newTimeLeft <= 0) {
                        _state.value = currentState.copy(
                            timeLeftMillis = 0,
                            isRunning = false,
                            sessionCompleted = true
                        )
                        callback.onSessionCompleted()
                        resetTimer()
                        break
                    } else {
                        _state.value = currentState.copy(timeLeftMillis = newTimeLeft)
                    }
                } else {
                    break
                }
            }
        }
    }
    
    fun stopTimer() {
        val currentState = _state.value
        if (!currentState.isRunning) return
        
        timerJob?.cancel()
        val timeElapsed = sessionDurationMillis - currentState.timeLeftMillis
        
        _state.value = currentState.copy(isRunning = false)
        callback.onSessionStopped(wasRunning = true, timeElapsed = timeElapsed)
        resetTimer()
    }
    
    fun resetTimer() {
        timerJob?.cancel()
        _state.value = TimerState()
    }
    
    fun cleanup() {
        timerJob?.cancel()
        coroutineScope.cancel()
    }
    
    fun getCurrentTimeText(): String {
        val timeLeft = _state.value.timeLeftMillis
        val minutes = (timeLeft / 1000) / 60
        val seconds = (timeLeft / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    fun getStatusMessage(): String {
        return when {
            _state.value.isRunning -> "Focus on your task!"
            _state.value.sessionCompleted -> "Session complete!"
            else -> "Ready to focus?"
        }
    }
}