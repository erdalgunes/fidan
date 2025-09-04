package com.erdalgunes.fidan.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.erdalgunes.fidan.R
import com.erdalgunes.fidan.data.Tree
import com.erdalgunes.fidan.data.TreeType
import kotlinx.coroutines.delay

/**
 * Pixel art tree component using sprite-based animation
 * Follows Stardew Valley style with crisp pixel graphics
 */
@Composable
fun PixelArtTree(
    tree: Tree,
    modifier: Modifier = Modifier,
    isGrowing: Boolean = false,
    onGrowthComplete: () -> Unit = {}
) {
    // Growth stages for pixel art sprites
    val growthStages = remember(tree.treeType) {
        getTreeGrowthStages(tree.treeType)
    }
    
    // Current growth stage index
    var currentStageIndex by remember(tree.id) {
        mutableIntStateOf(
            if (isGrowing) 0 
            else calculateInitialStage(tree)
        )
    }
    
    // Growth animation
    LaunchedEffect(isGrowing, currentStageIndex) {
        if (isGrowing && currentStageIndex < growthStages.size - 1) {
            // Animate through growth stages
            while (currentStageIndex < growthStages.size - 1) {
                delay(500) // 500ms per growth stage
                currentStageIndex++
            }
            onGrowthComplete()
        }
    }
    
    Box(
        modifier = modifier.size(64.dp), // Standard pixel art size
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = growthStages[currentStageIndex]),
            contentDescription = "Pixel art tree - ${tree.treeType.name}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}

/**
 * Get growth stage sprites for each tree type
 * Following KISS principle - simple resource mapping
 */
private fun getTreeGrowthStages(treeType: TreeType): List<Int> {
    return when (treeType) {
        TreeType.OAK -> listOf(
            R.drawable.pixel_seed,
            R.drawable.pixel_sprout,
            R.drawable.pixel_sapling_oak,
            R.drawable.pixel_young_oak,
            R.drawable.pixel_mature_oak
        )
        TreeType.PINE -> listOf(
            R.drawable.pixel_seed,
            R.drawable.pixel_sprout,
            R.drawable.pixel_sapling_pine,
            R.drawable.pixel_young_pine,
            R.drawable.pixel_mature_pine
        )
        TreeType.CHERRY -> listOf(
            R.drawable.pixel_seed,
            R.drawable.pixel_sprout,
            R.drawable.pixel_sapling_cherry,
            R.drawable.pixel_young_cherry,
            R.drawable.pixel_mature_cherry
        )
        TreeType.PALM -> listOf(
            R.drawable.pixel_seed,
            R.drawable.pixel_sprout,
            R.drawable.pixel_sapling_palm,
            R.drawable.pixel_young_palm,
            R.drawable.pixel_mature_palm
        )
        TreeType.SAPLING -> listOf(
            R.drawable.pixel_seed,
            R.drawable.pixel_sprout,
            R.drawable.pixel_sapling_generic
        )
        TreeType.GOLDEN_OAK -> listOf(
            R.drawable.pixel_seed,
            R.drawable.pixel_sprout,
            R.drawable.pixel_sapling_golden,
            R.drawable.pixel_young_golden,
            R.drawable.pixel_mature_golden
        )
        else -> listOf(
            R.drawable.pixel_seed,
            R.drawable.pixel_sprout,
            R.drawable.pixel_sapling_generic,
            R.drawable.pixel_young_generic,
            R.drawable.pixel_mature_generic
        )
    }
}

/**
 * Calculate initial growth stage based on tree data
 */
private fun calculateInitialStage(tree: Tree): Int {
    return when {
        !tree.sessionData.wasCompleted -> 2 // Sapling stage
        tree.treeType.isSpecial -> 4 // Mature stage
        tree.sessionData.streakPosition >= 3 -> 4 // Mature
        tree.sessionData.streakPosition >= 1 -> 3 // Young
        else -> 2 // Sapling
    }
}