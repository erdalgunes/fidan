package com.erdalgunes.fidan.service

import com.erdalgunes.fidan.domain.PomodoroSession
import com.erdalgunes.fidan.domain.PomodoroSessionManager
import com.erdalgunes.fidan.domain.PomodoroState
import com.erdalgunes.fidan.domain.SessionTimer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PomodoroSessionService constructor(
    private val sessionTimer: SessionTimer
) : PomodoroSessionManager {
    
    private val _sessionState = MutableStateFlow(PomodoroSession())
    override val sessionState: StateFlow<PomodoroSession> = _sessionState.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitoringJob: Job? = null
    
    override fun startSession() {
        val currentSession = _sessionState.value
        
        // If we're starting fresh or resuming from a break
        if (currentSession.currentState == PomodoroState.IDLE || !currentSession.isRunning) {
            val newState = when (currentSession.currentState) {
                PomodoroState.IDLE -> PomodoroState.WORKING
                else -> currentSession.currentState
            }
            
            val newSession = currentSession.copy(
                currentState = newState,
                timeLeftMillis = newState.getCurrentDurationMillis(),
                isRunning = true
            )
            
            _sessionState.value = newSession
            sessionTimer.start()
            startMonitoring()
        }
    }
    
    override fun stopSession() {
        sessionTimer.stop()
        monitoringJob?.cancel()
        
        _sessionState.value = _sessionState.value.copy(isRunning = false)
    }
    
    override fun pauseSession() {
        sessionTimer.pause()
        _sessionState.value = _sessionState.value.copy(isRunning = false)
    }
    
    override fun completeCurrentInterval() {
        val currentSession = _sessionState.value
        val nextState = currentSession.getNextState()
        
        val newCompletedSessions = when (currentSession.currentState) {
            PomodoroState.WORKING -> currentSession.completedWorkSessions + 1
            else -> currentSession.completedWorkSessions
        }
        
        val newSession = PomodoroSession(
            currentState = nextState,
            completedWorkSessions = newCompletedSessions,
            timeLeftMillis = nextState.getCurrentDurationMillis(),
            isRunning = false
        )
        
        _sessionState.value = newSession
        sessionTimer.reset()
        
        // Auto-start the next interval for a smooth flow
        if (nextState != PomodoroState.IDLE) {
            startSession()
        }
    }
    
    override fun resetSession() {
        sessionTimer.reset()
        monitoringJob?.cancel()
        _sessionState.value = PomodoroSession()
    }
    
    override fun skipToNextInterval() {
        completeCurrentInterval()
    }
    
    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive && _sessionState.value.isRunning) {
                delay(1000)
                
                val timeLeft = sessionTimer.getTimeLeftMillis()
                _sessionState.value = _sessionState.value.copy(timeLeftMillis = timeLeft)
                
                if (sessionTimer.isCompleted()) {
                    completeCurrentInterval()
                    break
                }
            }
        }
    }
    
    fun cleanup() {
        monitoringJob?.cancel()
        scope.cancel()
    }
}