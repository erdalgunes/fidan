package com.erdalgunes.fidan.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.erdalgunes.fidan.service.TimerService
import com.erdalgunes.fidan.service.TimerForegroundService
import com.erdalgunes.fidan.service.PomodoroSessionService
import com.erdalgunes.fidan.domain.PomodoroState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
object TimerScreen : Screen

data class TimerState(
    val timeLeftMillis: Long = 25 * 60 * 1000L, // 25 minutes
    val isRunning: Boolean = false,
    val sessionCompleted: Boolean = false,
    val treeWithering: Boolean = false,
    val statusMessage: String = "Ready to focus",
    val currentTask: com.erdalgunes.fidan.data.ActiveMaintenanceTask? = null,
    val pomodoroState: PomodoroState = PomodoroState.IDLE,
    val completedWorkSessions: Int = 0,
    val showBreakTimer: Boolean = false,
    val onStartStop: () -> Unit = {},
    val onSessionStopped: () -> Unit = {},
    val onToggleMode: () -> Unit = {}
) : CircuitUiState

class TimerPresenter @Inject constructor(
    private val timerService: TimerService,
    private val pomodoroSessionService: PomodoroSessionService
) : Presenter<TimerState> {
    
    @Composable
    override fun present(): TimerState {
        val context = LocalContext.current
        val timerState by timerService.state.collectAsState()
        val pomodoroSession by pomodoroSessionService.sessionState.collectAsState()
        var showBreakTimer by rememberRetained { mutableStateOf(false) }
        
        return TimerState(
            timeLeftMillis = if (showBreakTimer) pomodoroSession.timeLeftMillis else timerState.timeLeftMillis,
            isRunning = if (showBreakTimer) pomodoroSession.isRunning else timerState.isRunning,
            sessionCompleted = if (showBreakTimer) pomodoroSession.currentState == PomodoroState.IDLE else timerState.sessionCompleted,
            treeWithering = timerState.treeWithering,
            statusMessage = if (showBreakTimer) getPomodoroStatusMessage(pomodoroSession.currentState) else timerService.getStatusMessage(),
            currentTask = timerService.getCurrentMaintenanceTask(),
            pomodoroState = pomodoroSession.currentState,
            completedWorkSessions = pomodoroSession.completedWorkSessions,
            showBreakTimer = showBreakTimer,
            onStartStop = {
                if (showBreakTimer) {
                    if (pomodoroSession.isRunning) {
                        pomodoroSessionService.stopSession()
                    } else {
                        pomodoroSessionService.startSession()
                    }
                } else {
                    if (timerState.isRunning) {
                        TimerForegroundService.stopService(context)
                    } else {
                        TimerForegroundService.startService(context)
                    }
                }
            },
            onSessionStopped = {
                if (showBreakTimer) {
                    pomodoroSessionService.stopSession()
                } else {
                    TimerForegroundService.stopService(context)
                }
            },
            onToggleMode = {
                showBreakTimer = !showBreakTimer
                if (showBreakTimer) {
                    // Stop old service when switching to Pomodoro mode
                    TimerForegroundService.stopService(context)
                } else {
                    // Stop Pomodoro when switching to simple mode
                    pomodoroSessionService.stopSession()
                }
            }
        )
    }
    
    private fun getPomodoroStatusMessage(state: PomodoroState): String {
        return when (state) {
            PomodoroState.IDLE -> "Ready for Pomodoro"
            PomodoroState.WORKING -> "Focus Time"
            PomodoroState.SHORT_BREAK -> "Short Break"
            PomodoroState.LONG_BREAK -> "Long Break"
        }
    }
}

class TimerUi @Inject constructor() : Ui<TimerState> {
    
    @Composable
    override fun Content(state: TimerState, modifier: Modifier) {
        val minutes = (state.timeLeftMillis / 1000) / 60
        val seconds = (state.timeLeftMillis / 1000) % 60
        val timeText = "%02d:%02d".format(minutes, seconds)
        
        val scale by animateFloatAsState(
            targetValue = if (state.isRunning) 1.02f else 1f,  // Subtle animation
            animationSpec = tween(durationMillis = 500),
            label = "timer_scale"
        )
        
        // Minimalist design with optimal spacing for focus
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 60.dp),  // Increased whitespace
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly  // Better distribution
        ) {
            // Pomodoro Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Break Timer Mode",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = state.showBreakTimer,
                    onCheckedChange = { state.onToggleMode() }
                )
            }
            
            // Pomodoro Progress Indicator
            if (state.showBreakTimer && state.completedWorkSessions > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Session Progress",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${state.completedWorkSessions} work sessions completed",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        if (state.completedWorkSessions % 4 == 0 && state.completedWorkSessions > 0) {
                            Text(
                                text = "🎉 Long break earned!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Current maintenance task display
            state.currentTask?.let { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = task.task.emoji,
                            fontSize = 32.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = task.task.displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = task.task.description,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        // Urgency indicator
                        if (task.urgency > 0.7f) {
                            Text(
                                text = "🔥 Urgent",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Minimalist timer display - focus on the essentials
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(scale)
            ) {
                // Large, clear time display - the main focus element
                Text(
                    text = timeText,
                    fontSize = 72.sp,  // Extra large for visibility
                    fontWeight = FontWeight.Light,  // Clean, modern look
                    letterSpacing = 4.sp,  // Better readability
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                
                // Simple status indicator with Pomodoro state
                Text(
                    text = when {
                        state.showBreakTimer -> {
                            when {
                                state.sessionCompleted -> "Ready for Next Pomodoro"
                                state.isRunning -> when (state.pomodoroState) {
                                    PomodoroState.WORKING -> "Focus Time 🍅"
                                    PomodoroState.SHORT_BREAK -> "Short Break ☕"
                                    PomodoroState.LONG_BREAK -> "Long Break 🌟"
                                    else -> "Focusing"
                                }
                                else -> state.statusMessage
                            }
                        }
                        state.sessionCompleted -> if (state.currentTask != null) "Task Complete! ✨" else "Session Complete"
                        state.treeWithering -> "Focus Lost"
                        state.isRunning -> if (state.currentTask != null) "Maintaining..." else "Focusing"
                        else -> if (state.currentTask != null) "Ready to Help" else "Ready"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
            }
            
            // Minimalist action button - single focus point
            Button(
                onClick = state.onStartStop,
                modifier = Modifier
                    .height(56.dp)
                    .width(200.dp),
                shape = RoundedCornerShape(28.dp),  // Smooth, modern shape
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        state.isRunning -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.primary
                    }
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,  // Flat design
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = when {
                        state.showBreakTimer -> {
                            when {
                                state.sessionCompleted -> "New Pomodoro"
                                state.isRunning -> "Stop"
                                else -> when (state.pomodoroState) {
                                    PomodoroState.WORKING -> "Start Focus"
                                    PomodoroState.SHORT_BREAK -> "Start Break"
                                    PomodoroState.LONG_BREAK -> "Start Long Break"
                                    else -> "Start Pomodoro"
                                }
                            }
                        }
                        state.sessionCompleted -> "New Session"
                        state.isRunning -> "Stop"
                        state.currentTask != null -> "Start ${state.currentTask.task.displayName}"
                        else -> "Start Focus"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

class TimerPresenterFactory @Inject constructor(
    private val presenter: TimerPresenter
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext
    ): Presenter<*>? {
        return when (screen) {
            is TimerScreen -> presenter
            else -> null
        }
    }
}

class TimerUiFactory @Inject constructor(
    private val ui: TimerUi
) : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is TimerScreen -> ui
            else -> null
        }
    }
}