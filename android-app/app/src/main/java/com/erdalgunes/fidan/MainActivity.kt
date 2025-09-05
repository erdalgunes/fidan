package com.erdalgunes.fidan

// Android imports
import android.os.Bundle

// AndroidX imports
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

// Compose imports
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState

// Kotlinx imports
import kotlinx.coroutines.launch

// App imports
import com.erdalgunes.fidan.data.*
import com.erdalgunes.fidan.forest.*
import com.erdalgunes.fidan.repository.*
import com.erdalgunes.fidan.ui.theme.FidanTheme
import com.erdalgunes.fidan.ui.screens.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity for the Fidan app
 * Following SOLID principles:
 * - Single Responsibility: Handles UI navigation and timer callbacks
 * - Dependency Inversion: Injects dependencies instead of creating them
 * - Interface Segregation: Implements only necessary TimerCallback interface
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity(), TimerCallback {
    @Inject
    lateinit var timerManager: TimerManager
    
    @Inject
    lateinit var forestManager: ForestManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Dependencies injected by Hilt - no manual instantiation needed
        // This follows Dependency Inversion Principle
        
        setContent {
            FidanTheme {
                FidanApp(timerManager, forestManager)
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Notify timer manager that app is going to background
        timerManager.onAppPaused()
    }
    
    override fun onResume() {
        super.onResume()
        // Notify timer manager that app is resuming
        timerManager.onAppResumed()
    }
    
    override fun onSessionCompleted() {
        // Add tree to forest when session is completed
        val sessionData = SessionData(
            taskName = "Focus Session",
            durationMillis = 25 * 60 * 1000L,
            completedDate = java.util.Date(),
            wasCompleted = true
        )
        forestManager.addTree(sessionData)
    }
    
    override fun onSessionStopped(wasRunning: Boolean, timeElapsed: Long) {
        // Add incomplete tree if session was stopped early
        if (wasRunning && timeElapsed > 0) {
            val sessionData = SessionData(
                taskName = "Focus Session (Stopped)",
                durationMillis = timeElapsed,
                completedDate = java.util.Date(),
                wasCompleted = false
            )
            forestManager.addTree(sessionData)
        }
    }
    
    override fun onError(error: String, isRecoverable: Boolean) {
        // Handle timer errors
        // Could show toast or dialog here
    }
    
    
    override fun onDestroy() {
        super.onDestroy()
        timerManager.cleanup()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FidanApp(
    timerManager: TimerManager,
    forestManager: ForestManager
) {
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
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Text("🌍", fontSize = 20.sp) },
                    label = { Text("Impact") }
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
            1 -> ForestScreen(innerPadding, forestManager)
            2 -> StatsScreen(innerPadding, completedTrees, incompleteTrees)
            3 -> ImpactScreen(innerPadding)
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