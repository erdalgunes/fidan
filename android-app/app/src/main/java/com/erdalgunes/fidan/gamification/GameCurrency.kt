package com.erdalgunes.fidan.gamification

import kotlinx.serialization.Serializable
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

/**
 * Core currency system for idle game mechanics.
 * Follows SOLID principles - Single Responsibility for each currency type.
 */
@Serializable
data class GameCurrency(
    val focusPoints: Long = 0L,
    val growthTokens: Long = 0L,
    val prestigeSeeds: Long = 0L,
    val experiencePoints: Long = 0L
) {
    /**
     * Checks if user can afford a cost.
     */
    fun canAfford(cost: GameCurrency): Boolean {
        return focusPoints >= cost.focusPoints &&
                growthTokens >= cost.growthTokens &&
                prestigeSeeds >= cost.prestigeSeeds &&
                experiencePoints >= cost.experiencePoints
    }
    
    /**
     * Subtract cost from current currency.
     */
    fun spend(cost: GameCurrency): GameCurrency {
        require(canAfford(cost)) { "Insufficient currency to spend $cost" }
        
        return copy(
            focusPoints = focusPoints - cost.focusPoints,
            growthTokens = growthTokens - cost.growthTokens,
            prestigeSeeds = prestigeSeeds - cost.prestigeSeeds,
            experiencePoints = experiencePoints - cost.experiencePoints
        )
    }
    
    /**
     * Add earnings to current currency.
     */
    fun earn(earnings: GameCurrency): GameCurrency {
        return copy(
            focusPoints = focusPoints + earnings.focusPoints,
            growthTokens = growthTokens + earnings.growthTokens,
            prestigeSeeds = prestigeSeeds + earnings.prestigeSeeds,
            experiencePoints = experiencePoints + earnings.experiencePoints
        )
    }
    
    companion object {
        fun focusPoints(amount: Long) = GameCurrency(focusPoints = amount)
        fun growthTokens(amount: Long) = GameCurrency(growthTokens = amount)
        fun prestigeSeeds(amount: Long) = GameCurrency(prestigeSeeds = amount)
        fun experiencePoints(amount: Long) = GameCurrency(experiencePoints = amount)
    }
}

/**
 * Calculates earnings based on focus session.
 * Follows KISS principle - simple but effective formulas.
 */
class EarningsCalculator {
    
    companion object {
        // Base rates (can be modified by upgrades)
        private const val BASE_FP_PER_MINUTE = 1L
        private const val BONUS_THRESHOLD_MINUTES = 25 // Pomodoro standard
        private const val BONUS_MULTIPLIER = 1.5
        
        // Growth token conversion rates
        private const val FP_TO_GT_RATIO = 100L
        private const val MIN_SESSION_FOR_GT = 5 * 60 * 1000L // 5 minutes
        
        // Experience points for different activities
        private const val XP_PER_COMPLETED_SESSION = 50L
        private const val XP_PER_ACHIEVEMENT = 100L
    }
    
    /**
     * Calculate earnings from a focus session.
     * Uses hybrid model - linear base + exponential bonus for longer sessions.
     */
    fun calculateSessionEarnings(
        durationMillis: Long,
        wasCompleted: Boolean,
        sessionType: SessionType = SessionType.FOCUS,
        multiplier: Double = 1.0
    ): GameCurrency {
        val minutes = durationMillis / (60 * 1000L)
        
        // Base focus points (linear)
        var focusPoints = (minutes * BASE_FP_PER_MINUTE * multiplier).toLong()
        
        // Bonus for completing full sessions
        if (wasCompleted && minutes >= BONUS_THRESHOLD_MINUTES) {
            val bonusPoints = (focusPoints * (BONUS_MULTIPLIER - 1.0)).toLong()
            focusPoints += bonusPoints
        }
        
        // Session type multipliers
        focusPoints = when (sessionType) {
            SessionType.FOCUS -> focusPoints
            SessionType.HIIT -> (focusPoints * 1.2).toLong() // Slightly higher for HIIT
            SessionType.STOPWATCH -> (focusPoints * 0.8).toLong() // Lower for free-form timing
        }
        
        // Growth tokens for longer sessions
        val growthTokens = if (durationMillis >= MIN_SESSION_FOR_GT) {
            max(1L, focusPoints / FP_TO_GT_RATIO)
        } else 0L
        
        // Experience points
        val experiencePoints = if (wasCompleted) {
            XP_PER_COMPLETED_SESSION + (minutes / 10L) // Bonus XP for longer sessions
        } else {
            (XP_PER_COMPLETED_SESSION * 0.3).toLong() // Partial XP for incomplete sessions
        }
        
        return GameCurrency(
            focusPoints = focusPoints,
            growthTokens = growthTokens,
            experiencePoints = experiencePoints
        )
    }
    
