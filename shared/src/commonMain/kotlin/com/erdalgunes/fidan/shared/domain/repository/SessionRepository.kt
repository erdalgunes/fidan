package com.erdalgunes.fidan.shared.domain.repository

import com.erdalgunes.fidan.shared.domain.model.Session
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for session operations.
 * SOLID: Dependency Inversion - depend on abstraction, not concrete implementation.
 * Platform-specific implementations will be provided via expect/actual.
 */
interface SessionRepository {
    suspend fun saveSession(session: Session)
    suspend fun updateSession(session: Session)
    suspend fun getSession(id: String): Session?
    suspend fun deleteSession(id: String)
    fun getAllSessions(): Flow<List<Session>>
    fun getCompletedSessions(): Flow<List<Session>>
    suspend fun getTotalFocusTimeSeconds(): Long
    suspend fun getCompletedSessionCount(): Int
}