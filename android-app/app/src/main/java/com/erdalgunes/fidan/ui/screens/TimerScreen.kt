package com.erdalgunes.fidan.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.erdalgunes.fidan.TimerManager
import com.erdalgunes.fidan.ui.components.TimerControlButton
import com.erdalgunes.fidan.ui.theme.Dimensions

/**
 * Timer screen extracted from MainActivity following KISS principle
 * Single responsibility: Display timer and controls
 */
@Composable
fun TimerScreen(
    paddingValues: PaddingValues,
    timerManager: TimerManager,
    onSessionStopped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timerState by timerManager.state.collectAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (timerState.isRunning) 1.1f else 1f,
        animationSpec = tween(300),
        label = "timer_scale"
    )
    
    val timeText = timerManager.getCurrentTimeText()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9),
                        Color(0xFFC8E6C9)
                    )
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Timer Circle
        TimerCircle(
            scale = scale,
            timeText = timeText,
            timerState = timerState
        )
        
        Spacer(modifier = Modifier.height(Dimensions.SpacingXXLarge))
        
        // Timer Controls
        TimerControls(
            isRunning = timerState.isRunning,
            onStart = { timerManager.startTimer() },
            onStop = { 
                timerManager.stopTimer()
                onSessionStopped()
            }
        )
        
        Spacer(modifier = Modifier.height(Dimensions.SpacingXLarge))
        
        // Status Message
        Text(
            text = timerManager.getStatusMessage(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun TimerCircle(
    scale: Float,
    timeText: String,
    timerState: com.erdalgunes.fidan.TimerState
) {
    Box(
        modifier = Modifier
            .size(Dimensions.TimerCircleSize)
            .scale(scale)
            .clip(CircleShape)
            .background(
                when {
                    timerState.treeWithering -> Color(0xFFFFF3E0)
                    else -> Color.White
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Tree emoji based on state
            Text(
                text = when {
                    timerState.treeWithering -> "🥀"
                    timerState.isRunning -> "🌱"
                    else -> "🌱"
                },
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = Dimensions.PaddingMedium)
            )
            Text(
                text = timeText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    timerState.treeWithering -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.primary
                }
            )
            Text(
                text = when {
                    timerState.treeWithering -> "Tree Withering"
                    else -> "Focus Time"
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimerControls(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingXLarge)
    ) {
        TimerControlButton(
            onClick = if (isRunning) onStop else onStart,
            isRunning = isRunning
        )
    }
}