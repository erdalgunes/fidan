package com.fidan.timer.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fidan.timer.viewmodel.TimerViewModel

@Composable
fun TimerScreen(
    timerViewModel: TimerViewModel = viewModel()
) {
    val timerState by timerViewModel.timerState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            // Tree Growth Animation
            TreeGrowthAnimation(progress = timerState.progress)
            
            // Circular Progress
            CircularTimerProgress(
                progress = timerState.progress,
                modifier = Modifier.fillMaxSize()
            )
            
            // Timer Text
            Text(
                text = timerState.displayTime,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Control Buttons
        TimerControls(
            isRunning = timerState.isRunning,
            isCompleted = timerState.isCompleted,
            onStart = { timerViewModel.startTimer() },
            onPause = { timerViewModel.pauseTimer() },
            onReset = { timerViewModel.resetTimer() }
        )

        // Status Message
        if (timerState.isCompleted) {
            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Great job! Your tree has grown! 🌳",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun CircularTimerProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )

    Canvas(modifier = modifier) {
        drawCircularProgress(animatedProgress)
    }
}

fun DrawScope.drawCircularProgress(progress: Float) {
    val strokeWidth = 12.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val center = Offset(size.width / 2, size.height / 2)

    // Background circle
    drawCircle(
        color = Color.Gray.copy(alpha = 0.2f),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth)
    )

    // Progress arc
    drawArc(
        color = Color(0xFF4CAF50),
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

@Composable
fun TreeGrowthAnimation(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val treeSize by animateFloatAsState(
        targetValue = 50f + (100f * progress),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "treeGrowth"
    )

    Canvas(
        modifier = modifier
            .size(treeSize.dp)
            .offset(y = (-30).dp)
    ) {
        drawTree(progress)
    }
}

fun DrawScope.drawTree(progress: Float) {
    val trunkColor = Color(0xFF8D6E63)
    val leavesColor = Color(0xFF4CAF50)
    
    // Draw trunk
    val trunkWidth = size.width * 0.2f
    val trunkHeight = size.height * 0.4f
    drawRect(
        color = trunkColor,
        topLeft = Offset((size.width - trunkWidth) / 2, size.height - trunkHeight),
        size = Size(trunkWidth, trunkHeight)
    )
    
    // Draw leaves (growing with progress)
    val leavesRadius = size.width * 0.4f * (0.3f + 0.7f * progress)
    drawCircle(
        color = leavesColor.copy(alpha = 0.8f),
        radius = leavesRadius,
        center = Offset(size.width / 2, size.height - trunkHeight)
    )
}

@Composable
fun TimerControls(
    isRunning: Boolean,
    isCompleted: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (!isRunning && !isCompleted) {
            // Start button
            Button(
                onClick = onStart,
                modifier = Modifier
                    .height(60.dp)
                    .width(200.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "START",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (isRunning) {
            // Pause button
            Button(
                onClick = onPause,
                modifier = Modifier
                    .height(60.dp)
                    .width(150.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    text = "PAUSE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        if (isCompleted || (isRunning || (!isRunning && timerState.progress > 0))) {
            // Reset button
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .height(60.dp)
                    .width(150.dp)
            ) {
                Text(
                    text = "RESET",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}