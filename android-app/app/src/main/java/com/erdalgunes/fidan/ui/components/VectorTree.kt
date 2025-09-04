package com.erdalgunes.fidan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.erdalgunes.fidan.R
import com.erdalgunes.fidan.data.Tree
import com.erdalgunes.fidan.data.TreeType

/**
 * Simple tree component using vector drawables for better performance and quality
 * Following YAGNI principle - uses existing vector resources instead of complex Canvas drawing
 */
@Composable
fun VectorTree(
    tree: Tree,
    modifier: Modifier = Modifier,
    isAnimated: Boolean = true
) {
    // Simple breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "tree_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Gentle sway animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Main tree icon
        Image(
            painter = painterResource(id = getTreeDrawable(tree.treeType)),
            contentDescription = tree.treeType.displayName,
            modifier = Modifier
                .fillMaxSize(0.7f) // Take up 70% of available space
                .scale(if (isAnimated) scale else 1f)
                .rotate(if (isAnimated) rotation else 0f)
                .alpha(tree.maintenanceState.healthLevel),
            colorFilter = ColorFilter.tint(getTreeColor(tree))
        )
    }
}

private fun getTreeDrawable(treeType: TreeType): Int {
    return when (treeType) {
        TreeType.OAK -> R.drawable.ic_tree_oak
        TreeType.PINE -> R.drawable.ic_tree_turkish_pine
        TreeType.PALM -> R.drawable.ic_tree_bamboo
        TreeType.CHERRY -> R.drawable.ic_tree_sakura
        TreeType.SAPLING -> R.drawable.ic_tree
        TreeType.GOLDEN_OAK -> R.drawable.ic_tree_oak // Same but with golden tint
        TreeType.CRYSTAL_TREE -> R.drawable.ic_tree_birch
        TreeType.ANCIENT_TREE -> R.drawable.ic_tree_banyan
    }
}

private fun getTreeColor(tree: Tree): Color {
    // Base color depends on tree type
    val baseColor = when (tree.treeType) {
        TreeType.CHERRY -> Color(0xFFE91E63) // Pink
        TreeType.GOLDEN_OAK -> Color(0xFFFFD700) // Gold
        TreeType.CRYSTAL_TREE -> Color(0xFF00BCD4) // Cyan
        TreeType.ANCIENT_TREE -> Color(0xFF9C27B0) // Purple
        TreeType.SAPLING -> Color(0xFF8BC34A) // Light green
        else -> Color(0xFF4CAF50) // Standard green
    }
    
    // Adjust color based on health
    return if (tree.maintenanceState.healthLevel < 0.5f) {
        baseColor.copy(
            red = baseColor.red * 0.7f + 0.3f, // Add brown tint for unhealthy trees
            green = baseColor.green * 0.5f
        )
    } else {
        baseColor
    }
}