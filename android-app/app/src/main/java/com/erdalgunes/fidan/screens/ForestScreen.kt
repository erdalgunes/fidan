package com.erdalgunes.fidan.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.erdalgunes.fidan.forest.*
import com.erdalgunes.fidan.service.ForestService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectAsState
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
object ForestScreen : Screen

data class ForestState(
    val forestState: com.erdalgunes.fidan.forest.ForestState,
    val selectedTree: Tree? = null,
    val onTreeSelected: (Tree?) -> Unit = {},
    val onPlotTapped: (Plot) -> Unit = {}
) : CircuitUiState

class ForestPresenter @Inject constructor(
    private val forestService: ForestService
) : Presenter<ForestState> {
    
    @Composable
    override fun present(): ForestState {
        val forestState by forestService.forestState.collectAsState()
        var selectedTree by rememberRetained { mutableStateOf<Tree?>(null) }
        
        // Update day/night cycle periodically
        LaunchedEffect(Unit) {
            while (true) {
                delay(60000) // Update every minute
                forestService.updateDayNightCycle()
            }
        }
        
        return ForestState(
            forestState = forestState,
            selectedTree = selectedTree,
            onTreeSelected = { selectedTree = it },
            onPlotTapped = { plot ->
                plot.tree?.let { tree -> selectedTree = tree }
            }
        )
    }
}

class ForestUi @Inject constructor() : Ui<ForestState> {
    
    @Composable
    override fun Content(state: ForestState, modifier: Modifier) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            if (state.forestState.trees.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🏞️",
                        fontSize = 72.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your Forest Awaits",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Complete focus sessions to grow your virtual forest!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Farm grid
                FarmGrid(
                    forestState = state.forestState,
                    onPlotTapped = state.onPlotTapped,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Forest info overlay
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        val completedTrees = state.forestState.trees.count { it.sessionData.wasCompleted }
                        val incompleteTrees = state.forestState.trees.count { !it.sessionData.wasCompleted }
                        
                        Text(
                            text = if (state.forestState.isDayTime) "☀️ Day" else "🌙 Night",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "🌳 $completedTrees trees",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (incompleteTrees > 0) {
                            Text(
                                text = "🌱 $incompleteTrees saplings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
            
            // Tree detail dialog
            state.selectedTree?.let { tree ->
                TreeDetailDialog(
                    tree = tree,
                    onDismiss = { state.onTreeSelected(null) }
                )
            }
        }
    }
}

class ForestPresenterFactory @Inject constructor(
    private val presenter: ForestPresenter
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext
    ): Presenter<*>? {
        return when (screen) {
            is ForestScreen -> presenter
            else -> null
        }
    }
}

class ForestUiFactory @Inject constructor(
    private val ui: ForestUi
) : Ui.Factory {
    override fun create(screen: Screen, context: CircuitContext): Ui<*>? {
        return when (screen) {
            is ForestScreen -> ui
            else -> null
        }
    }
}