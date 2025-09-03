package com.erdalgunes.fidan.data

import java.util.Date
import java.util.UUID

data class Tree(
    val id: String = UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    val treeType: TreeType,
    val sessionData: SessionData,
    val plantedDate: Date = Date()
)

data class SessionData(
    val taskName: String? = null,
    val durationMillis: Long,
    val completedDate: Date,
    val wasCompleted: Boolean
)

enum class TreeType(
    val displayName: String,
    val emoji: String,
    val color: Long,
    val minHeight: Float,
    val maxHeight: Float
) {
    OAK("Oak Tree", "🌳", 0xFF4CAF50, 80f, 120f),
    PINE("Pine Tree", "🌲", 0xFF2E7D32, 90f, 140f),
    PALM("Palm Tree", "🌴", 0xFF66BB6A, 70f, 110f),
    CHERRY("Cherry Tree", "🌸", 0xFFE91E63, 60f, 100f),
    SAPLING("Sapling", "🌱", 0xFF8BC34A, 30f, 50f);
    
    companion object {
        fun getRandomCompletedTreeType(): TreeType {
            val completedTypes = values().filter { it != SAPLING }
            return completedTypes.random()
        }
        
        fun getTreeTypeForSession(wasCompleted: Boolean): TreeType {
            return if (wasCompleted) getRandomCompletedTreeType() else SAPLING
        }
    }
}

data class ForestState(
    val trees: List<Tree> = emptyList(),
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val isDayTime: Boolean = true
)