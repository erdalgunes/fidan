package com.erdalgunes.fidan.forest

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erdalgunes.fidan.data.*
import java.text.DateFormat
import java.util.concurrent.TimeUnit

@Composable
fun FarmGrid(
    forestState: ForestState,
    onPlotTapped: (FarmPlot) -> Unit,
    modifier: Modifier = Modifier
) {
    // Memoize plots generation to avoid recomputation
    val plots = remember(forestState.trees) {
        generateFarmPlots(forestState.trees)
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        items(
            items = plots,
            key = { plot -> "${plot.gridX}-${plot.gridY}" }
        ) { plot ->
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
    var isPressed by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    
    // Animated properties
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "plotScale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when (plot.state) {
            PlotState.EMPTY -> Color.Transparent
            PlotState.PLANTED -> if (isDayTime) Color(0xFF795548) else Color(0xFF3E2723)
        },
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = when (plot.state) {
            PlotState.EMPTY -> if (isDayTime) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color(0xFF2E7D32).copy(alpha = 0.4f)
            PlotState.PLANTED -> if (isDayTime) Color(0xFF4CAF50) else Color(0xFF2E7D32)
        },
        animationSpec = tween(300),
        label = "borderColor"
    )
    
    Box(
        modifier = Modifier
            .size(60.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (plot.state == PlotState.EMPTY) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = borderColor, bounded = true),
                onClickLabel = if (plot.tree != null) "View tree details" else "Empty plot"
            ) {
                onClick()
            }
            .semantics {
                if (plot.tree != null) {
                    val tree = plot.tree
                    val duration = formatDuration(tree.sessionData.durationMillis)
                    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(tree.sessionData.completedDate)
                    val status = if (tree.sessionData.wasCompleted) "completed" else "stopped early"
                    
                    contentDescription = "${tree.treeType.displayName}, $status focus session of $duration on $date. Tap to view details."
                    role = Role.Button
                } else {
                    contentDescription = "Empty plot at row ${plot.gridY + 1}, column ${plot.gridX + 1}"
                    role = Role.Button
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (plot.state) {
            PlotState.EMPTY -> {
                // Animated plus icon with breathing effect
                val infiniteTransition = rememberInfiniteTransition(label = "breathingAnimation")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alphaAnimation"
                )
                
                Text(
                    text = "+",
                    fontSize = 24.sp,
                    color = borderColor,
                    modifier = Modifier.alpha(alpha)
                )
            }
            PlotState.PLANTED -> {
                plot.tree?.let { tree ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    ) {
                        // Tree emoji with subtle bounce animation
                        val bounceScale by animateFloatAsState(
                            targetValue = if (plot.tree != null) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "treeScale"
                        )
                        
                        Text(
                            text = tree.treeType.emoji,
                            fontSize = when (tree.treeType) {
                                TreeType.SAPLING -> 20.sp
                                else -> 28.sp
                            },
                            modifier = Modifier.scale(bounceScale)
                        )
                        
                        if (tree.treeType != TreeType.SAPLING) {
                            Spacer(modifier = Modifier.height(2.dp))
                            // Animated completion indicator with glow effect
                            val indicatorAlpha by animateFloatAsState(
                                targetValue = 1f,
                                animationSpec = tween(500, delayMillis = 200),
                                label = "indicatorAlpha"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .alpha(indicatorAlpha)
                                    .background(
                                        Color(0xFF4CAF50),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                } ?: run {
                    // Just planted soil with fade-in effect
                    val soilAlpha by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = tween(300),
                        label = "soilAlpha"
                    )
                    
                    Text(
                        text = "🟫",
                        fontSize = 24.sp,
                        modifier = Modifier.alpha(soilAlpha)
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
    // Calculate minimum grid size based on trees, with some buffer
    val minRows = if (trees.isNotEmpty()) (trees.size - 1) / 6 + 3 else 8
    val gridRows = maxOf(minRows, 8) // At least 8 rows
    
    // Pre-allocate list for better performance
    val totalPlots = gridRows * 6
    val plots = ArrayList<FarmPlot>(totalPlots)
    
    // Create a more efficient map of occupied positions
    val occupiedPlots = trees.mapIndexedNotNull { index, tree ->
        val gridX = index % 6
        val gridY = index / 6
        Pair(gridX, gridY) to tree
    }.toMap()
    
    // Generate grid more efficiently
    repeat(gridRows) { y ->
        repeat(6) { x ->
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

// Helper function to format duration for accessibility
private fun formatDuration(durationMillis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
    return if (minutes > 0) {
        "$minutes minutes and $seconds seconds"
    } else {
        "$seconds seconds"
    }
}