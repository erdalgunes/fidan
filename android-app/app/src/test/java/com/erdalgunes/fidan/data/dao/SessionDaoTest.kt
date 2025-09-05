package com.erdalgunes.fidan.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.erdalgunes.fidan.data.database.FidanDatabase
import com.erdalgunes.fidan.data.entity.SessionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * DAO tests for SessionDao.
 * KISS: Simple, focused tests for each DAO operation.
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SessionDaoTest {
    
    private lateinit var database: FidanDatabase
    private lateinit var sessionDao: SessionDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FidanDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        
        sessionDao = database.sessionDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveSession() = runTest {
        // Given
        val session = createTestSession("test-1")
        
        // When
        sessionDao.insertSession(session)
        val retrieved = sessionDao.getSession("test-1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(session.id, retrieved.id)
        assertEquals(session.taskName, retrieved.taskName)
    }
    
    @Test
    fun updateSession() = runTest {
        // Given
        val session = createTestSession("test-1")
        sessionDao.insertSession(session)
        
        // When
        val updatedSession = session.copy(
            isCompleted = true,
            actualDurationMillis = 1500000
        )
        sessionDao.updateSession(updatedSession)
        val retrieved = sessionDao.getSession("test-1")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(true, retrieved.isCompleted)
        assertEquals(1500000, retrieved.actualDurationMillis)
    }
    
    @Test
    fun deleteSession() = runTest {
        // Given
        val session = createTestSession("test-1")
        sessionDao.insertSession(session)
        
        // When
        sessionDao.deleteSession(session)
        val retrieved = sessionDao.getSession("test-1")
        
        // Then
        assertNull(retrieved)
    }
    
    @Test
    fun getCompletedSessionsReturnsOnlyCompleted() = runTest {
        // Given
        sessionDao.insertSession(createTestSession("test-1", isCompleted = true))
        sessionDao.insertSession(createTestSession("test-2", isCompleted = false))
        sessionDao.insertSession(createTestSession("test-3", isCompleted = true))
        
        // When
        val completedSessions = sessionDao.getCompletedSessions().first()
        
        // Then
        assertEquals(2, completedSessions.size)
        assertEquals(true, completedSessions.all { it.isCompleted })
    }
    
    @Test
    fun getTotalFocusTimeCalculatesCorrectly() = runTest {
        // Given
        sessionDao.insertSession(
            createTestSession("test-1", isCompleted = true, durationMillis = 1500000)
        )
        sessionDao.insertSession(
            createTestSession("test-2", isCompleted = true, durationMillis = 1500000)
        )
        sessionDao.insertSession(
            createTestSession("test-3", isCompleted = false, durationMillis = 500000)
        )
        
        // When
        val totalTime = sessionDao.getTotalFocusTime()
        
        // Then
        assertEquals(3000000L, totalTime)
    }
    
    private fun createTestSession(
        id: String,
        isCompleted: Boolean = false,
        durationMillis: Long = 1500000
    ) = SessionEntity(
        id = id,
        startTime = Date(),
        endTime = if (isCompleted) Date() else null,
        targetDurationMillis = 1500000,
        actualDurationMillis = durationMillis,
        isCompleted = isCompleted,
        taskName = "Test Task",
        treeId = null
    )
}