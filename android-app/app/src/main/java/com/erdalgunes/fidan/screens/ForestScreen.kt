package com.erdalgunes.fidan.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.erdalgunes.fidan.data.*
import com.erdalgunes.fidan.service.ForestService
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
object ForestScreen : Screen

data class ForestScreenState(
    val forestState: ForestState,
    val selectedTree: Tree? = null,
    val onTreeSelected: (Tree?) -> Unit = {}
) : CircuitUiState

class ForestPresenter @Inject constructor(
    private val forestService: ForestService
) : Presenter<ForestScreenState> {
    
    @Composable
    override fun present(): ForestScreenState {
        val forestState by forestService.forestState.collectAsState()
        var selectedTree by rememberRetained { mutableStateOf<Tree?>(null) }
        
        // Update day/night cycle periodically
        LaunchedEffect(Unit) {
            while (true) {
                delay(60000) // Update every minute
                forestService.updateDayNightCycle()
            }
        }
        
        return ForestScreenState(
            forestState = forestState,
            selectedTree = selectedTree,
            onTreeSelected = { selectedTree = it }
        )
    }
}

class ForestUi @Inject constructor() : Ui<ForestScreenState> {
    
    @Composable
    override fun Content(state: ForestScreenState, modifier: Modifier) {
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
                // Simple tree grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.forestState.trees) { tree ->
                        Card(
                            modifier = Modifier
                                .size(60.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tree.treeType.emoji,
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }
                }
                
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
                        val specialTrees = state.forestState.trees.count { it.treeType.isSpecial }
                        
                        Text(
                            text = if (state.forestState.isDayTime) "☀️ Day" else "🌙 Night",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.forestState.currentStreak > 0) {
                            Text(
                                text = "🔥 ${state.forestState.currentStreak} streak",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (state.forestState.longestStreak > 0) {
                            Text(
                                text = "👑 ${state.forestState.longestStreak} best",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "$completedTrees trees",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (specialTrees > 0) {
                            Text(
                                text = "✨ $specialTrees special",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (incompleteTrees > 0) {
                            Text(
                                text = "$incompleteTrees saplings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
            
            // Tree detail display (simplified)
            state.selectedTree?.let { tree ->
                // Simple tree info - could expand this later
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