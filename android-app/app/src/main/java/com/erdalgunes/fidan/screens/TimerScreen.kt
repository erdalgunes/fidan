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
    val onStartStop: () -> Unit = {},
    val onSessionStopped: () -> Unit = {}
) : CircuitUiState

class TimerPresenter @Inject constructor(
    private val timerService: TimerService
) : Presenter<TimerState> {
    
    @Composable
    override fun present(): TimerState {
        val context = LocalContext.current
        val timerState by timerService.state.collectAsState()
        
        return TimerState(
            timeLeftMillis = timerState.timeLeftMillis,
            isRunning = timerState.isRunning,
            sessionCompleted = timerState.sessionCompleted,
            treeWithering = timerState.treeWithering,
            statusMessage = timerService.getStatusMessage(),
            currentTask = timerService.getCurrentMaintenanceTask(),
            onStartStop = {
                if (timerState.isRunning) {
                    TimerForegroundService.stopService(context)
                } else {
                    TimerForegroundService.startService(context)
                }
            },
            onSessionStopped = {
                TimerForegroundService.stopService(context)
            }
        )
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
                
                // Simple status indicator
                Text(
                    text = when {
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