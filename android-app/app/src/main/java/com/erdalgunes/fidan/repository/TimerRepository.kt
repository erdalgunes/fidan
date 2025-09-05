package com.erdalgunes.fidan.repository

import com.erdalgunes.fidan.common.Result
import com.erdalgunes.fidan.common.safeCall
import com.erdalgunes.fidan.data.SessionData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import java.util.Date

/**
 * Repository interface for timer operations.
 * Follows Interface Segregation Principle from SOLID.
 */
interface TimerRepository {
    suspend fun startSession(durationMillis: Long): Result<SessionData>
    suspend fun pauseSession(sessionId: String): Result<SessionData>
    suspend fun resumeSession(sessionId: String): Result<SessionData>
    suspend fun stopSession(sessionId: String): Result<SessionData>
    suspend fun completeSession(sessionId: String): Result<SessionData>
    fun getSessionProgress(sessionId: String): Flow<Result<TimerProgress>>
    suspend fun validateSession(sessionId: String): Result<Boolean>
}

/**
 * Data class representing timer progress.
 * Follows KISS principle - simple data holder.
 */
data class TimerProgress(
    val sessionId: String,
    val timeLeftMillis: Long,
    val totalDurationMillis: Long,
    val isRunning: Boolean,
    val isCompleted: Boolean,
    val isBackgroundMode: Boolean = false,
    val graceTimeLeftMillis: Long = 0L
) {
    val progress: Float
        get() = if (totalDurationMillis > 0) {
            1f - (timeLeftMillis.toFloat() / totalDurationMillis)
        } else 0f
    
    val isInGracePeriod: Boolean
        get() = isBackgroundMode && graceTimeLeftMillis > 0
}

/**
 * Default implementation of TimerRepository.
 * Handles timer logic with proper error handling and recovery mechanisms.
 */
class DefaultTimerRepository : TimerRepository {
    
    private val activeSessions = mutableMapOf<String, ActiveSession>()
    private val gracePeriodMillis = 30_000L // 30 seconds grace period
    
    /**
     * Internal data class to track active sessions.
     */
    private data class ActiveSession(
        val sessionData: SessionData,
        var timeLeftMillis: Long,
        var isRunning: Boolean = false,
        var isPaused: Boolean = false,
        var isBackgroundMode: Boolean = false,
        var graceTimeLeftMillis: Long = 0L,
        val startTime: Long = System.currentTimeMillis()
    )
    
    override suspend fun startSession(durationMillis: Long): Result<SessionData> = safeCall {
        require(durationMillis > 0) { "Duration must be positive" }
        
        val sessionId = generateSessionId()
        val sessionData = SessionData(
            taskName = "Focus Session",
            durationMillis = durationMillis,
            completedDate = Date(),
            wasCompleted = false
        )
        
        val activeSession = ActiveSession(
            sessionData = sessionData,
            timeLeftMillis = durationMillis,
            isRunning = true
        )
        
        activeSessions[sessionId] = activeSession
        sessionData.copy(taskName = sessionId) // Use taskName to store sessionId temporarily
    }
    
    override suspend fun pauseSession(sessionId: String): Result<SessionData> = safeCall {
        val session = activeSessions[sessionId] 
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        require(session.isRunning) { "Session is not running" }
        
        session.isRunning = false
        session.isPaused = true
        session.sessionData
    }
    
    override suspend fun resumeSession(sessionId: String): Result<SessionData> = safeCall {
        val session = activeSessions[sessionId]
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        require(session.isPaused) { "Session is not paused" }
        
        session.isRunning = true
        session.isPaused = false
        session.isBackgroundMode = false
        session.graceTimeLeftMillis = 0L
        session.sessionData
    }
    
    override suspend fun stopSession(sessionId: String): Result<SessionData> = safeCall {
        val session = activeSessions[sessionId]
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        session.isRunning = false
        val timeElapsed = session.sessionData.durationMillis - session.timeLeftMillis
        
        val finalSessionData = session.sessionData.copy(
            durationMillis = timeElapsed,
            wasCompleted = false,
            completedDate = Date()
        )
        
        activeSessions.remove(sessionId)
        finalSessionData
    }
    
    override suspend fun completeSession(sessionId: String): Result<SessionData> = safeCall {
        val session = activeSessions[sessionId]
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        
        val finalSessionData = session.sessionData.copy(
            wasCompleted = true,
            completedDate = Date()
        )
        
        activeSessions.remove(sessionId)
        finalSessionData
    }
    
    override fun getSessionProgress(sessionId: String): Flow<Result<TimerProgress>> = flow {
        val session = activeSessions[sessionId]
        if (session == null) {
            emit(Result.Error(
                IllegalArgumentException("Session not found: $sessionId"),
                "Timer session not found"
            ))
            return@flow
        }
        
        try {
            while (session.timeLeftMillis > 0 && activeSessions.containsKey(sessionId)) {
                val currentSession = activeSessions[sessionId] ?: break
                
                emit(Result.Success(TimerProgress(
                    sessionId = sessionId,
                    timeLeftMillis = currentSession.timeLeftMillis,
                    totalDurationMillis = currentSession.sessionData.durationMillis,
                    isRunning = currentSession.isRunning,
                    isCompleted = false,
                    isBackgroundMode = currentSession.isBackgroundMode,
                    graceTimeLeftMillis = currentSession.graceTimeLeftMillis
                )))
                
                // Update time if session is running
                if (currentSession.isRunning && !currentSession.isBackgroundMode) {
                    currentSession.timeLeftMillis = (currentSession.timeLeftMillis - 1000).coerceAtLeast(0)
                } else if (currentSession.isBackgroundMode && currentSession.graceTimeLeftMillis > 0) {
                    currentSession.graceTimeLeftMillis = (currentSession.graceTimeLeftMillis - 1000).coerceAtLeast(0)
                    if (currentSession.graceTimeLeftMillis <= 0) {
                        // Grace period expired, stop session
                        stopSession(sessionId)
                        break
                    }
                }
                
                // Check if session completed
                if (currentSession.timeLeftMillis <= 0) {
                    completeSession(sessionId)
                    emit(Result.Success(TimerProgress(
                        sessionId = sessionId,
                        timeLeftMillis = 0,
                        totalDurationMillis = currentSession.sessionData.durationMillis,
                        isRunning = false,
                        isCompleted = true
                    )))
                    break
                }
                
                delay(1000) // Update every second
            }
        } catch (e: Exception) {
            emit(Result.Error(e, "Timer progress tracking failed"))
        }
    }
    
    override suspend fun validateSession(sessionId: String): Result<Boolean> = safeCall {
        activeSessions.containsKey(sessionId)
    }
    
    /**
     * Handles app going to background.
     * Starts grace period for active sessions.
     */
    suspend fun onAppBackground(sessionId: String): Result<Unit> = safeCall {
        val session = activeSessions[sessionId] ?: return@safeCall
        
        if (session.isRunning) {
            session.isBackgroundMode = true
            session.graceTimeLeftMillis = gracePeriodMillis
        }
    }
    
    /**
     * Handles app returning to foreground.
     * Cancels grace period if still within time limit.
     */
    suspend fun onAppForeground(sessionId: String): Result<Unit> = safeCall {
        val session = activeSessions[sessionId] ?: return@safeCall
        
        if (session.isBackgroundMode && session.graceTimeLeftMillis > 0) {
            session.isBackgroundMode = false
            session.graceTimeLeftMillis = 0L
        }
    }
    
    /**
     * Cleanup method for memory management.
     * Follows RAII principle for resource management.
     */
    fun cleanup() {
        activeSessions.clear()
    }
    
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}