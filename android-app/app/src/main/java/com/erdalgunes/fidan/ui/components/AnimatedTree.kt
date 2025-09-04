package com.erdalgunes.fidan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.erdalgunes.fidan.data.Tree
import com.erdalgunes.fidan.data.TreeType
import kotlin.math.*
import kotlin.random.Random

enum class TreeGrowthStage {
    SEED,
    SPROUT, 
    SAPLING,
    YOUNG_TREE,
    MATURE_TREE,
    SPECIAL_TREE
}

enum class PerformanceLevel {
    HIGH,    // Full details, all animations
    MEDIUM,  // Reduced effects, simplified branches  
    LOW      // Minimal animations, basic shapes only
}

data class TreeAnimationState(
    val stage: TreeGrowthStage,
    val growthProgress: Float = 0f,
    val branchCount: Int = 0,
    val height: Float = 0f,
    val leafDensity: Float = 0f,
    val specialEffect: Boolean = false
)

@Composable
fun AnimatedTree(
    tree: Tree,
    modifier: Modifier = Modifier,
    isGrowing: Boolean = false,
    performanceLevel: PerformanceLevel = PerformanceLevel.HIGH,
    onGrowthComplete: () -> Unit = {}
) {
    // Use proper tree colors instead of theme colors
    val trunkColor = Color(0xFF8D6E63) // Brown trunk
    val primaryColor = Color(0xFF4CAF50) // Green leaves  
    val secondaryColor = Color(0xFF66BB6A) // Light green
    val tertiaryColor = Color(0xFFE91E63) // Pink for cherry
    val surfaceColor = Color(0xFF795548) // Brown for seed
    
    // Determine target stage based on tree type and session data
    val targetStage = when {
        !tree.sessionData.wasCompleted -> TreeGrowthStage.SAPLING
        tree.treeType.isSpecial -> TreeGrowthStage.SPECIAL_TREE
        tree.sessionData.streakPosition >= 3 -> TreeGrowthStage.MATURE_TREE
        tree.sessionData.streakPosition >= 1 -> TreeGrowthStage.YOUNG_TREE
        else -> TreeGrowthStage.SAPLING
    }
    
    // Animation state - start at target stage for completed trees
    var currentStage by remember(tree.id) { 
        mutableStateOf(if (isGrowing) TreeGrowthStage.SEED else targetStage) 
    }
    
    // Growth progress animation - always show full progress for non-growing trees
    val growthProgress by animateFloatAsState(
        targetValue = if (isGrowing || currentStage.ordinal < targetStage.ordinal) 1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = {
            if (it == 1f && currentStage.ordinal < targetStage.ordinal) {
                // Move to next stage
                val nextStage = TreeGrowthStage.values()[currentStage.ordinal + 1]
                currentStage = nextStage
                
                if (currentStage == targetStage) {
                    onGrowthComplete()
                }
            }
        },
        label = "tree_growth"
    )
    
    // Special effect animation for special trees
    val specialEffectProgress by animateFloatAsState(
        targetValue = if (tree.treeType.isSpecial && currentStage == TreeGrowthStage.SPECIAL_TREE) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "special_effect"
    )
    
    // Wind sway animation - disabled for low performance
    val windSway by animateFloatAsState(
        targetValue = if (currentStage.ordinal >= TreeGrowthStage.YOUNG_TREE.ordinal && 
            performanceLevel != PerformanceLevel.LOW) 
            sin(System.currentTimeMillis() / 3000f) * 2f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "wind_sway"
    )
    
    Canvas(
        modifier = modifier.size(80.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height
        
        drawAnimatedTree(
            centerX = centerX,
            centerY = centerY,
            stage = currentStage,
            progress = growthProgress,
            treeType = tree.treeType,
            windSway = windSway,
            specialEffect = specialEffectProgress,
            performanceLevel = performanceLevel,
            trunkColor = trunkColor,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            tertiaryColor = tertiaryColor,
            surfaceColor = surfaceColor
        )
    }
}

private fun DrawScope.drawAnimatedTree(
    centerX: Float,
    centerY: Float,
    stage: TreeGrowthStage,
    progress: Float,
    treeType: TreeType,
    windSway: Float,
    specialEffect: Float,
    performanceLevel: PerformanceLevel,
    trunkColor: Color,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    surfaceColor: Color
) {
    // Apply wind sway transformation
    rotate(windSway, pivot = Offset(centerX, centerY)) {
        when (stage) {
            TreeGrowthStage.SEED -> drawSeed(centerX, centerY, progress, surfaceColor)
            TreeGrowthStage.SPROUT -> drawSprout(centerX, centerY, progress, primaryColor)
            TreeGrowthStage.SAPLING -> drawSapling(centerX, centerY, progress, trunkColor, secondaryColor)
            TreeGrowthStage.YOUNG_TREE -> drawYoungTree(centerX, centerY, progress, treeType, performanceLevel, trunkColor, primaryColor)
            TreeGrowthStage.MATURE_TREE -> drawMatureTree(centerX, centerY, progress, treeType, performanceLevel, trunkColor, primaryColor, tertiaryColor)
            TreeGrowthStage.SPECIAL_TREE -> drawSpecialTree(centerX, centerY, progress, treeType, specialEffect, performanceLevel, trunkColor, primaryColor, tertiaryColor)
        }
    }
}

private fun DrawScope.drawSeed(centerX: Float, centerY: Float, progress: Float, color: Color) {
    val scale = size.width / 80f  // Scale based on expected 80dp width
    val radius = 4f * scale * progress
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(centerX, centerY - radius)
    )
}

