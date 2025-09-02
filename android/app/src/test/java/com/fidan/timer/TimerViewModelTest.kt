package com.fidan.timer

import com.fidan.timer.viewmodel.TimerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

@ExperimentalCoroutinesApi
class TimerViewModelTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun initialState_isCorrect() {
        val viewModel = TimerViewModel()
        val state = viewModel.timerState.value
        
        assertEquals(TimerViewModel.TIMER_DURATION, state.timeLeftMillis)
        assertFalse(state.isRunning)
        assertFalse(state.isCompleted)
        assertEquals(0f, state.progress, 0.01f)
        assertEquals("25:00", state.displayTime)
    }

    @Test
    fun resetTimer_resetsToInitialState() = runTest {
        val viewModel = TimerViewModel()
        
        // Simulate some progress
        viewModel.startTimer()
        testDispatcher.scheduler.advanceTimeBy(1000) // Advance by 1 second
        viewModel.pauseTimer()
        
        // Reset
        viewModel.resetTimer()
        
        val state = viewModel.timerState.value
        assertEquals(TimerViewModel.TIMER_DURATION, state.timeLeftMillis)
        assertFalse(state.isRunning)
        assertFalse(state.isCompleted)
        assertEquals(0f, state.progress, 0.01f)
        assertEquals("25:00", state.displayTime)
    }

    @Test
    fun startTimer_changesRunningState() {
        val viewModel = TimerViewModel()
        
        viewModel.startTimer()
        
        val state = viewModel.timerState.value
        assertTrue(state.isRunning)
        assertFalse(state.isCompleted)
    }

    @Test
    fun pauseTimer_stopsRunning() {
        val viewModel = TimerViewModel()
        
        viewModel.startTimer()
        viewModel.pauseTimer()
        
        val state = viewModel.timerState.value
        assertFalse(state.isRunning)
    }

    @Test
    fun sessionStartTime_isRecorded() {
        val viewModel = TimerViewModel()
        val beforeStart = System.currentTimeMillis()
        
        viewModel.startTimer()
        
        val sessionStartTime = viewModel.getSessionStartTime()
        assertTrue(sessionStartTime >= beforeStart)
        assertTrue(sessionStartTime <= System.currentTimeMillis())
    }
}