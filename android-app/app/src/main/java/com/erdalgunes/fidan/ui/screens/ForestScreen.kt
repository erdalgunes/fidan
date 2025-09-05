package com.erdalgunes.fidan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.erdalgunes.fidan.forest.ForestManager
import com.erdalgunes.fidan.forest.FarmGrid
import com.erdalgunes.fidan.forest.TreeDetailDialog
import com.erdalgunes.fidan.forest.Tree
import com.erdalgunes.fidan.ui.components.InfoCard
import com.erdalgunes.fidan.ui.theme.Dimensions

/**
 * Forest screen extracted from MainActivity following KISS principle
 * Single responsibility: Display virtual forest and tree management
 */
@Composable
fun ForestScreen(
    paddingValues: PaddingValues,
    forestManager: ForestManager,
    modifier: Modifier = Modifier
) {
    val forestState by forestManager.forestState.collectAsState()
    var selectedTree by remember { mutableStateOf<Tree?>(null) }
    
    // Update day/night cycle periodically
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60000) // Update every minute
            forestManager.updateDayNightCycle()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (forestState.trees.isEmpty()) {
            ForestEmptyState()
        } else {
            // Farm grid
            FarmGrid(
                forestState = forestState,
                onPlotTapped = { plot ->
                    plot.tree?.let { tree -> selectedTree = tree }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Forest info overlay
            ForestInfoOverlay(
                forestState = forestState,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        
        // Tree detail dialog
        selectedTree?.let { tree ->
            TreeDetailDialog(
                tree = tree,
                onDismiss = { selectedTree = null }
            )
        }
    }
}

@Composable
private fun ForestEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.PaddingHuge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏞️",
            fontSize = 72.sp
        )
        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
        Text(
            text = "Your Forest Awaits",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
        Text(
            text = "Complete focus sessions to grow your virtual forest!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ForestInfoOverlay(
    forestState: com.erdalgunes.fidan.forest.ForestState,
    modifier: Modifier = Modifier
) {
    InfoCard(
        modifier = modifier.padding(Dimensions.PaddingLarge)
    ) {
        val completedTrees = forestState.trees.count { it.sessionData.wasCompleted }
        val incompleteTrees = forestState.trees.count { !it.sessionData.wasCompleted }
        
        Text(
            text = if (forestState.isDayTime) "☀️ Day" else "🌙 Night",
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