private fun DrawScope.drawSprout(centerX: Float, centerY: Float, progress: Float, color: Color) {
    val scale = size.width / 80f
    val height = 15f * scale * progress
    
    // Draw small stem
    drawLine(
        color = color,
        start = Offset(centerX, centerY),
        end = Offset(centerX, centerY - height),
        strokeWidth = 2f * scale
    )
    
    // Draw tiny leaves
    if (progress > 0.5f) {
        val leafProgress = (progress - 0.5f) * 2f
        drawCircle(
            color = color.copy(alpha = 0.7f),
            radius = 3f * scale * leafProgress,
            center = Offset(centerX - 4f * scale, centerY - height * 0.8f)
        )
        drawCircle(
            color = color.copy(alpha = 0.7f),
            radius = 3f * scale * leafProgress,
            center = Offset(centerX + 4f * scale, centerY - height * 0.8f)
        )
    }
}

private fun DrawScope.drawSapling(centerX: Float, centerY: Float, progress: Float, trunkColor: Color, leafColor: Color) {
    val scale = size.width / 80f
    val height = 30f * scale * progress
    val trunkWidth = 3f * scale
    
    // Draw trunk
    drawLine(
        color = trunkColor,
        start = Offset(centerX, centerY),
        end = Offset(centerX, centerY - height),
        strokeWidth = trunkWidth
    )
    
    // Draw simple crown
    if (progress > 0.6f) {
        val crownProgress = (progress - 0.6f) * 2.5f
        val crownRadius = 12f * scale * crownProgress
        
        drawCircle(
            color = leafColor.copy(alpha = 0.8f),
            radius = crownRadius,
            center = Offset(centerX, centerY - height + 8f * scale)
        )
    }
}

private fun DrawScope.drawYoungTree(
    centerX: Float, 
    centerY: Float, 
    progress: Float, 
    treeType: TreeType,
    performanceLevel: PerformanceLevel,
    trunkColor: Color,
    leafColor: Color
) {
    val scale = size.width / 80f
    val height = 50f * scale * progress
    val trunkWidth = 4f * scale
    
    // Draw main trunk
    drawLine(
        color = trunkColor,
        start = Offset(centerX, centerY),
        end = Offset(centerX, centerY - height),
        strokeWidth = trunkWidth
    )
    
    // Draw branches based on tree type
    if (progress > 0.4f) {
        val branchProgress = (progress - 0.4f) * 1.67f
        val branchCount = when (performanceLevel) {
            PerformanceLevel.LOW -> 1
            PerformanceLevel.MEDIUM -> 2
            PerformanceLevel.HIGH -> 2
        }
        drawBranches(centerX, centerY - height * 0.7f, height * 0.3f, branchProgress, branchCount, performanceLevel, trunkColor, leafColor, treeType)
    }
}

