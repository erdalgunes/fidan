package com.erdalgunes.fidan.domain

enum class PomodoroState {
    IDLE,
    WORKING,
    SHORT_BREAK,
    LONG_BREAK
}

data class PomodoroSession(
    val currentState: PomodoroState = PomodoroState.IDLE,
    val completedWorkSessions: Int = 0,
    val timeLeftMillis: Long = 0L,
    val isRunning: Boolean = false
) {
    companion object {
        const val WORK_DURATION_MILLIS = 25 * 60 * 1000L // 25 minutes
        const val SHORT_BREAK_DURATION_MILLIS = 5 * 60 * 1000L // 5 minutes  
        const val LONG_BREAK_DURATION_MILLIS = 15 * 60 * 1000L // 15 minutes
        const val SESSIONS_BEFORE_LONG_BREAK = 4
    }
    
    fun getCurrentDurationMillis(): Long {
        return when (currentState) {
            PomodoroState.IDLE -> WORK_DURATION_MILLIS
            PomodoroState.WORKING -> WORK_DURATION_MILLIS
            PomodoroState.SHORT_BREAK -> SHORT_BREAK_DURATION_MILLIS
            PomodoroState.LONG_BREAK -> LONG_BREAK_DURATION_MILLIS
        }
    }
    
    fun shouldTriggerLongBreak(): Boolean {
        return completedWorkSessions > 0 && completedWorkSessions % SESSIONS_BEFORE_LONG_BREAK == 0
    }
    
    fun getNextState(): PomodoroState {
        return when (currentState) {
            PomodoroState.IDLE -> PomodoroState.WORKING
            PomodoroState.WORKING -> {
                if (shouldTriggerLongBreak()) {
                    PomodoroState.LONG_BREAK
                } else {
                    PomodoroState.SHORT_BREAK
                }
            }
            PomodoroState.SHORT_BREAK -> PomodoroState.WORKING
            PomodoroState.LONG_BREAK -> PomodoroState.WORKING
        }
    }
}