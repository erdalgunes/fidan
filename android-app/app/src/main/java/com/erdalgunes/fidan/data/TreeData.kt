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
    val wasCompleted: Boolean,
    val streakPosition: Int = 0, // Position in current streak (0 = not part of streak)
    val wasPerfectFocus: Boolean = false // True if user didn't interrupt the session
)

enum class TreeType(
    val displayName: String,
    val emoji: String,
    val color: Long,
    val minHeight: Float,
    val maxHeight: Float,
    val isSpecial: Boolean = false
) {
    OAK("Oak Tree", "🌳", 0xFF4CAF50, 80f, 120f),
    PINE("Pine Tree", "🌲", 0xFF2E7D32, 90f, 140f),
    PALM("Palm Tree", "🌴", 0xFF66BB6A, 70f, 110f),
    CHERRY("Cherry Tree", "🌸", 0xFFE91E63, 60f, 100f),
    SAPLING("Sapling", "🌱", 0xFF8BC34A, 30f, 50f),
    // Special streak reward trees
    GOLDEN_OAK("Golden Oak", "🏆", 0xFFFFD700, 120f, 160f, true),
    CRYSTAL_TREE("Crystal Tree", "💎", 0xFF9C27B0, 100f, 140f, true),
    ANCIENT_TREE("Ancient Tree", "🌟", 0xFFFF9800, 140f, 180f, true);
    
    companion object {
        fun getRandomCompletedTreeType(): TreeType {
            val completedTypes = values().filter { it != SAPLING && !it.isSpecial }
            return completedTypes.random()
        }
        
        fun getTreeTypeForSession(
            wasCompleted: Boolean, 
            streakPosition: Int = 0, 
            wasPerfectFocus: Boolean = false
        ): TreeType {
            return when {
                !wasCompleted -> SAPLING
                streakPosition >= 10 && wasPerfectFocus -> ANCIENT_TREE
                streakPosition >= 7 -> CRYSTAL_TREE
                streakPosition >= 5 -> GOLDEN_OAK
                else -> getRandomCompletedTreeType()
            }
        }
        
        fun getSpecialTrees(): List<TreeType> {
            return values().filter { it.isSpecial }
        }
    }
}

data class ForestState(
    val trees: List<Tree> = emptyList(),
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val isDayTime: Boolean = true,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletedSessions: Int = 0
)