private fun DrawScope.drawMatureTree(
    centerX: Float,
    centerY: Float,
    progress: Float,
    treeType: TreeType,
    performanceLevel: PerformanceLevel,
    trunkColor: Color,
    leafColor: Color,
    accentColor: Color
) {
    val scale = size.width / 80f
    val height = 70f * scale * progress
    val trunkWidth = 6f * scale
    
    // Draw thick trunk with texture
    drawLine(
        color = trunkColor,
        start = Offset(centerX, centerY),
        end = Offset(centerX, centerY - height),
        strokeWidth = trunkWidth,
        cap = StrokeCap.Round
    )
    
    // Add trunk texture
    for (i in 0..3) {
        val y = centerY - height * (0.2f + i * 0.2f)
        drawLine(
            color = trunkColor.copy(alpha = 0.3f),
            start = Offset(centerX - trunkWidth/2, y),
            end = Offset(centerX + trunkWidth/2, y),
            strokeWidth = 1f * scale
        )
    }
    
    // Draw complex branch system
    if (progress > 0.3f) {
        val branchProgress = (progress - 0.3f) * 1.43f
        val branchCount1 = when (performanceLevel) {
            PerformanceLevel.LOW -> 2
            PerformanceLevel.MEDIUM -> 3
            PerformanceLevel.HIGH -> 4
        }
        val branchCount2 = when (performanceLevel) {
            PerformanceLevel.LOW -> 1
            PerformanceLevel.MEDIUM -> 2
            PerformanceLevel.HIGH -> 3
        }
        val branchCount3 = when (performanceLevel) {
            PerformanceLevel.LOW -> 1
            PerformanceLevel.MEDIUM -> 1
            PerformanceLevel.HIGH -> 2
        }
        drawBranches(centerX, centerY - height * 0.8f, height * 0.4f, branchProgress, branchCount1, performanceLevel, trunkColor, leafColor, treeType)
        drawBranches(centerX, centerY - height * 0.6f, height * 0.3f, branchProgress, branchCount2, performanceLevel, trunkColor, leafColor, treeType)
        drawBranches(centerX, centerY - height * 0.4f, height * 0.2f, branchProgress, branchCount3, performanceLevel, trunkColor, leafColor, treeType)
    }
    
    // Add fruit/flowers for mature trees
    if (progress > 0.8f && treeType == TreeType.CHERRY) {
        val fruitProgress = (progress - 0.8f) * 5f
        drawFruit(centerX, centerY - height * 0.6f, fruitProgress, accentColor)
    }
}

private fun DrawScope.drawSpecialTree(
    centerX: Float,
    centerY: Float,
    progress: Float,
    treeType: TreeType,
    specialEffect: Float,
    performanceLevel: PerformanceLevel,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color
) {
    // Draw base mature tree
    drawMatureTree(centerX, centerY, progress, treeType, performanceLevel, primaryColor, secondaryColor, tertiaryColor)
    
    if (progress > 0.9f) {
        val scale = size.width / 80f
        // Add special effects based on tree type
        when (treeType) {
            TreeType.GOLDEN_OAK -> drawGoldenEffect(centerX, centerY - 35f * scale, specialEffect, Color.Yellow)
            TreeType.CRYSTAL_TREE -> drawCrystalEffect(centerX, centerY - 35f * scale, specialEffect, Color.Cyan)
            TreeType.ANCIENT_TREE -> drawAncientEffect(centerX, centerY - 35f * scale, specialEffect, Color.White)
            else -> {}
        }
    }
}

