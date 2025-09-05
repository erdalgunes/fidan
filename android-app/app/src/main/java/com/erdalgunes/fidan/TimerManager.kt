package com.erdalgunes.fidan

import com.erdalgunes.fidan.common.TimerUiState
import com.erdalgunes.fidan.common.Result
import com.erdalgunes.fidan.repository.TimerRepository
import com.erdalgunes.fidan.repository.TimerProgress
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

/**
 * Refactored TimerManager following SOLID principles.
 * Uses supervisor scope for fault tolerance and proper separation of concerns.
 */
data class TimerState(
    val timeLeftMillis: Long = 25 * 60 * 1000L,
    val isRunning: Boolean = false,
    val sessionCompleted: Boolean = false,
    val isAppInBackground: Boolean = false,
    val graceTimeLeftMillis: Long = 30 * 1000L,
    val treeWithering: Boolean = false,
    val error: String? = null,
    val sessionId: String? = null
)

interface TimerCallback {
    fun onSessionCompleted()
    fun onSessionStopped(wasRunning: Boolean, timeElapsed: Long)
    fun onError(error: String, isRecoverable: Boolean)
}

class TimerManager(
    private val callback: TimerCallback,
    private val timerRepository: TimerRepository,
    parentScope: CoroutineScope? = null
) {
    // Use supervisor scope for fault tolerance - child failures don't cancel siblings
    private val supervisorScope = CoroutineScope(
        (parentScope?.coroutineContext ?: Dispatchers.Default) + SupervisorJob()
    )
    
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()
    
    private val _uiState = MutableStateFlow<TimerUiState>(TimerUiState.Idle)
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()
    
    private var progressJob: Job? = null
    private var currentSessionId: String? = null
    private val sessionDurationMillis = 25 * 60 * 1000L
    
    fun startTimer() {
        if (_state.value.isRunning) return
        
        supervisorScope.launch {
            try {
                // Start session through repository
                val sessionResult = timerRepository.startSession(sessionDurationMillis)
                
                sessionResult.onSuccess { sessionData ->
                    currentSessionId = sessionData.taskName // Using taskName as sessionId temporarily
                    
                    _state.value = _state.value.copy(
                        isRunning = true,
                        sessionCompleted = false,
                        error = null,
                        sessionId = currentSessionId
                    )
                    
                    _uiState.value = TimerUiState.Running(
                        timeLeftMillis = sessionDurationMillis,
                        totalDurationMillis = sessionDurationMillis
                    )
                    
                    // Start progress monitoring
                    startProgressMonitoring(currentSessionId!!)
                }.onError { exception, message ->
                    handleError("Failed to start timer: $message", exception, true)
                }
            } catch (e: Exception) {
                handleError("Unexpected error starting timer", e, true)
            }
        }
    }
    
    fun stopTimer() {
        val currentState = _state.value
        if (!currentState.isRunning || currentSessionId == null) return
        
        supervisorScope.launch {
            try {
                val stopResult = timerRepository.stopSession(currentSessionId!!)
                
                stopResult.onSuccess { sessionData ->
                    progressJob?.cancel()
                    
                    val timeElapsed = sessionData.durationMillis
                    
                    _state.value = currentState.copy(
                        isRunning = false,
                        error = null
                    )
                    
                    _uiState.value = TimerUiState.Completed(
                        totalDurationMillis = sessionDurationMillis,
                        wasInterrupted = true
                    )
                    
                    callback.onSessionStopped(wasRunning = true, timeElapsed = timeElapsed)
                    resetTimer()
                }.onError { exception, message ->
                    handleError("Failed to stop timer: $message", exception, true)
                }
            } catch (e: Exception) {
                handleError("Unexpected error stopping timer", e, true)
            }
        }
    }
    
    fun resetTimer() {
        progressJob?.cancel()
        currentSessionId = null
        _state.value = TimerState()
        _uiState.value = TimerUiState.Idle
    }
    
    fun cleanup() {
        progressJob?.cancel()
        supervisorScope.cancel()
        currentSessionId?.let { sessionId ->
            // Cleanup any active sessions
            supervisorScope.launch {
                timerRepository.stopSession(sessionId)
            }
        }
    }
    
    fun onAppPaused() {
        val currentState = _state.value
        if (currentState.isRunning && currentSessionId != null) {
            supervisorScope.launch {
                try {
                    (timerRepository as? com.erdalgunes.fidan.repository.DefaultTimerRepository)
                        ?.onAppBackground(currentSessionId!!)
                    
                    _state.value = currentState.copy(
                        isAppInBackground = true,
                        treeWithering = true
                    )
                } catch (e: Exception) {
                    handleError("Error handling app pause", e, true)
                }
            }
        }
    }
    
    fun onAppResumed() {
        val currentState = _state.value
        if (currentState.isAppInBackground && currentSessionId != null) {
            supervisorScope.launch {
                try {
                    (timerRepository as? com.erdalgunes.fidan.repository.DefaultTimerRepository)
                        ?.onAppForeground(currentSessionId!!)
                    
                    _state.value = currentState.copy(
                        isAppInBackground = false,
                        treeWithering = false
                    )
                } catch (e: Exception) {
                    handleError("Error handling app resume", e, true)
                }
            }
        }
    }
    
    private fun startProgressMonitoring(sessionId: String) {
        progressJob = supervisorScope.launch {
            try {
                timerRepository.getSessionProgress(sessionId)
                    .catch { exception ->
                        handleError("Progress monitoring failed", exception, true)
                    }
                    .collect { result ->
                        result.onSuccess { progress ->
                            updateStateFromProgress(progress)
                        }.onError { exception, message ->
                            handleError("Progress update failed: $message", exception, true)
                        }
                    }
            } catch (e: Exception) {
                handleError("Failed to start progress monitoring", e, true)
            }
        }
    }
    
    private fun updateStateFromProgress(progress: TimerProgress) {
        val currentState = _state.value
        
        _state.value = currentState.copy(
            timeLeftMillis = progress.timeLeftMillis,
            isRunning = progress.isRunning,
            sessionCompleted = progress.isCompleted,
            isAppInBackground = progress.isBackgroundMode,
            graceTimeLeftMillis = progress.graceTimeLeftMillis,
            treeWithering = progress.isInGracePeriod
        )
        
        // Update UI state
        _uiState.value = when {
            progress.isCompleted -> {
                callback.onSessionCompleted()
                TimerUiState.Completed(progress.totalDurationMillis)
            }
            progress.isInGracePeriod -> {
                TimerUiState.BackgroundWarning(
                    progress.timeLeftMillis,
                    progress.graceTimeLeftMillis
                )
            }
            progress.isRunning -> {
                TimerUiState.Running(
                    progress.timeLeftMillis,
                    progress.totalDurationMillis,
                    progress.progress
                )
            }
            else -> {
                TimerUiState.Paused(
                    progress.timeLeftMillis,
                    progress.totalDurationMillis,
                    progress.progress
                )
            }
        }
    }
    
    private fun handleError(message: String, exception: Throwable, isRecoverable: Boolean) {
        _state.value = _state.value.copy(error = message)
        _uiState.value = TimerUiState.Error(message, isRecoverable)
        callback.onError(message, isRecoverable)
    }
    
    fun getCurrentTimeText(): String {
        val timeLeft = _state.value.timeLeftMillis
        val minutes = (timeLeft / 1000) / 60
        val seconds = (timeLeft / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    fun getStatusMessage(): String {
        val state = _state.value
        return when {
            state.error != null -> "⚠️ ${state.error}"
            state.treeWithering -> "⚠️ Return to app in ${state.graceTimeLeftMillis / 1000}s to keep growing!"
            state.isRunning -> "Focus on your task!"
            state.sessionCompleted -> "Session complete!"
            else -> "Ready to focus?"
        }
    }
}