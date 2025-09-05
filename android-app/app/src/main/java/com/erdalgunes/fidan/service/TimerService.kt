package com.erdalgunes.fidan.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import com.erdalgunes.fidan.domain.SessionTimer

data class TimerServiceState(
    val timeLeftMillis: Long = 25 * 60 * 1000L, // 25 minutes
    val isRunning: Boolean = false,
    val sessionCompleted: Boolean = false,
    val treeWithering: Boolean = false,
    val isPaused: Boolean = false
)

@Singleton
class TimerService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val forestService: ForestService
) : SessionTimer {
    
    companion object {
        private const val TAG = "TimerService"
        private const val PREFS_NAME = "timer_state"
        private const val KEY_TIME_LEFT = "time_left_millis"
        private const val KEY_IS_RUNNING = "is_running"
        private const val KEY_SESSION_COMPLETED = "session_completed"
        private const val KEY_TREE_WITHERING = "tree_withering"
        private const val KEY_IS_PAUSED = "is_paused"
        private const val KEY_SESSION_START_TIME = "session_start_time"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _state = MutableStateFlow(loadPersistedState())
    val state: StateFlow<TimerServiceState> = _state.asStateFlow()
    
    private var timerJob: Job? = null
    private var pausedTime: Long = 0L
    private var sessionStartTime: Long = 0L
    private val sessionDurationMs = 25 * 60 * 1000L // 25 minutes
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    init {
        // Restore session start time from prefs
        sessionStartTime = prefs.getLong(KEY_SESSION_START_TIME, 0L)
        
        // If we were running when app was killed, resume the timer
        if (_state.value.isRunning && !_state.value.isPaused) {
            resumeTimerAfterRestart()
        }
    }
    
    override fun start() {
        android.util.Log.d("TimerService", "startTimer called, current state: ${_state.value}")
        if (_state.value.sessionCompleted) {
            resetTimer()
        }
        
        if (_state.value.isPaused) {
            resumeTimer()
        } else {
            startNewTimer()
        }
    }

    fun startTimer() {
        start()
    }
    
    private fun startNewTimer() {
        sessionStartTime = System.currentTimeMillis()
        val newState = _state.value.copy(
            isRunning = true,
            isPaused = false,
            sessionCompleted = false,
            treeWithering = false,
            timeLeftMillis = sessionDurationMs
        )
        _state.value = newState
        saveState(newState)
        startCountdown()
    }
    
    private fun resumeTimer() {
        sessionStartTime = System.currentTimeMillis() - (sessionDurationMs - _state.value.timeLeftMillis)
        _state.value = _state.value.copy(isRunning = true, isPaused = false)
        startCountdown()
    }
    
    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = scope.launch {
            android.util.Log.d("TimerService", "Countdown started")
            while (_state.value.isRunning && _state.value.timeLeftMillis > 0) {
                delay(1000)
                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - sessionStartTime
                val remaining = (sessionDurationMs - elapsed).coerceAtLeast(0)
                
                android.util.Log.d("TimerService", "Updating time: $remaining ms left")
                val newState = _state.value.copy(timeLeftMillis = remaining)
                _state.value = newState
                saveState(newState)
                
                if (remaining == 0L) {
                    completeSession()
                    break
                }
            }
        }
    }
    
    override fun stop() {
        timerJob?.cancel()
        val wasRunning = _state.value.isRunning
        _state.value = _state.value.copy(
            isRunning = false,
            isPaused = true
        )
        
        // If stopped early, mark as tree withering
        if (wasRunning && _state.value.timeLeftMillis > 0) {
            _state.value = _state.value.copy(treeWithering = true)
        }
    }

    fun stopTimer() {
        stop()
    }
    
    override fun pause() {
        timerJob?.cancel()
        pausedTime = _state.value.timeLeftMillis
        _state.value = _state.value.copy(isRunning = false, isPaused = true)
    }

    fun pauseTimer() {
        pause()
    }
    
    private fun completeSession() {
        timerJob?.cancel()
        
        // Handle maintenance task completion
        val currentTask = forestService.getCurrentMaintenanceTask()
        if (currentTask != null) {
            // Complete the maintenance task
            forestService.completeMaintenanceTask(
                taskId = currentTask.createdDate.toString(), // Simple task ID
                treeId = currentTask.treeId,
                task = currentTask.task
            )
            Log.d(TAG, "Completed maintenance task: ${currentTask.task.displayName}")
        } else {
            // No specific task, just add a new tree
            val sessionData = com.erdalgunes.fidan.data.SessionData(
                taskName = "Focus Session",
                durationMillis = 25 * 60 * 1000L,
                completedDate = java.util.Date(),
                wasCompleted = true,
                streakPosition = forestService.getCurrentStreak() + 1,
                wasPerfectFocus = true
            )
            forestService.addTree(sessionData)
            Log.d(TAG, "Added new tree for completed session")
        }
        
        // Update maintenance needs for all trees
        forestService.updateMaintenanceNeeds()
        
        _state.value = _state.value.copy(
            isRunning = false,
            sessionCompleted = true,
            timeLeftMillis = 0L,
            treeWithering = false,
            isPaused = false
        )
    }
    
    override fun reset() {
        timerJob?.cancel()
        _state.value = TimerServiceState()
        pausedTime = 0L
        sessionStartTime = 0L
    }

    fun resetTimer() {
        reset()
    }
    
    fun getStatusMessage(): String {
        return when {
            _state.value.sessionCompleted -> "Session completed!"
            _state.value.treeWithering -> "Tree withering..."
            _state.value.isRunning -> {
                val currentTask = forestService.getCurrentMaintenanceTask()
                currentTask?.task?.focusMessage ?: "Focus time active"
            }
            _state.value.isPaused -> "Timer paused"
            else -> {
                val currentTask = forestService.getCurrentMaintenanceTask()
                currentTask?.task?.focusMessage ?: "Ready to focus"
            }
        }
    }
    
    fun getCurrentMaintenanceTask(): com.erdalgunes.fidan.data.ActiveMaintenanceTask? {
        return forestService.getCurrentMaintenanceTask()
    }
    
    override fun getTimeLeftMillis(): Long {
        return _state.value.timeLeftMillis
    }
    
    override fun isRunning(): Boolean {
        return _state.value.isRunning
    }
    
    override fun isCompleted(): Boolean {
        return _state.value.sessionCompleted
    }

    fun getTimeElapsed(): Long {
        return sessionDurationMs - _state.value.timeLeftMillis
    }
    
    fun cleanup() {
        timerJob?.cancel()
        scope.cancel()
    }
    
    private fun loadPersistedState(): TimerServiceState {
        return try {
            TimerServiceState(
                timeLeftMillis = prefs.getLong(KEY_TIME_LEFT, 25 * 60 * 1000L),
                isRunning = prefs.getBoolean(KEY_IS_RUNNING, false),
                sessionCompleted = prefs.getBoolean(KEY_SESSION_COMPLETED, false),
                treeWithering = prefs.getBoolean(KEY_TREE_WITHERING, false),
                isPaused = prefs.getBoolean(KEY_IS_PAUSED, false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading timer state, using default", e)
            TimerServiceState()
        }
    }
    
    private fun saveState(state: TimerServiceState) {
        try {
            prefs.edit()
                .putLong(KEY_TIME_LEFT, state.timeLeftMillis)
                .putBoolean(KEY_IS_RUNNING, state.isRunning)
                .putBoolean(KEY_SESSION_COMPLETED, state.sessionCompleted)
                .putBoolean(KEY_TREE_WITHERING, state.treeWithering)
                .putBoolean(KEY_IS_PAUSED, state.isPaused)
                .putLong(KEY_SESSION_START_TIME, sessionStartTime)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving timer state", e)
        }
    }
    
    private fun resumeTimerAfterRestart() {
        if (sessionStartTime > 0) {
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - sessionStartTime
            val remaining = (sessionDurationMs - elapsed).coerceAtLeast(0)
            
            if (remaining > 0) {
                Log.d(TAG, "Resuming timer after restart with ${remaining}ms remaining")
                val newState = _state.value.copy(timeLeftMillis = remaining)
                _state.value = newState
                saveState(newState)
                startCountdown()
            } else {
                // Session should have completed while app was closed
                completeSession()
            }
        }
    }
}