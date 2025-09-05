package com.erdalgunes.fidan.data.dao

import androidx.room.*
import com.erdalgunes.fidan.data.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * DAO for session operations.
 * Interface Segregation: Focused on session-specific operations only.
 */
@Dao
interface SessionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)
    
    @Update
    suspend fun updateSession(session: SessionEntity)
    
    @Delete
    suspend fun deleteSession(session: SessionEntity)
    
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): SessionEntity?
    
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>
    
    @Query("SELECT * FROM sessions WHERE isCompleted = 1 ORDER BY startTime DESC")
    fun getCompletedSessions(): Flow<List<SessionEntity>>
    
    @Query("SELECT * FROM sessions WHERE startTime >= :startDate AND startTime <= :endDate")
    fun getSessionsInRange(startDate: Date, endDate: Date): Flow<List<SessionEntity>>
    
    @Query("SELECT COUNT(*) FROM sessions WHERE isCompleted = 1")
    suspend fun getCompletedSessionCount(): Int
    
    @Query("SELECT SUM(actualDurationMillis) FROM sessions WHERE isCompleted = 1")
    suspend fun getTotalFocusTime(): Long?
    
    @Query("DELETE FROM sessions WHERE startTime < :beforeDate")
    suspend fun deleteOldSessions(beforeDate: Date)
}