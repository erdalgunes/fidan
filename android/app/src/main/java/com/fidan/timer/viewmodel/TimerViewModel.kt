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
    val progress: Float = 0f,
    val displayTime: String = "25:00"
)

class TimerViewModel : ViewModel() {
    companion object {
        const val TIMER_DURATION = 25 * 60 * 1000L // 25 minutes in milliseconds
        const val TICK_INTERVAL = 1000L // Update every second
    }

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var countDownTimer: CountDownTimer? = null
    private var sessionStartTime: Long = 0

    fun startTimer() {
        if (_timerState.value.isRunning) return

        sessionStartTime = System.currentTimeMillis()
        
        countDownTimer = object : CountDownTimer(_timerState.value.timeLeftMillis, TICK_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerState(millisUntilFinished, true)
            }

            override fun onFinish() {
                viewModelScope.launch {
                    _timerState.value = TimerState(
                        timeLeftMillis = 0,
                        isRunning = false,
                        isCompleted = true,
                        progress = 1f,
                        displayTime = "00:00"
                    )
                }
            }
        }.start()

        _timerState.value = _timerState.value.copy(isRunning = true, isCompleted = false)
    }

    fun pauseTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    fun resetTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        _timerState.value = TimerState()
    }

    private fun updateTimerState(millisLeft: Long, isRunning: Boolean) {
        val progress = 1f - (millisLeft.toFloat() / TIMER_DURATION)
        val minutes = (millisLeft / 1000) / 60
        val seconds = (millisLeft / 1000) % 60
        val displayTime = String.format("%02d:%02d", minutes, seconds)

        viewModelScope.launch {
            _timerState.value = _timerState.value.copy(
                timeLeftMillis = millisLeft,
                isRunning = isRunning,
                progress = progress,
                displayTime = displayTime
            )
        }
    }

    fun getSessionStartTime(): Long = sessionStartTime

    fun restoreTimer(storedStartTime: Long) {
        val elapsedTime = System.currentTimeMillis() - storedStartTime
        val remainingTime = TIMER_DURATION - elapsedTime
        
        if (remainingTime > 0) {
            // Set the time remaining and start immediately to maintain consistency
            _timerState.value = _timerState.value.copy(timeLeftMillis = remainingTime)
            startTimer()
        } else {
            _timerState.value = TimerState(
                timeLeftMillis = 0,
                isRunning = false,
                isCompleted = true,
                progress = 1f,
                displayTime = "00:00"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}