    /**
     * Calculate offline earnings based on last session performance.
     * Follows YAGNI - simple implementation that can be extended.
     */
    fun calculateOfflineEarnings(
        offlineMinutes: Long,
        lastSessionPerformance: GameCurrency,
        maxOfflineHours: Int = 8
    ): GameCurrency {
        // Cap offline time to prevent abuse
        val cappedMinutes = minOf(offlineMinutes, maxOfflineHours * 60L)
        
        // Offline earnings at reduced rate (10% of active rate)
        val offlineRate = 0.1
        val offlineFP = (lastSessionPerformance.focusPoints * offlineRate * (cappedMinutes / 25.0)).toLong()
        
        return GameCurrency(focusPoints = maxOf(1L, offlineFP))
    }
    
    /**
     * Calculate prestige earnings based on total FP accumulated.
     * Uses logarithmic scaling to prevent runaway growth.
     */
    fun calculatePrestigeReward(totalFocusPoints: Long): GameCurrency {
        if (totalFocusPoints < 10000L) return GameCurrency()
        
        // Logarithmic prestige seed calculation
        val prestigeSeeds = floor(ln(totalFocusPoints / 1000.0) / ln(2.0)).toLong()
        
        return GameCurrency(prestigeSeeds = maxOf(1L, prestigeSeeds))
    }
    
    /**
     * Calculate achievement reward.
     */
    fun calculateAchievementReward(achievementTier: AchievementTier): GameCurrency {
        return when (achievementTier) {
            AchievementTier.BRONZE -> GameCurrency(
                focusPoints = 100L,
                experiencePoints = XP_PER_ACHIEVEMENT
            )
            AchievementTier.SILVER -> GameCurrency(
                focusPoints = 500L,
                growthTokens = 5L,
                experiencePoints = XP_PER_ACHIEVEMENT * 2
            )
            AchievementTier.GOLD -> GameCurrency(
                focusPoints = 2000L,
                growthTokens = 20L,
                experiencePoints = XP_PER_ACHIEVEMENT * 5
            )
            AchievementTier.PLATINUM -> GameCurrency(
                focusPoints = 10000L,
                growthTokens = 100L,
                prestigeSeeds = 1L,
                experiencePoints = XP_PER_ACHIEVEMENT * 10
            )
        }
    }
}

/**
 * Session types with different reward modifiers.
 */
enum class SessionType {
    FOCUS,      // Standard pomodoro-style focus session
    STOPWATCH,  // Free-form timing
    HIIT        // High-intensity interval training
}

/**
 * Achievement tiers for reward scaling.
 */
enum class AchievementTier {
    BRONZE, SILVER, GOLD, PLATINUM
}

/**
 * Player level system based on experience points.
 * Follows Open/Closed principle - easy to extend with new levels.
 */
data class PlayerLevel(
    val level: Int,
    val currentXP: Long,
    val xpToNextLevel: Long
) {
    val progress: Float
        get() = if (xpToNextLevel > 0) currentXP.toFloat() / xpToNextLevel else 1f
    
    companion object {
        /**
         * Calculate level from total XP using exponential scaling.
         */
        fun fromExperience(totalXP: Long): PlayerLevel {
            if (totalXP <= 0) return PlayerLevel(1, 0, 100)
            
            // Exponential XP requirements: level^2 * 100
            var level = 1
            var xpRequired = 0L
            
            while (xpRequired < totalXP) {
                level++
                xpRequired += level * level * 100L
            }
            
            val previousLevelXP = xpRequired - (level * level * 100L)
            val currentXP = totalXP - previousLevelXP
            val xpToNext = (level * level * 100L) - currentXP
            
            return PlayerLevel(level - 1, currentXP, xpToNext)
        }
        
        /**
         * Get level multiplier for earnings.
         */
        fun getLevelMultiplier(level: Int): Double {
            return 1.0 + (level - 1) * 0.05 // 5% bonus per level
        }
    }
}