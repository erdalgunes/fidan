package com.erdalgunes.fidan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import com.erdalgunes.fidan.screens.*
import com.erdalgunes.fidan.ui.theme.FidanTheme
import com.erdalgunes.fidan.data.SessionData
import com.erdalgunes.fidan.forest.ForestManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CircuitMainActivity : ComponentActivity(), TimerCallback {
    
    @Inject
    lateinit var presenterFactories: @JvmSuppressWildcards Set<Presenter.Factory>
    
    @Inject
    lateinit var uiFactories: @JvmSuppressWildcards Set<Ui.Factory>
    
    private lateinit var timerManager: TimerManager
    private lateinit var forestManager: ForestManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        timerManager = TimerManager(this, lifecycleScope)
        forestManager = ForestManager(this)
        
        val circuit = Circuit.Builder()
            .addPresenterFactories(presenterFactories)
            .addUiFactories(uiFactories)
            .build()
        
        setContent {
            FidanTheme {
                CircuitCompositionLocals(circuit) {
                    FidanCircuitApp()
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        timerManager.onAppPaused()
    }
    
    override fun onResume() {
        super.onResume()
        timerManager.onAppResumed()
    }
    
    override fun onSessionCompleted() {
        val sessionData = SessionData(
            taskName = "Focus Session",
            durationMillis = 25 * 60 * 1000L,
            completedDate = java.util.Date(),
            wasCompleted = true
        )
        forestManager.addTree(sessionData)
    }
    
    override fun onSessionStopped(wasRunning: Boolean, timeElapsed: Long) {
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
    
    override fun onDestroy() {
        super.onDestroy()
        timerManager.cleanup()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FidanCircuitApp() {
    val backStack = rememberSaveableBackStack(root = TimerScreen)
    val circuitNavigator = rememberCircuitNavigator(backStack)
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Fidan",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Text(
                            text = "⚙️",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { 
                        selectedTab = 0
                        circuitNavigator.goTo(TimerScreen)
                    },
                    icon = { Text("⏱️", fontSize = 20.sp) },
                    label = { Text("Timer") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        circuitNavigator.goTo(ForestScreen)
                    },
                    icon = { Text("🌳", fontSize = 20.sp) },
                    label = { Text("Forest") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { 
                        selectedTab = 2
                        circuitNavigator.goTo(StatsScreen)
                    },
                    icon = { Text("📊", fontSize = 20.sp) },
                    label = { Text("Stats") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { 
                        selectedTab = 3
                        circuitNavigator.goTo(ImpactScreen)
                    },
                    icon = { Text("🌍", fontSize = 20.sp) },
                    label = { Text("Impact") }
                )
            }
        }
    ) { innerPadding ->
        NavigableCircuitContent(
            navigator = circuitNavigator,
            backStack = backStack,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}