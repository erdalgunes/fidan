package com.erdalgunes.fidan.shared.domain.usecase

import com.erdalgunes.fidan.shared.domain.model.Session
import com.erdalgunes.fidan.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

/**
 * Shared timer business logic for KMP.
 * SOLID: Single responsibility for timer operations.
 * DRY: Reusable across all platforms.
 */
class TimerUseCase(
    private val sessionRepository: SessionRepository,
    private val clock: Clock = Clock.System
) {
    
    suspend fun startSession(
        durationSeconds: Long = 25 * 60,
        taskName: String? = null
    ): Result<Session> {
        return try {
            val session = Session(
                id = generateSessionId(),
                startTime = clock.now(),
                targetDurationSeconds = durationSeconds,
                taskName = taskName
            )
            sessionRepository.saveSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun stopSession(sessionId: String): Result<Session> {
        return try {
            val session = sessionRepository.getSession(sessionId)
                ?: return Result.failure(NoSuchElementException("Session not found"))
            
            val updatedSession = session.copy(
                endTime = clock.now(),
                actualDurationSeconds = calculateDuration(session),
                isCompleted = false
            )
            
            sessionRepository.updateSession(updatedSession)
            Result.success(updatedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun completeSession(sessionId: String): Result<Session> {
        return try {
            val session = sessionRepository.getSession(sessionId)
                ?: return Result.failure(NoSuchElementException("Session not found"))
            
            val updatedSession = session.copy(
                endTime = clock.now(),
                actualDurationSeconds = session.targetDurationSeconds,
                isCompleted = true
            )
            
            sessionRepository.updateSession(updatedSession)
            Result.success(updatedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeSession(sessionId: String): Flow<Session?> {
        return flow {
            while (true) {
                val session = sessionRepository.getSession(sessionId)
                emit(session)
                kotlinx.coroutines.delay(1000) // Update every second
                
                if (session?.isCompleted == true || session?.endTime != null) {
                    break
                }
            }
        }
    }
    
    private fun calculateDuration(session: Session): Long {
        val endTime = session.endTime ?: clock.now()
        return (endTime - session.startTime).inWholeSeconds
    }
    
    private fun generateSessionId(): String {
        return "session_${clock.now().toEpochMilliseconds()}"
    }
}