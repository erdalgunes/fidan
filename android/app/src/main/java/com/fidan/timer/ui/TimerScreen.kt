package com.fidan.timer.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fidan.timer.viewmodel.TimerViewModel
import com.fidan.timer.viewmodel.TimerEvent
import kotlin.math

@Composable
fun TimerScreen(
    timerViewModel: TimerViewModel = viewModel()
) {
    val timerState by timerViewModel.timerState.collectAsState()
    val timerEvent by timerViewModel.timerEvents.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Handle timer events
    LaunchedEffect(timerEvent) {
        when (timerEvent) {
            is TimerEvent.SessionCompleted -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            is TimerEvent.SessionStarted, TimerEvent.SessionResumed -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            is TimerEvent.Error -> {
                // Show error snackbar in production
            }
            else -> {}
        }
        timerViewModel.clearEvent()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics {
                contentDescription = "Focus timer screen with 25 minute countdown"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Title
        Text(
            text = "Focus Session",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Timer Display with Tree Animation
        Box(
            modifier = Modifier
                .size(320.dp)
                .semantics {
                    contentDescription = "Timer display showing ${timerState.displayTime} remaining"
                },
            contentAlignment = Alignment.Center
        ) {
            // Tree Growth Animation
            TreeGrowthAnimation(
                progress = timerState.progress,
                isRunning = timerState.isRunning
            )
            
            // Circular Progress
            CircularTimerProgress(
                progress = timerState.progress,
                isRunning = timerState.isRunning,
                isPaused = timerState.isPaused,
                modifier = Modifier.fillMaxSize()
            )
            
            // Timer Text with animated color
            val timerTextColor by animateColorAsState(
                targetValue = when {
                    timerState.isCompleted -> MaterialTheme.colorScheme.primary
                    timerState.isPaused -> MaterialTheme.colorScheme.outline
                    timerState.isRunning -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface
                },
                animationSpec = tween(300),
                label = "timerTextColor"
            )
            
            Text(
                text = timerState.displayTime,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = timerTextColor,
                modifier = Modifier.semantics {
                    contentDescription = "Time remaining: ${timerState.displayTime}"
                }
            )
        }

        // Control Buttons
        TimerControls(
            isRunning = timerState.isRunning,
            isCompleted = timerState.isCompleted,
            progress = timerState.progress,
            onStart = { timerViewModel.startTimer() },
            onPause = { timerViewModel.pauseTimer() },
            onReset = { timerViewModel.resetTimer() }
        )

        // Enhanced Status Messages
        AnimatedVisibility(
            visible = timerState.isCompleted || timerState.isPaused,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (timerState.isCompleted) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = when {
                        timerState.isCompleted -> "🌳 Excellent focus! Your tree has grown!"
                        timerState.isPaused -> "⏸️ Session paused. Resume when ready."
                        else -> ""
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .semantics {
                            contentDescription = if (timerState.isCompleted) 
                                "Focus session completed successfully" 
                            else "Focus session is paused"
                        },
                    color = if (timerState.isCompleted) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CircularTimerProgress(
    progress: Float,
    isRunning: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (isRunning) {
            tween(durationMillis = 800, easing = EaseOutCubic)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        },
        label = "progress"
    )
    
    val progressColor by animateColorAsState(
        targetValue = when {
            isPaused -> Color(0xFFFF9800) // Orange for paused
            isRunning -> Color(0xFF4CAF50) // Green for running
            progress >= 1f -> Color(0xFF2196F3) // Blue for completed
            else -> Color(0xFF4CAF50)
        },
        animationSpec = tween(300),
        label = "progressColor"
    )

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Timer progress: ${(progress * 100).toInt()}% complete"
        }
    ) {
        drawCircularProgress(animatedProgress, progressColor)
    }
}

fun DrawScope.drawCircularProgress(progress: Float, progressColor: Color) {
    val strokeWidth = 14.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val center = Offset(size.width / 2, size.height / 2)

    // Background circle with subtle gradient effect
    drawCircle(
        color = Color.Gray.copy(alpha = 0.15f),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth)
    )

    // Progress arc with enhanced styling
    if (progress > 0f) {
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun TreeGrowthAnimation(
    progress: Float,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val treeSize by animateFloatAsState(
        targetValue = 50f + (120f * progress),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "treeGrowth"
    )
    
    val treeAlpha by animateFloatAsState(
        targetValue = 0.3f + (0.7f * progress),
        animationSpec = tween(1000),
        label = "treeAlpha"
    )
    
    // Subtle breathing animation when running
    val breathingScale by animateFloatAsState(
        targetValue = if (isRunning) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    Canvas(
        modifier = modifier
            .size((treeSize * breathingScale).dp)
            .offset(y = (-30).dp)
            .semantics {
                contentDescription = "Growing tree animation at ${(progress * 100).toInt()}% growth"
            }
    ) {
        drawTree(progress, treeAlpha)
    }
}

fun DrawScope.drawTree(progress: Float, alpha: Float) {
    val trunkColor = Color(0xFF8D6E63).copy(alpha = alpha)
    val leavesColor = Color(0xFF4CAF50).copy(alpha = alpha)
    val bloomColor = Color(0xFFFFEB3B).copy(alpha = alpha * 0.8f) // Golden blooms
    
    // Draw trunk with slight taper
    val trunkWidthBase = size.width * 0.25f
    val trunkWidthTop = size.width * 0.18f
    val trunkHeight = size.height * 0.45f
    
    drawRect(
        color = trunkColor,
        topLeft = Offset((size.width - trunkWidthBase) / 2, size.height - trunkHeight),
        size = Size(trunkWidthBase, trunkHeight * 0.7f)
    )
    
    drawRect(
        color = trunkColor,
        topLeft = Offset((size.width - trunkWidthTop) / 2, size.height - trunkHeight),
        size = Size(trunkWidthTop, trunkHeight * 0.3f)
    )
    
    // Draw leaves with multiple layers for depth
    val baseRadius = size.width * 0.35f
    val leavesRadius = baseRadius * (0.2f + 0.8f * progress)
    
    // Main canopy
    drawCircle(
        color = leavesColor,
        radius = leavesRadius,
        center = Offset(size.width / 2, size.height - trunkHeight * 0.8f)
    )
    
    // Secondary leaf clusters for mature tree
    if (progress > 0.5f) {
        val secondaryRadius = leavesRadius * 0.6f
        drawCircle(
            color = leavesColor.copy(alpha = alpha * 0.7f),
            radius = secondaryRadius,
            center = Offset(size.width * 0.3f, size.height - trunkHeight * 0.6f)
        )
        
        drawCircle(
            color = leavesColor.copy(alpha = alpha * 0.7f),
            radius = secondaryRadius,
            center = Offset(size.width * 0.7f, size.height - trunkHeight * 0.6f)
        )
    }
    
    // Add golden blooms when nearly complete
    if (progress > 0.8f) {
        val bloomRadius = 4f
        repeat(5) { i ->
            val angle = (i * 72f) * (Math.PI / 180f)
            val bloomX = size.width / 2 + (leavesRadius * 0.7f * kotlin.math.cos(angle)).toFloat()
            val bloomY = size.height - trunkHeight * 0.8f + (leavesRadius * 0.7f * kotlin.math.sin(angle)).toFloat()
            
            drawCircle(
                color = bloomColor,
                radius = bloomRadius,
                center = Offset(bloomX, bloomY)
            )
        }
    }
}

@Composable
fun TimerControls(
    isRunning: Boolean,
    isCompleted: Boolean,
    progress: Float,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val hapticFeedback = LocalHapticFeedback.current
        
        if (!isRunning && !isCompleted) {
            // Start/Resume button with enhanced styling
            Button(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onStart()
                },
                modifier = Modifier
                    .height(64.dp)
                    .width(220.dp)
                    .clip(CircleShape)
                    .semantics {
                        contentDescription = if (progress > 0f) "Resume focus session" else "Start 25 minute focus session"
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.elevatedButtonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = if (progress > 0f) "RESUME" else "START FOCUS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (isRunning) {
            // Pause button with animation
            Button(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPause()
                },
                modifier = Modifier
                    .height(60.dp)
                    .width(160.dp)
                    .semantics {
                        contentDescription = "Pause focus session"
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(
                    text = "PAUSE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        AnimatedVisibility(
            visible = isCompleted || isRunning || progress > 0f,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            OutlinedButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onReset()
                },
                modifier = Modifier
                    .height(60.dp)
                    .width(160.dp)
                    .semantics {
                        contentDescription = "Reset timer to start over"
                    },
                border = BorderStroke(
                    2.dp, 
                    MaterialTheme.colorScheme.outline
                )
            ) {
                Text(
                    text = "RESET",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}