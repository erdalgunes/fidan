package com.erdalgunes.fidan

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class TimerManagerTest {

    @Mock
    private lateinit var callback: TimerCallback
    
    private lateinit var timerManager: TimerManager
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testScope = TestScope()
        timerManager = TimerManager(callback, testScope)
    }

    @After
    fun tearDown() {
        if (::timerManager.isInitialized) {
            timerManager.cleanup()
        }
    }

    @Test
    fun `initial state should be correct`() {
        val initialState = timerManager.state.value
        
        assertFalse("Timer should not be running initially", initialState.isRunning)
        assertEquals("Initial time should be 25 minutes", 25 * 60 * 1000L, initialState.timeLeftMillis)
        assertFalse("Session should not be completed initially", initialState.sessionCompleted)
    }

    @Test
    fun `getCurrentTimeText should format time correctly`() {
        assertEquals("25:00", timerManager.getCurrentTimeText())
    }

    @Test
    fun `getStatusMessage should return correct messages for different states`() = runTest {
        assertEquals("Ready to focus?", timerManager.getStatusMessage())
        
        timerManager.startTimer()
        testScope.advanceTimeBy(100)
        assertEquals("Focus on your task!", timerManager.getStatusMessage())
        
        timerManager.stopTimer()
        assertEquals("Ready to focus?", timerManager.getStatusMessage())
    }

    @Test
    fun `startTimer should change state to running`() = runTest {
        assertFalse("Timer should not be running initially", timerManager.state.value.isRunning)
        
        timerManager.startTimer()
        testScope.advanceTimeBy(100)
        
        assertTrue("Timer should be running after start", timerManager.state.value.isRunning)
    }

    @Test
    fun `stopTimer should change state to not running`() = runTest {
        timerManager.startTimer()
        testScope.advanceTimeBy(1000)
        assertTrue("Timer should be running", timerManager.state.value.isRunning)
        
        timerManager.stopTimer()
        
        assertFalse("Timer should not be running after stop", timerManager.state.value.isRunning)
        verify(callback, times(1)).onSessionStopped(eq(true), anyLong())
    }

    @Test
    fun `resetTimer should restore initial state`() = runTest {
        timerManager.startTimer()
        testScope.advanceTimeBy(5000)
        
        timerManager.resetTimer()
        
        val state = timerManager.state.value
        assertFalse("Timer should not be running after reset", state.isRunning)
        assertEquals("Time should be reset to 25 minutes", 25 * 60 * 1000L, state.timeLeftMillis)
        assertFalse("Session should not be completed after reset", state.sessionCompleted)
    }

    @Test
    fun `starting already running timer should be ignored`() = runTest {
        timerManager.startTimer()
        testScope.advanceTimeBy(1000)
        assertTrue("Timer should be running", timerManager.state.value.isRunning)
        
        // Try to start again - should be ignored
        timerManager.startTimer()
        testScope.advanceTimeBy(1000)
        
        // Timer should still be running (no state change)
        assertTrue("Timer should still be running", timerManager.state.value.isRunning)
    }

    @Test
    fun `stopping non-running timer should be ignored`() {
        // Timer is not running initially
        timerManager.stopTimer()
        
        // Should not call callback since timer wasn't running
        verify(callback, never()).onSessionStopped(anyBoolean(), anyLong())
    }

    @Test
    fun `timer state should be observable via StateFlow`() = runTest {
        val initialState = timerManager.state.value
        assertFalse("Initial state should not be running", initialState.isRunning)
        
        timerManager.startTimer()
        testScope.advanceTimeBy(1000)
        
        val runningState = timerManager.state.value
        assertTrue("State should show timer is running", runningState.isRunning)
    }

    @Test
    fun `full session completion should call callback`() = runTest {
        timerManager.startTimer()
        
        // Complete full 25-minute session
        testScope.advanceTimeBy(25 * 60 * 1000L + 2000L)
        
        verify(callback, times(1)).onSessionCompleted()
        assertFalse("Timer should not be running after completion", timerManager.state.value.isRunning)
    }

    @Test
    fun `timer countdown should decrease time`() = runTest {
        val initialTime = timerManager.state.value.timeLeftMillis
        
        timerManager.startTimer()
        testScope.advanceTimeBy(3000) // Advance by 3 seconds
        
        val newTime = timerManager.state.value.timeLeftMillis
        assertTrue("Time should have decreased", newTime < initialTime)
    }
}