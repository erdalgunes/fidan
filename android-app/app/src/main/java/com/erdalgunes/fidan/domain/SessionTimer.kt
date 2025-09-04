package com.erdalgunes.fidan.domain

import kotlinx.coroutines.flow.StateFlow

interface SessionTimer {
    fun start()
    fun stop()
    fun pause()
    fun reset()
    fun getTimeLeftMillis(): Long
    fun isRunning(): Boolean
    fun isCompleted(): Boolean
}

interface PomodoroSessionManager {
    val sessionState: StateFlow<PomodoroSession>
    
    fun startSession()
    fun stopSession()
    fun pauseSession()
    fun completeCurrentInterval()
    fun resetSession()
    fun skipToNextInterval()
}