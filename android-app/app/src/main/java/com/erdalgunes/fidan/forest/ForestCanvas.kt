package com.erdalgunes.fidan.forest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erdalgunes.fidan.data.*
import kotlin.math.*

@Composable
fun ForestCanvas(
    forestState: ForestState,
    onTreeTapped: (Tree) -> Unit,
    onPanAndZoom: (offsetX: Float, offsetY: Float, scale: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Create text measurer for tree labels
    val textMeasurer = rememberTextMeasurer()
    
    // Background colors based on day/night cycle
    val backgroundColor = if (forestState.isDayTime) {
        listOf(
            Color(0xFFE3F2FD), // Light blue sky
            Color(0xFFF1F8E9)  // Light green ground
        )
    } else {
        listOf(
            Color(0xFF1A237E), // Dark blue night sky
            Color(0xFF2E4B2B)  // Dark green ground
        )
    }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Brush.verticalGradient(backgroundColor))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Convert screen coordinates to world coordinates
                    val worldX = (offset.x / forestState.scale) - forestState.offsetX
                    val worldY = (offset.y / forestState.scale) - forestState.offsetY
                    
                    // Find tapped tree
                    val tappedTree = forestState.trees.find { tree ->
                        val distance = sqrt(
                            (tree.x - worldX).pow(2) + (tree.y - worldY).pow(2)
                        )
                        distance <= 50f // Touch tolerance
                    }
                    
                    tappedTree?.let { onTreeTapped(it) }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (forestState.scale * zoom).coerceIn(0.5f, 3f)
                    val newOffsetX = forestState.offsetX + pan.x / forestState.scale
                    val newOffsetY = forestState.offsetY + pan.y / forestState.scale
                    onPanAndZoom(newOffsetX, newOffsetY, newScale)
                }
            }
    ) {
        translate(
            left = forestState.offsetX * forestState.scale,
            top = forestState.offsetY * forestState.scale
        ) {
            scale(forestState.scale, forestState.scale) {
                drawForestBackground(forestState.isDayTime)
                drawTrees(forestState.trees, textMeasurer, forestState.isDayTime)
            }
        }
    }
}

private fun DrawScope.drawForestBackground(isDayTime: Boolean) {
    // Draw ground
    val groundColor = if (isDayTime) Color(0xFF8BC34A) else Color(0xFF4E7B47)
    drawRect(
        color = groundColor,
        topLeft = Offset(0f, size.height * 0.7f),
        size = Size(size.width * 2, size.height * 0.3f)
    )
    
    // Draw some background elements
    if (isDayTime) {
        // Draw sun
        drawCircle(
            color = Color(0xFFFFEB3B),
            radius = 30f,
            center = Offset(size.width * 0.8f, size.height * 0.2f)
        )
        
        // Draw clouds
        drawCloud(Offset(size.width * 0.2f, size.height * 0.3f))
        drawCloud(Offset(size.width * 0.7f, size.height * 0.25f))
    } else {
        // Draw moon
        drawCircle(
            color = Color(0xFFF5F5F5),
            radius = 25f,
            center = Offset(size.width * 0.8f, size.height * 0.2f)
        )
        
        // Draw stars
        repeat(20) { i ->
            val x = (i * 100f + 50f) % (size.width * 1.5f)
            val y = (i * 67f + 30f) % (size.height * 0.6f)
            drawCircle(
                color = Color.White,
                radius = 2f,
                center = Offset(x, y)
            )
        }
    }
}

private fun DrawScope.drawCloud(center: Offset) {
    val cloudColor = Color.White.copy(alpha = 0.8f)
    drawCircle(cloudColor, 20f, center)
    drawCircle(cloudColor, 15f, center.copy(x = center.x - 15f))
    drawCircle(cloudColor, 15f, center.copy(x = center.x + 15f))
    drawCircle(cloudColor, 12f, center.copy(x = center.x - 8f, y = center.y - 8f))
    drawCircle(cloudColor, 12f, center.copy(x = center.x + 8f, y = center.y - 8f))
}

private fun DrawScope.drawTrees(
    trees: List<Tree>,
    textMeasurer: TextMeasurer,
    isDayTime: Boolean
) {
    trees.forEach { tree ->
        drawTree(tree, textMeasurer, isDayTime)
    }
}

