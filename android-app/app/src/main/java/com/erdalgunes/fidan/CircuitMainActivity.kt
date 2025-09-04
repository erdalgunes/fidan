package com.erdalgunes.fidan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.erdalgunes.fidan.R
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
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
import com.erdalgunes.fidan.service.TimerService
import com.erdalgunes.fidan.service.ForestService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CircuitMainActivity : ComponentActivity() {
    
    @Inject
    lateinit var presenterFactories: @JvmSuppressWildcards Set<Presenter.Factory>
    
    @Inject
    lateinit var uiFactories: @JvmSuppressWildcards Set<Ui.Factory>
    
    @Inject
    lateinit var timerService: TimerService
    
    @Inject
    lateinit var forestService: ForestService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val circuit = Circuit.Builder()
            .addPresenterFactories(presenterFactories)
            .addUiFactories(uiFactories)
            .build()
        
        // Observe timer completion to add trees
        lifecycleScope.launch {
            try {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    timerService.state.collect { timerState ->
                        try {
                            if (timerState.sessionCompleted) {
                                val sessionData = SessionData(
                                    taskName = "Focus Session",
                                    durationMillis = 25 * 60 * 1000L,
                                    completedDate = java.util.Date(),
                                    wasCompleted = true
                                )
                                forestService.addTree(sessionData)
                                timerService.resetTimer()
                            }
                            
                            if (timerState.treeWithering && !timerState.isRunning) {
                                val sessionData = SessionData(
                                    taskName = "Focus Session (Stopped)",
                                    durationMillis = timerService.getTimeElapsed(),
                                    completedDate = java.util.Date(),
                                    wasCompleted = false
                                )
                                forestService.addTree(sessionData)
                            }
                        } catch (e: Exception) {
                            // Log error but continue - don't crash the main activity
                            println("Error processing timer state: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle lifecycle/collection errors
                println("Error in timer observation lifecycle: ${e.message}")
            }
        }
        
        setContent {
            FidanTheme(dynamicColor = false) {
                CircuitCompositionLocals(circuit) {
                    FidanCircuitApp()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timerService.cleanup()
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
                    IconButton(onClick = { 
                        circuitNavigator.goTo(SettingsScreen)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
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
                    onClick = { 
                        selectedTab = 0
                        circuitNavigator.goTo(TimerScreen)
                    },
                    icon = { 
                        Icon(
                            painter = painterResource(id = R.drawable.ic_timer),
                            contentDescription = "Timer"
                        )
                    },
                    label = { Text("Timer") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        circuitNavigator.goTo(ForestScreen)
                    },
                    icon = { 
                        Icon(
                            painter = painterResource(id = R.drawable.ic_forest),
                            contentDescription = "Forest"
                        )
                    },
                    label = { Text("Forest") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { 
                        selectedTab = 2
                        circuitNavigator.goTo(StatsScreen)
                    },
                    icon = { 
                        Icon(
                            painter = painterResource(id = R.drawable.ic_stats),
                            contentDescription = "Stats"
                        )
                    },
                    label = { Text("Stats") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { 
                        selectedTab = 3
                        circuitNavigator.goTo(ImpactScreen)
                    },
                    icon = { 
                        Icon(
                            painter = painterResource(id = R.drawable.ic_impact),
                            contentDescription = "Impact"
                        )
                    },
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