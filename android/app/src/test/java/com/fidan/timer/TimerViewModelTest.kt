package com.fidan.timer

import com.fidan.timer.viewmodel.TimerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class TimerViewModelTest {

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
    fun resetTimer_resetsToInitialState() {
        val viewModel = TimerViewModel()
        
        // Simulate some progress
        viewModel.startTimer()
        Thread.sleep(100) // Let timer run briefly
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