private fun DrawScope.drawTree(
    tree: Tree,
    textMeasurer: TextMeasurer,
    isDayTime: Boolean
) {
    val baseSize = when (tree.treeType) {
        TreeType.SAPLING -> 20f
        TreeType.CHERRY -> 35f
        TreeType.PALM -> 40f
        TreeType.OAK -> 45f
        TreeType.PINE -> 50f
    }
    
    val treeHeight = tree.treeType.minHeight + 
        (tree.treeType.maxHeight - tree.treeType.minHeight) * 
        (tree.sessionData.durationMillis / (25 * 60 * 1000f)).coerceIn(0f, 1f)
    
    // Draw shadow if day time
    if (isDayTime) {
        val shadowOffset = Offset(tree.x + 15f, tree.y + treeHeight + 5f)
        drawOval(
            color = Color.Black.copy(alpha = 0.2f),
            topLeft = shadowOffset,
            size = Size(baseSize * 1.5f, baseSize * 0.3f)
        )
    }
    
    // Draw tree trunk
    val trunkColor = if (isDayTime) Color(0xFF8D6E63) else Color(0xFF5D4037)
    drawRect(
        color = trunkColor,
        topLeft = Offset(tree.x - 3f, tree.y + treeHeight - 20f),
        size = Size(6f, 20f)
    )
    
    // Draw tree based on type
    when (tree.treeType) {
        TreeType.OAK -> drawOakTree(tree.x, tree.y, baseSize, treeHeight, isDayTime)
        TreeType.PINE -> drawPineTree(tree.x, tree.y, baseSize, treeHeight, isDayTime)
        TreeType.PALM -> drawPalmTree(tree.x, tree.y, baseSize, treeHeight, isDayTime)
        TreeType.CHERRY -> drawCherryTree(tree.x, tree.y, baseSize, treeHeight, isDayTime)
        TreeType.SAPLING -> drawSapling(tree.x, tree.y, baseSize, treeHeight, isDayTime)
    }
    
    // Draw emoji above tree for visual appeal
    if (tree.treeType != TreeType.SAPLING) {
        val textStyle = TextStyle(
            fontSize = (baseSize * 0.6f).sp,
            color = Color.Black
        )
        val textResult = textMeasurer.measure(tree.treeType.emoji, textStyle)
        drawText(
            textResult,
            topLeft = Offset(
                tree.x - textResult.size.width / 2f,
                tree.y - 15f
            )
        )
    }
}

private fun DrawScope.drawOakTree(x: Float, y: Float, size: Float, height: Float, isDayTime: Boolean) {
    val leafColor = if (isDayTime) Color(0xFF4CAF50) else Color(0xFF2E7D32)
    
    // Draw multiple circular leaf clusters
    drawCircle(leafColor, size * 0.8f, Offset(x, y + height - size))
    drawCircle(leafColor, size * 0.6f, Offset(x - size * 0.5f, y + height - size * 0.8f))
    drawCircle(leafColor, size * 0.6f, Offset(x + size * 0.5f, y + height - size * 0.8f))
    drawCircle(leafColor, size * 0.7f, Offset(x, y + height - size * 1.3f))
}

private fun DrawScope.drawPineTree(x: Float, y: Float, size: Float, height: Float, isDayTime: Boolean) {
    val leafColor = if (isDayTime) Color(0xFF2E7D32) else Color(0xFF1B5E20)
    
    // Draw triangular layers
    val layers = 4
    for (i in 0 until layers) {
        val layerY = y + height - (i + 1) * (height / layers) * 0.8f
        val layerSize = size * (1f - i * 0.15f)
        
        // Draw triangle for each layer
        val path = Path().apply {
            moveTo(x, layerY - layerSize * 0.8f)
            lineTo(x - layerSize, layerY + layerSize * 0.3f)
            lineTo(x + layerSize, layerY + layerSize * 0.3f)
            close()
        }
        drawPath(path, leafColor)
    }
}

private fun DrawScope.drawPalmTree(x: Float, y: Float, size: Float, height: Float, isDayTime: Boolean) {
    val leafColor = if (isDayTime) Color(0xFF66BB6A) else Color(0xFF388E3C)
    
    // Draw palm fronds
    val frondCount = 6
    for (i in 0 until frondCount) {
        val angle = (i * 60f) * PI / 180f
        val frondLength = size * 1.2f
        val endX = x + cos(angle).toFloat() * frondLength
        val endY = y + height - size + sin(angle).toFloat() * frondLength * 0.3f
        
        // Draw frond as an arc
        val path = Path().apply {
            moveTo(x, y + height - size)
            quadraticBezierTo(
                x + cos(angle).toFloat() * frondLength * 0.7f,
                y + height - size - frondLength * 0.3f,
                endX, endY
            )
        }
        drawPath(
            path = path,
            color = leafColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawCherryTree(x: Float, y: Float, size: Float, height: Float, isDayTime: Boolean) {
    val leafColor = if (isDayTime) Color(0xFFE91E63) else Color(0xFFC2185B)
    
    // Draw flower clusters
    drawCircle(leafColor, size * 0.7f, Offset(x, y + height - size))
    drawCircle(leafColor, size * 0.5f, Offset(x - size * 0.6f, y + height - size * 0.7f))
    drawCircle(leafColor, size * 0.5f, Offset(x + size * 0.6f, y + height - size * 0.7f))
    
    // Add small white flowers
    val flowerPositions = listOf(
        Offset(x - 10f, y + height - size + 5f),
        Offset(x + 8f, y + height - size - 3f),
        Offset(x - 5f, y + height - size + 12f)
    )
    
    flowerPositions.forEach { pos ->
        drawCircle(Color.White, 3f, pos)
    }
}

private fun DrawScope.drawSapling(x: Float, y: Float, size: Float, height: Float, isDayTime: Boolean) {
    val leafColor = if (isDayTime) Color(0xFF8BC34A) else Color(0xFF689F38)
    
    // Simple small leaves
    drawCircle(leafColor, size * 0.5f, Offset(x, y + height - size * 0.5f))
    drawCircle(leafColor, size * 0.3f, Offset(x - size * 0.3f, y + height - size * 0.3f))
    drawCircle(leafColor, size * 0.3f, Offset(x + size * 0.3f, y + height - size * 0.3f))
}