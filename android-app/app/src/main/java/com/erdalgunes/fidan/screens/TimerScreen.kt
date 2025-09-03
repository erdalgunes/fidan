package com.erdalgunes.fidan.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.flow.collectAsState
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
    val onStartStop: () -> Unit = {},
    val onSessionStopped: () -> Unit = {}
) : CircuitUiState

class TimerPresenter @Inject constructor(
    private val timerService: TimerService
) : Presenter<TimerState> {
    
    @Composable
    override fun present(): TimerState {
        val timerState by timerService.state.collectAsState()
        
        return TimerState(
            timeLeftMillis = timerState.timeLeftMillis,
            isRunning = timerState.isRunning,
            sessionCompleted = timerState.sessionCompleted,
            treeWithering = timerState.treeWithering,
            statusMessage = timerService.getStatusMessage(),
            onStartStop = {
                if (timerState.isRunning) {
                    timerService.stopTimer()
                } else {
                    timerService.startTimer()
                }
            },
            onSessionStopped = {
                // Handle early session stop
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
            targetValue = if (state.isRunning) 1.05f else 1f,
            animationSpec = tween(durationMillis = 300),
            label = "timer_scale"
        )
        
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Timer Circle
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        when {
                            state.treeWithering -> Color(0xFFFFF3E0)
                            else -> Color.White
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Show tree emoji based on state
                    Text(
                        text = when {
                            state.treeWithering -> "🥀"
                            state.isRunning -> "🌱"
                            else -> "🌱"
                        },
                        fontSize = 32.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = timeText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            state.treeWithering -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Text(
                        text = when {
                            state.treeWithering -> "Tree Withering"
                            else -> "Focus Time"
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Start/Stop Button
            if (!state.sessionCompleted) {
                FloatingActionButton(
                    onClick = state.onStartStop,
                    modifier = Modifier.size(72.dp),
                    containerColor = if (state.isRunning) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (state.isRunning) "Stop Timer" else "Start Timer",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            } else {
                FloatingActionButton(
                    onClick = { /* Reset for new session */ },
                    modifier = Modifier.size(72.dp),
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start New Session",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
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