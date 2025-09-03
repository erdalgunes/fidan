package com.erdalgunes.fidan.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class TimerServiceState(
    val timeLeftMillis: Long = 25 * 60 * 1000L, // 25 minutes
    val isRunning: Boolean = false,
    val sessionCompleted: Boolean = false,
    val treeWithering: Boolean = false
)

@Singleton
class TimerService @Inject constructor() {
    
    private val _state = MutableStateFlow(TimerServiceState())
    val state: StateFlow<TimerServiceState> = _state.asStateFlow()
    
    fun startTimer() {
        _state.value = _state.value.copy(isRunning = true)
    }
    
    fun stopTimer() {
        _state.value = _state.value.copy(isRunning = false)
    }
    
    fun getStatusMessage(): String {
        return when {
            _state.value.sessionCompleted -> "Session completed! 🎉"
            _state.value.isRunning -> "Focus time active 🌱"
            _state.value.treeWithering -> "Tree withering... 🥀"
            else -> "Ready to focus"
        }
    }
}