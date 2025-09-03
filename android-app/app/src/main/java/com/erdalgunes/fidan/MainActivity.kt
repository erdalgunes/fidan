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
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity(), TimerCallback {
    private lateinit var timerManager: TimerManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        timerManager = TimerManager(this, lifecycleScope)
        
        setContent {
            FidanTheme {
                FidanApp(timerManager)
            }
        }
    }
    
    override fun onSessionCompleted() {
        // This will be handled in the Composable through state updates
    }
    
    override fun onSessionStopped(wasRunning: Boolean, timeElapsed: Long) {
        // This will be handled in the Composable through state updates
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timerManager.cleanup()
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
fun FidanApp(timerManager: TimerManager) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var completedTrees by rememberSaveable { mutableIntStateOf(0) }
    var incompleteTrees by rememberSaveable { mutableIntStateOf(0) }
    
    // Observe timer state from TimerManager
    val timerState by timerManager.state.collectAsState()
    
    // Handle session completion and early stopping
    LaunchedEffect(timerState.sessionCompleted, timerState.isRunning) {
        if (timerState.sessionCompleted && !timerState.isRunning) {
            completedTrees++
        }
    }
    
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
                timerManager = timerManager,
                onSessionStopped = { 
                    if (timerState.timeLeftMillis < 25 * 60 * 1000L) {
                        incompleteTrees++
                    }
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
    timerManager: TimerManager,
    onSessionStopped: () -> Unit
) {
    val timerState by timerManager.state.collectAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (timerState.isRunning) 1.1f else 1f,
        animationSpec = tween(300),
        label = "timer_scale"
    )
    
    val timeText = timerManager.getCurrentTimeText()
    
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
            if (timerState.isRunning) {
                FloatingActionButton(
                    onClick = { 
                        timerManager.stopTimer()
                        onSessionStopped()
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
                        timerManager.startTimer()
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
            text = timerManager.getStatusMessage(),
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
        // Preview with mock timer manager would need DI setup
        // Commenting out for now
        // FidanApp(timerManager)
    }
}