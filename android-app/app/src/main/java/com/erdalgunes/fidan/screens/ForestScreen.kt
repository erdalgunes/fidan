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
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.erdalgunes.fidan.data.*
import com.erdalgunes.fidan.service.ForestService
import com.erdalgunes.fidan.ui.components.PixelArtTree
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
    
    @OptIn(ExperimentalFoundationApi::class)
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
                // Enhanced tree grid with smooth animations
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.forestState.trees,
                        key = { tree -> tree.id }
                    ) { tree ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(
                                animationSpec = tween(600, easing = EaseOutCubic)
                            ) + scaleIn(
                                initialScale = 0.8f,
                                animationSpec = tween(600, easing = EaseOutBack)
                            ),
                            exit = fadeOut(
                                animationSpec = tween(300)
                            ) + scaleOut(
                                targetScale = 0.8f,
                                animationSpec = tween(300)
                            ),
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = tween(400, easing = EaseInOutCubic)
                            )
                        ) {
                            Card(
                                modifier = Modifier
                                    .size(80.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (tree.maintenanceState.needsWatering || 
                                        tree.maintenanceState.hasWeeds || 
                                        tree.maintenanceState.hasPests) {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    }
                                )
                            ) {
                                PixelArtTree(
                                    tree = tree,
                                    isGrowing = false,
                                    modifier = Modifier.fillMaxSize()
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
                        
                        // Animated counters for smooth number changes
                        val animatedCompletedTrees by animateIntAsState(
                            targetValue = completedTrees,
                            animationSpec = tween(500, easing = EaseOutCubic),
                            label = "completedTrees"
                        )
                        val animatedCurrentStreak by animateIntAsState(
                            targetValue = state.forestState.currentStreak,
                            animationSpec = tween(400, easing = EaseOutBack),
                            label = "currentStreak"
                        )
                        val animatedLongestStreak by animateIntAsState(
                            targetValue = state.forestState.longestStreak,
                            animationSpec = tween(400, easing = EaseOutCubic),
                            label = "longestStreak"
                        )
                        
                        Text(
                            text = if (state.forestState.isDayTime) "☀️ Day" else "🌙 Night",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (animatedCurrentStreak > 0) {
                            Text(
                                text = "🔥 $animatedCurrentStreak streak",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (animatedLongestStreak > 0) {
                            Text(
                                text = "👑 $animatedLongestStreak best",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "$animatedCompletedTrees trees",
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