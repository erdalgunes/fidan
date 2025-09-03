package com.erdalgunes.fidan.forest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erdalgunes.fidan.data.*

@Composable
fun FarmGrid(
    forestState: ForestState,
    onPlotTapped: (FarmPlot) -> Unit,
    modifier: Modifier = Modifier
) {
    val plots = generateFarmPlots(forestState.trees)
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        items(plots) { plot ->
            FarmPlotItem(
                plot = plot,
                isDayTime = forestState.isDayTime,
                onClick = { onPlotTapped(plot) }
            )
        }
    }
}

@Composable
private fun FarmPlotItem(
    plot: FarmPlot,
    isDayTime: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (plot.state) {
        PlotState.EMPTY -> if (isDayTime) Color(0xFF8BC34A) else Color(0xFF4E7B47) // Grass
        PlotState.PLANTED -> if (isDayTime) Color(0xFF795548) else Color(0xFF3E2723) // Soil
    }
    
    val borderColor = if (isDayTime) Color(0xFF4CAF50) else Color(0xFF2E7D32)
    
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (plot.state) {
            PlotState.EMPTY -> {
                // Empty plot - show grass texture
                Text(
                    text = if (isDayTime) "🌿" else "🌾",
                    fontSize = 16.sp,
                    modifier = Modifier.offset(x = (-8).dp, y = (-8).dp)
                )
                Text(
                    text = if (isDayTime) "🌿" else "🌾", 
                    fontSize = 14.sp,
                    modifier = Modifier.offset(x = 6.dp, y = 4.dp)
                )
            }
            PlotState.PLANTED -> {
                plot.tree?.let { tree ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tree.treeType.emoji,
                            fontSize = when (tree.treeType) {
                                TreeType.SAPLING -> 20.sp
                                else -> 28.sp
                            }
                        )
                        if (tree.treeType != TreeType.SAPLING) {
                            // Small completion indicator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        Color(0xFF4CAF50),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                } ?: run {
                    // Just planted soil
                    Text(
                        text = "🟫",
                        fontSize = 24.sp
                    )
                }
            }
        }
        
        // Plot coordinates (for debugging - can be removed)
        if (false) { // Set to true for debugging
            Text(
                text = "${plot.gridX},${plot.gridY}",
                fontSize = 8.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

data class FarmPlot(
    val gridX: Int,
    val gridY: Int,
    val state: PlotState,
    val tree: Tree? = null
)

enum class PlotState {
    EMPTY,
    PLANTED
}

private fun generateFarmPlots(trees: List<Tree>): List<FarmPlot> {
    val gridSize = 12 // 12x12 grid for now
    val plots = mutableListOf<FarmPlot>()
    
    // Create a map of occupied positions
    val occupiedPlots = mutableMapOf<Pair<Int, Int>, Tree>()
    
    trees.forEachIndexed { index, tree ->
        val gridX = index % 6 // 6 columns
        val gridY = index / 6
        occupiedPlots[Pair(gridX, gridY)] = tree
    }
    
    // Generate grid
    for (y in 0 until gridSize) {
        for (x in 0 until 6) { // 6 columns to fit screen nicely
            val position = Pair(x, y)
            val tree = occupiedPlots[position]
            
            plots.add(
                FarmPlot(
                    gridX = x,
                    gridY = y,
                    state = if (tree != null) PlotState.PLANTED else PlotState.EMPTY,
                    tree = tree
                )
            )
        }
    }
    
    return plots
}