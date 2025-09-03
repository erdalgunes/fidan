package com.erdalgunes.fidan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            FidanTheme {
                FidanApp()
            }
        }
    }
}

@Composable
fun FidanTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF4CAF50),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF81C784),
        secondary = Color(0xFF795548),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFA1887F),
        background = Color(0xFFF5F5F5),
        surface = Color.White,
        onBackground = Color(0xFF1B5E20),
        onSurface = Color(0xFF2E7D32)
    )
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FidanApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var completedTrees by rememberSaveable { mutableIntStateOf(0) }
    var incompleteTrees by rememberSaveable { mutableIntStateOf(0) }
    
    // Timer state persisted across tab changes
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var timeLeftMillis by rememberSaveable { mutableLongStateOf(25 * 60 * 1000L) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🌱",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Fidan",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { /* TODO: Open settings */ }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("⏰", fontSize = 20.sp) },
                    label = { Text("Timer") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("🌳", fontSize = 20.sp) },
                    label = { Text("Forest") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("📊", fontSize = 20.sp) },
                    label = { Text("Stats") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> TimerScreen(
                paddingValues = innerPadding,
                timeLeftMillis = timeLeftMillis,
                isRunning = isRunning,
                onTimeUpdate = { timeLeftMillis = it },
                onRunningChange = { isRunning = it },
                onSessionComplete = { 
                    completedTrees++
                    timeLeftMillis = 25 * 60 * 1000L
                },
                onSessionStopped = { 
                    incompleteTrees++
                    timeLeftMillis = 25 * 60 * 1000L
                }
            )
            1 -> ForestScreen(innerPadding, completedTrees, incompleteTrees)
            2 -> StatsScreen(innerPadding, completedTrees, incompleteTrees)
        }
    }
}

@Composable
fun TimerScreen(
    paddingValues: PaddingValues,
    timeLeftMillis: Long,
    isRunning: Boolean,
    onTimeUpdate: (Long) -> Unit,
    onRunningChange: (Boolean) -> Unit,
    onSessionComplete: () -> Unit,
    onSessionStopped: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isRunning) 1.1f else 1f,
        animationSpec = tween(300),
        label = "timer_scale"
    )
    
    // Timer countdown effect
    LaunchedEffect(isRunning, timeLeftMillis) {
        if (isRunning && timeLeftMillis > 0) {
            delay(1000)
            val newTime = timeLeftMillis - 1000
            if (newTime <= 0) {
                onTimeUpdate(0)
                onRunningChange(false)
                onSessionComplete()
            } else {
                onTimeUpdate(newTime)
            }
        }
    }
    
    val minutes = (timeLeftMillis / 1000) / 60
    val seconds = (timeLeftMillis / 1000) % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)
    
    Column(
        modifier = Modifier
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
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Focus Time",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Start and Stop Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (isRunning) {
                FloatingActionButton(
                    onClick = { 
                        onRunningChange(false)
                        if (timeLeftMillis < 25 * 60 * 1000L) {
                            onSessionStopped()
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    containerColor = MaterialTheme.colorScheme.error,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
            } else {
                FloatingActionButton(
                    onClick = { 
                        if (timeLeftMillis <= 0) {
                            onTimeUpdate(25 * 60 * 1000L)
                        }
                        onRunningChange(true)
                    },
                    modifier = Modifier.size(80.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = when {
                isRunning -> "Focus on your task!"
                timeLeftMillis <= 0 -> "Session complete!"
                else -> "Ready to focus?"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ForestScreen(
    paddingValues: PaddingValues,
    completedTrees: Int,
    incompleteTrees: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E9)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Show forest visualization
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    // Show completed trees
                    repeat(completedTrees) {
                        Text(
                            text = "🌳",
                            fontSize = 36.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    // Show incomplete trees (stubs)
                    repeat(incompleteTrees) {
                        Text(
                            text = "🌱",
                            fontSize = 36.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    // Show placeholder if no trees yet
                    if (completedTrees == 0 && incompleteTrees == 0) {
                        Text(
                            text = "🏞️",
                            fontSize = 72.sp
                        )
                    }
                }
                
                Text(
                    text = "Your Forest",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$completedTrees 🌳 Full Trees",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (incompleteTrees > 0) {
                        Text(
                            text = "$incompleteTrees 🌱 Seedlings (incomplete)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (completedTrees == 0 && incompleteTrees == 0) {
                        "Complete focus sessions to grow your virtual forest!"
                    } else {
                        "Keep focusing to grow more trees!"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatsScreen(
    paddingValues: PaddingValues,
    completedTrees: Int,
    incompleteTrees: Int
) {
    val totalSessions = completedTrees + incompleteTrees
    val completionRate = if (totalSessions > 0) {
        (completedTrees * 100) / totalSessions
    } else 0
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                title = "Today",
                value = "0h",
                subtitle = "Focus Time"
            )
            StatCard(
                title = "This Week",
                value = "0h",
                subtitle = "Total Time"
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                title = "Sessions",
                value = "$completedTrees",
                subtitle = "Completed"
            )
            StatCard(
                title = "Success",
                value = "$completionRate%",
                subtitle = "Completion Rate"
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FidanAppPreview() {
    FidanTheme {
        FidanApp()
    }
}