package com.fidan.timer.viewmodel

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimerState(
    val timeLeftMillis: Long = TIMER_DURATION,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val isPaused: Boolean = false,
    val progress: Float = 0f,
    val displayTime: String = "25:00",
    val sessionProgress: Int = 0,
    val totalSessions: Int = 1
)

sealed class TimerEvent {
    object SessionCompleted : TimerEvent()
    object SessionStarted : TimerEvent()
    object SessionPaused : TimerEvent()
    object SessionResumed : TimerEvent()
    object SessionReset : TimerEvent()
    data class Error(val message: String) : TimerEvent()
}

class TimerViewModel : ViewModel() {
    companion object {
        const val TIMER_DURATION = 25 * 60 * 1000L // 25 minutes in milliseconds
        const val TICK_INTERVAL = 1000L // Update every second
        private const val TAG = "TimerViewModel"
    }

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _timerEvents = MutableStateFlow<TimerEvent?>(null)
    val timerEvents: StateFlow<TimerEvent?> = _timerEvents.asStateFlow()

    private var countDownTimer: CountDownTimer? = null
    private var sessionStartTime: Long = 0
    private var isPaused: Boolean = false

    fun startTimer() {
        val currentState = _timerState.value
        if (currentState.isRunning) {
            emitEvent(TimerEvent.Error("Timer is already running"))
            return
        }

        try {
            if (!isPaused) {
                sessionStartTime = System.currentTimeMillis()
            }
            isPaused = false
            
            countDownTimer = object : CountDownTimer(currentState.timeLeftMillis, TICK_INTERVAL) {
                override fun onTick(millisUntilFinished: Long) {
                    updateTimerState(millisUntilFinished, true, false)
                }

                override fun onFinish() {
                    viewModelScope.launch {
                        _timerState.value = currentState.copy(
                            timeLeftMillis = 0,
                            isRunning = false,
                            isCompleted = true,
                            isPaused = false,
                            progress = 1f,
                            displayTime = "00:00"
                        )
                        emitEvent(TimerEvent.SessionCompleted)
                    }
                }
            }.start()

            _timerState.value = currentState.copy(
                isRunning = true, 
                isCompleted = false, 
                isPaused = false
            )
            emitEvent(if (currentState.isPaused) TimerEvent.SessionResumed else TimerEvent.SessionStarted)
        } catch (e: Exception) {
            emitEvent(TimerEvent.Error("Failed to start timer: ${e.message}"))
        }
    }

    fun pauseTimer() {
        try {
            countDownTimer?.cancel()
            countDownTimer = null
            isPaused = true
            _timerState.value = _timerState.value.copy(
                isRunning = false,
                isPaused = true
            )
            emitEvent(TimerEvent.SessionPaused)
        } catch (e: Exception) {
            emitEvent(TimerEvent.Error("Failed to pause timer: ${e.message}"))
        }
    }

    fun resetTimer() {
        try {
            countDownTimer?.cancel()
            countDownTimer = null
            isPaused = false
            sessionStartTime = 0
            _timerState.value = TimerState()
            emitEvent(TimerEvent.SessionReset)
        } catch (e: Exception) {
            emitEvent(TimerEvent.Error("Failed to reset timer: ${e.message}"))
        }
    }

    private fun updateTimerState(millisLeft: Long, isRunning: Boolean, isPaused: Boolean) {
        try {
            val progress = 1f - (millisLeft.toFloat() / TIMER_DURATION)
            val minutes = (millisLeft / 1000) / 60
            val seconds = (millisLeft / 1000) % 60
            val displayTime = String.format("%02d:%02d", minutes, seconds)

            viewModelScope.launch {
                _timerState.value = _timerState.value.copy(
                    timeLeftMillis = millisLeft,
                    isRunning = isRunning,
                    isPaused = isPaused,
                    progress = progress.coerceIn(0f, 1f),
                    displayTime = displayTime
                )
            }
        } catch (e: Exception) {
            emitEvent(TimerEvent.Error("Failed to update timer state: ${e.message}"))
        }
    }

    fun getSessionStartTime(): Long = sessionStartTime

    fun restoreTimer(storedStartTime: Long) {
        try {
            if (storedStartTime <= 0) {
                emitEvent(TimerEvent.Error("Invalid session start time"))
                return
            }

            val elapsedTime = System.currentTimeMillis() - storedStartTime
            val remainingTime = (TIMER_DURATION - elapsedTime).coerceAtLeast(0)
            
            if (remainingTime > 0) {
                sessionStartTime = storedStartTime
                isPaused = true
                _timerState.value = _timerState.value.copy(
                    timeLeftMillis = remainingTime,
                    isPaused = true
                )
                startTimer()
            } else {
                _timerState.value = TimerState(
                    timeLeftMillis = 0,
                    isRunning = false,
                    isCompleted = true,
                    progress = 1f,
                    displayTime = "00:00"
                )
                emitEvent(TimerEvent.SessionCompleted)
            }
        } catch (e: Exception) {
            emitEvent(TimerEvent.Error("Failed to restore timer: ${e.message}"))
        }
    }

    private fun emitEvent(event: TimerEvent) {
        viewModelScope.launch {
            _timerEvents.value = event
        }
    }

    fun clearEvent() {
        _timerEvents.value = null
    }

    override fun onCleared() {
        super.onCleared()
        try {
            countDownTimer?.cancel()
            countDownTimer = null
        } catch (e: Exception) {
            // Silent cleanup - logging would be appropriate here
        }
    }
}