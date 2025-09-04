package com.erdalgunes.fidan.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import kotlinx.parcelize.Parcelize
import com.erdalgunes.fidan.BuildConfig
import com.erdalgunes.fidan.data.TreeType
import com.erdalgunes.fidan.service.ForestService
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Parcelize
object SettingsScreen : Screen

data class SettingsScreenState(
    val onBackClick: () -> Unit = {},
    val onClearForest: () -> Unit = {},
    val onAddDemoTrees: () -> Unit = {},
    val onAddRandomTree: () -> Unit = {},
    val onSimulateSession: (Int) -> Unit = {}
) : CircuitUiState

class SettingsPresenter(
    private val navigator: Navigator,
    private val forestService: ForestService
) : Presenter<SettingsScreenState> {
    
    @Composable
    override fun present(): SettingsScreenState {
        val scope = rememberCoroutineScope()
        
        return SettingsScreenState(
            onBackClick = { navigator.pop() },
            onClearForest = {
                scope.launch {
                    try {
                        forestService.clearAllTrees()
                    } catch (e: Exception) {
                        // Log error but don't crash - debug operation should be robust
                        println("Error clearing forest: ${e.message}")
                    }
                }
            },
            onAddDemoTrees = {
                scope.launch {
                    try {
                        forestService.clearAllTrees()
                        // Add variety of trees
                        forestService.addDebugTree(TreeType.OAK)
                        forestService.addDebugTree(TreeType.CHERRY)
                        forestService.addDebugTree(TreeType.PINE)
                        forestService.addDebugTree(TreeType.PALM, needsWatering = true)
                        forestService.addDebugTree(TreeType.SAPLING)
                    } catch (e: Exception) {
                        println("Error adding demo trees: ${e.message}")
                    }
                }
            },
            onAddRandomTree = {
                scope.launch {
                    try {
                        val types = TreeType.values()
                        forestService.addDebugTree(types.random())
                    } catch (e: Exception) {
                        println("Error adding random tree: ${e.message}")
                    }
                }
            },
            onSimulateSession = { minutes ->
                scope.launch {
                    try {
                        forestService.simulateCompletedSession(minutes)
                    } catch (e: Exception) {
                        println("Error simulating session: ${e.message}")
                    }
                }
            }
        )
    }
}

class SettingsUi @Inject constructor() : Ui<SettingsScreenState> {
    
    @Composable
    override fun Content(state: SettingsScreenState, modifier: Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = state.onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
                // App Info Section
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Fidan - Focus & Grow",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version 1.0.0${if (BuildConfig.DEBUG) " (Debug)" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Debug Section (only in debug builds)
                if (BuildConfig.DEBUG) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "🛠️ Debug Tools",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            
                            Text(
                                text = "Testing Controls",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            
                            // Forest Management
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = state.onClearForest,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Clear Forest")
                                }
                                
                                Button(
                                    onClick = state.onAddDemoTrees,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Demo Trees")
                                }
                            }
                            
                            // Add Trees
                            Button(
                                onClick = state.onAddRandomTree,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add Random Tree")
                            }
                            
                            Divider(
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f)
                            )
                            
                            Text(
                                text = "Simulate Sessions",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            
                            // Session Simulation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { state.onSimulateSession(5) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("5 min")
                                }
                                
                                OutlinedButton(
                                    onClick = { state.onSimulateSession(25) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("25 min")
                                }
                                
                                OutlinedButton(
                                    onClick = { state.onSimulateSession(50) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("50 min")
                                }
                            }
                            
                            Text(
                                text = "⚠️ Debug features are only available in debug builds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // About Section
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Fidan helps you focus and grow your virtual forest. Each successful focus session plants a tree in your forest.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

class SettingsPresenterFactory @Inject constructor(
    private val forestService: ForestService
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext
    ): Presenter<*>? {
        return when (screen) {
            is SettingsScreen -> SettingsPresenter(navigator, forestService)
            else -> null
        }
    }
}

class SettingsUiFactory @Inject constructor(
    private val ui: SettingsUi
) : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is SettingsScreen -> ui
            else -> null
        }
    }
}