private fun DrawScope.drawBranches(
    x: Float,
    y: Float,
    baseLength: Float,
    progress: Float,
    branchCount: Int,
    performanceLevel: PerformanceLevel,
    trunkColor: Color,
    leafColor: Color,
    treeType: TreeType
) {
    val scale = size.width / 80f
    val angleStep = 360f / branchCount
    
    repeat(branchCount) { i ->
        val angle = i * angleStep + Random.nextFloat() * 30f - 15f
        val length = baseLength * (0.7f + Random.nextFloat() * 0.3f) * progress
        val endX = x + cos(Math.toRadians(angle.toDouble())).toFloat() * length
        val endY = y - sin(Math.toRadians(angle.toDouble())).toFloat() * length * 0.5f
        
        // Draw branch
        drawLine(
            color = trunkColor,
            start = Offset(x, y),
            end = Offset(endX, endY),
            strokeWidth = 2f * scale * progress,
            cap = StrokeCap.Round
        )
        
        // Draw leaves at branch end
        if (progress > 0.5f) {
            val leafSize = when (treeType) {
                TreeType.PINE -> 6f * scale * progress
                TreeType.PALM -> 4f * scale * progress  
                else -> 8f * scale * progress
            }
            
            drawCircle(
                color = leafColor.copy(alpha = 0.7f),
                radius = leafSize,
                center = Offset(endX, endY)
            )
        }
    }
}

private fun DrawScope.drawFruit(x: Float, y: Float, progress: Float, color: Color) {
    val scale = size.width / 80f
    repeat(3) { i ->
        val offsetX = (i - 1) * 15f * scale
        val offsetY = Random.nextFloat() * 10f * scale
        drawCircle(
            color = color.copy(alpha = 0.8f * progress),
            radius = 3f * scale * progress,
            center = Offset(x + offsetX, y + offsetY)
        )
    }
}

private fun DrawScope.drawGoldenEffect(x: Float, y: Float, progress: Float, color: Color) {
    val scale = size.width / 80f
    // Golden sparkles
    repeat(6) { i ->
        val angle = i * 60f + progress * 360f
        val distance = (25f + sin(progress * PI).toFloat() * 5f) * scale
        val sparkleX = x + cos(Math.toRadians(angle.toDouble())).toFloat() * distance
        val sparkleY = y + sin(Math.toRadians(angle.toDouble())).toFloat() * distance
        
        drawCircle(
            color = color.copy(alpha = 0.6f + sin(progress * PI).toFloat() * 0.4f),
            radius = (2f + sin(progress * PI).toFloat() * 1f) * scale,
            center = Offset(sparkleX, sparkleY)
        )
    }
}

private fun DrawScope.drawCrystalEffect(x: Float, y: Float, progress: Float, color: Color) {
    val scale = size.width / 80f
    // Crystal shards rotating
    repeat(4) { i ->
        val angle = i * 90f + progress * 180f
        val distance = 20f * scale
        val shardX = x + cos(Math.toRadians(angle.toDouble())).toFloat() * distance
        val shardY = y + sin(Math.toRadians(angle.toDouble())).toFloat() * distance
        
        drawLine(
            color = color.copy(alpha = 0.7f),
            start = Offset(shardX - 3f * scale, shardY - 3f * scale),
            end = Offset(shardX + 3f * scale, shardY + 3f * scale),
            strokeWidth = 2f * scale
        )
        drawLine(
            color = color.copy(alpha = 0.7f),
            start = Offset(shardX - 3f * scale, shardY + 3f * scale),
            end = Offset(shardX + 3f * scale, shardY - 3f * scale),
            strokeWidth = 2f * scale
        )
    }
}

private fun DrawScope.drawAncientEffect(x: Float, y: Float, progress: Float, color: Color) {
    val scale = size.width / 80f
    // Ethereal glow
    val glowRadius = (30f + sin(progress * PI).toFloat() * 10f) * scale
    val brush = RadialGradientShader(
        colors = listOf(color.copy(alpha = 0.1f), Color.Transparent),
        center = Offset(x, y),
        radius = glowRadius
    )
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.2f), Color.Transparent),
            center = Offset(x, y),
            radius = glowRadius
        ),
        radius = glowRadius,
        center = Offset(x, y)
    )
}