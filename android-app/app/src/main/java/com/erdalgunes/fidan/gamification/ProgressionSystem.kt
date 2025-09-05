package com.erdalgunes.fidan.gamification

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow

/**
 * Core progression system with prestige mechanics.
 * Implements idle game progression loops following SOLID principles.
 */
@Serializable
data class PlayerProgress(
    val currency: GameCurrency = GameCurrency(),
    @Contextual val level: PlayerLevel = PlayerLevel.fromExperience(0L),
    val totalLifetimeFP: Long = 0L,
    val totalSessionsCompleted: Long = 0L,
    val currentStreak: Long = 0L,
    val longestStreak: Long = 0L,
    val lastSessionDate: Long = 0L,
    val treeUpgrades: TreeUpgrades = TreeUpgrades(),
    val prestigeLevel: Int = 0,
    val totalPrestiges: Long = 0L,
    val prestigeBonus: Double = 1.0,
    val offlineEarningsMultiplier: Double = 1.0
) {
    
    /**
     * Calculate effective earnings multiplier based on level and prestige.
     */
    val effectiveMultiplier: Double
        get() = PlayerLevel.getLevelMultiplier(level.level) * prestigeBonus
    
    /**
     * Check if player can prestige (requires minimum progress).
     */
    val canPrestige: Boolean
        get() = totalLifetimeFP >= getPrestigeRequirement()
    
    /**
     * Get next prestige requirement.
     */
    fun getPrestigeRequirement(): Long {
        return (10000L * (prestigeLevel + 1).toDouble().pow(1.5)).toLong()
    }
    
    /**
     * Get prestige preview - what player would earn.
     */
    fun getPrestigePreview(): PrestigePreview {
        if (!canPrestige) {
            return PrestigePreview(
                prestigeSeeds = 0L,
                newPrestigeBonus = prestigeBonus,
                nextPrestigeAt = getPrestigeRequirement() - totalLifetimeFP
            )
        }
        
        val calculator = EarningsCalculator()
        val prestigeReward = calculator.calculatePrestigeReward(totalLifetimeFP)
        val newBonus = calculatePrestigeBonus(prestigeLevel + 1, totalPrestiges + 1)
        
        return PrestigePreview(
            prestigeSeeds = prestigeReward.prestigeSeeds,
            newPrestigeBonus = newBonus,
            nextPrestigeAt = 0L
        )
    }
    
    /**
     * Execute prestige - reset progress but keep permanent upgrades.
     */
    fun prestige(): PlayerProgress {
        require(canPrestige) { "Cannot prestige yet - need ${getPrestigeRequirement() - totalLifetimeFP} more FP" }
        
        val prestigeReward = EarningsCalculator().calculatePrestigeReward(totalLifetimeFP)
        val newPrestigeBonus = calculatePrestigeBonus(prestigeLevel + 1, totalPrestiges + 1)
        
        return copy(
            currency = GameCurrency(
                prestigeSeeds = currency.prestigeSeeds + prestigeReward.prestigeSeeds
            ),
            level = PlayerLevel.fromExperience(0L),
            totalLifetimeFP = 0L, // Reset for next prestige cycle
            prestigeLevel = prestigeLevel + 1,
            totalPrestiges = totalPrestiges + 1,
            prestigeBonus = newPrestigeBonus,
            treeUpgrades = treeUpgrades.resetNonPermanent()
        )
    }
    
    /**
     * Add session earnings with all bonuses applied.
     */
    fun addSessionEarnings(baseEarnings: GameCurrency): PlayerProgress {
        val multipliedEarnings = GameCurrency(
            focusPoints = (baseEarnings.focusPoints * effectiveMultiplier).toLong(),
            growthTokens = baseEarnings.growthTokens,
            experiencePoints = baseEarnings.experiencePoints
        )
        
        val newCurrency = currency.earn(multipliedEarnings)
        val newLevel = PlayerLevel.fromExperience(currency.experiencePoints + multipliedEarnings.experiencePoints)
        
        return copy(
            currency = newCurrency,
            level = newLevel,
            totalLifetimeFP = totalLifetimeFP + multipliedEarnings.focusPoints,
            totalSessionsCompleted = totalSessionsCompleted + 1,
            lastSessionDate = System.currentTimeMillis()
        )
    }
    
    /**
     * Update daily streak.
     */
    fun updateStreak(isNewDay: Boolean): PlayerProgress {
        val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000) // Days since epoch
        val lastDay = lastSessionDate / (24 * 60 * 60 * 1000)
        
        val newStreak = when {
            isNewDay && (today - lastDay) == 1L -> currentStreak + 1
            isNewDay && (today - lastDay) > 1L -> 1L // Streak broken
            !isNewDay -> currentStreak // Same day
            else -> 1L
        }
        
        return copy(
            currentStreak = newStreak,
            longestStreak = maxOf(longestStreak, newStreak)
        )
    }
    
    private fun calculatePrestigeBonus(prestigeLevel: Int, totalPrestiges: Long): Double {
        // Each prestige gives 10% bonus, with diminishing returns after 10 prestiges
        val baseBonus = 1.0 + (prestigeLevel * 0.1)
        val diminishingFactor = if (totalPrestiges > 10) {
            1.0 + ((totalPrestiges - 10) * 0.02) // 2% per prestige after 10th
        } else {
            1.0
        }
        
        return baseBonus * diminishingFactor
    }
}

/**
 * Tree upgrade system for visual and mechanical progression.
 * Follows SOLID principles with clear separation of concerns.
 */
@Serializable
data class TreeUpgrades(
    val growthSpeed: Int = 1,
    val maxTreeSize: Int = 3,
    val treeVariety: Int = 1,
    val autoPlanting: Boolean = false,
    val forestExpansion: Int = 1,
    val seasonalEffects: Boolean = false,
    val magicalTrees: Boolean = false,
    val offlineGrowth: Boolean = false
) {
    
    /**
     * Get cost for next upgrade level.
     */
    fun getUpgradeCost(upgradeType: TreeUpgradeType): GameCurrency {
        return when (upgradeType) {
            TreeUpgradeType.GROWTH_SPEED -> GameCurrency(
                growthTokens = (growthSpeed * 10L),
                focusPoints = (growthSpeed * 100L)
            )
            TreeUpgradeType.MAX_SIZE -> GameCurrency(
                growthTokens = (maxTreeSize * 25L),
                focusPoints = (maxTreeSize * 500L)
            )
            TreeUpgradeType.VARIETY -> GameCurrency(
                growthTokens = (treeVariety * 50L),
                focusPoints = (treeVariety * 1000L)
            )
            TreeUpgradeType.AUTO_PLANTING -> GameCurrency(
                growthTokens = 200L,
                prestigeSeeds = 1L
            )
            TreeUpgradeType.FOREST_EXPANSION -> GameCurrency(
                growthTokens = (forestExpansion * 100L),
                focusPoints = (forestExpansion * 2000L)
            )
            TreeUpgradeType.SEASONAL_EFFECTS -> GameCurrency(
                growthTokens = 500L,
                prestigeSeeds = 3L
            )
            TreeUpgradeType.MAGICAL_TREES -> GameCurrency(
                growthTokens = 1000L,
                prestigeSeeds = 5L
            )
            TreeUpgradeType.OFFLINE_GROWTH -> GameCurrency(
                prestigeSeeds = 2L
            )
        }
    }
    
    /**
     * Apply upgrade if affordable.
     */
    fun upgrade(upgradeType: TreeUpgradeType): TreeUpgrades {
        return when (upgradeType) {
            TreeUpgradeType.GROWTH_SPEED -> copy(growthSpeed = growthSpeed + 1)
            TreeUpgradeType.MAX_SIZE -> copy(maxTreeSize = maxTreeSize + 1)
            TreeUpgradeType.VARIETY -> copy(treeVariety = treeVariety + 1)
            TreeUpgradeType.AUTO_PLANTING -> copy(autoPlanting = true)
            TreeUpgradeType.FOREST_EXPANSION -> copy(forestExpansion = forestExpansion + 1)
            TreeUpgradeType.SEASONAL_EFFECTS -> copy(seasonalEffects = true)
            TreeUpgradeType.MAGICAL_TREES -> copy(magicalTrees = true)
            TreeUpgradeType.OFFLINE_GROWTH -> copy(offlineGrowth = true)
        }
    }
    
    /**
     * Can upgrade this type?
     */
    fun canUpgrade(upgradeType: TreeUpgradeType): Boolean {
        return when (upgradeType) {
            TreeUpgradeType.GROWTH_SPEED -> growthSpeed < 10
            TreeUpgradeType.MAX_SIZE -> maxTreeSize < 5
            TreeUpgradeType.VARIETY -> treeVariety < 8
            TreeUpgradeType.AUTO_PLANTING -> !autoPlanting
            TreeUpgradeType.FOREST_EXPANSION -> forestExpansion < 5
            TreeUpgradeType.SEASONAL_EFFECTS -> !seasonalEffects
            TreeUpgradeType.MAGICAL_TREES -> !magicalTrees
            TreeUpgradeType.OFFLINE_GROWTH -> !offlineGrowth
        }
    }
    
    /**
     * Reset non-permanent upgrades for prestige.
     * Permanent upgrades bought with prestige seeds are kept.
     */
    fun resetNonPermanent(): TreeUpgrades {
        return copy(
            growthSpeed = 1,
            maxTreeSize = 3,
            treeVariety = 1,
            forestExpansion = 1,
            // Keep permanent upgrades
            autoPlanting = autoPlanting,
            seasonalEffects = seasonalEffects,
            magicalTrees = magicalTrees,
            offlineGrowth = offlineGrowth
        )
    }
    
    /**
     * Get growth multiplier based on upgrades.
     */
    val growthMultiplier: Double
        get() = growthSpeed * (if (offlineGrowth) 1.5 else 1.0) * (if (magicalTrees) 2.0 else 1.0)
}

/**
 * Types of tree upgrades available.
 */
enum class TreeUpgradeType {
    GROWTH_SPEED,      // Trees grow faster
    MAX_SIZE,          // Trees can grow larger  
    VARIETY,           // More tree types unlock
    AUTO_PLANTING,     // Auto-plant trees (permanent)
    FOREST_EXPANSION,  // Bigger forest area
    SEASONAL_EFFECTS,  // Day/night cycles (permanent)
    MAGICAL_TREES,     // Special tree effects (permanent)
    OFFLINE_GROWTH     // Trees grow while offline (permanent)
}

/**
 * Prestige preview for showing what player would gain.
 */
data class PrestigePreview(
    val prestigeSeeds: Long,
    val newPrestigeBonus: Double,
    val nextPrestigeAt: Long
)

/**
 * Offline progress calculator.
 * Implements idle game core mechanic - progress while away.
 */
class OfflineProgressCalculator {
    
    /**
     * Calculate what player earned while offline.
     */
    fun calculateOfflineProgress(
        playerProgress: PlayerProgress,
        offlineTimeMillis: Long
    ): OfflineProgress {
        
        if (offlineTimeMillis < 60_000L) { // Less than 1 minute
            return OfflineProgress()
        }
        
        val offlineMinutes = offlineTimeMillis / (60 * 1000L)
        val calculator = EarningsCalculator()
        
        // Base offline earnings from last session performance
        val lastSessionEarnings = GameCurrency(focusPoints = max(1L, playerProgress.totalLifetimeFP / playerProgress.totalSessionsCompleted))
        val baseOfflineEarnings = calculator.calculateOfflineEarnings(
            offlineMinutes,
            lastSessionEarnings
        )
        
        // Apply offline multipliers
        val offlineMultiplier = playerProgress.offlineEarningsMultiplier * 
                               (if (playerProgress.treeUpgrades.offlineGrowth) 2.0 else 1.0)
        
        val totalOfflineEarnings = GameCurrency(
            focusPoints = (baseOfflineEarnings.focusPoints * offlineMultiplier).toLong(),
            growthTokens = baseOfflineEarnings.growthTokens
        )
        
        // Calculate trees that would have grown
        val treesGrown = calculateTreeGrowth(playerProgress, offlineMinutes)
        
        return OfflineProgress(
            earnings = totalOfflineEarnings,
            treesGrown = treesGrown,
            timeAwayMillis = offlineTimeMillis
        )
    }
    
    private fun calculateTreeGrowth(playerProgress: PlayerProgress, offlineMinutes: Long): Int {
        if (!playerProgress.treeUpgrades.offlineGrowth) return 0
        
        val growthRate = playerProgress.treeUpgrades.growthMultiplier
        val baseGrowthPerHour = 1.0 // One tree per hour baseline
        
        return floor(offlineMinutes / 60.0 * baseGrowthPerHour * growthRate).toInt()
    }
}

/**
 * Offline progress result.
 */
data class OfflineProgress(
    val earnings: GameCurrency = GameCurrency(),
    val treesGrown: Int = 0,
    val timeAwayMillis: Long = 0L
) {
    val timeAwayFormatted: String
        get() {
            val hours = timeAwayMillis / (60 * 60 * 1000L)
            val minutes = (timeAwayMillis % (60 * 60 * 1000L)) / (60 * 1000L)
            
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "< 1m"
            }
        }
}

/**
 * Daily challenge system for engagement.
 * Follows KISS principle with simple rotating challenges.
 */
data class DailyChallenge(
    val id: String,
    val title: String,
    val description: String,
    val target: Long,
    val progress: Long = 0L,
    val reward: GameCurrency,
    val expiresAt: Long,
    val isCompleted: Boolean = false
) {
    val progressPercentage: Float
        get() = if (target > 0) minOf(100f, (progress.toFloat() / target) * 100f) else 100f
    
    fun updateProgress(newProgress: Long): DailyChallenge {
        val updatedProgress = minOf(target, progress + newProgress)
        return copy(
            progress = updatedProgress,
            isCompleted = updatedProgress >= target
        )
    }
    
    companion object {
        /**
         * Generate daily challenge based on day of year.
         * Ensures consistent challenges that rotate predictably.
         */
        fun generateDailyChallenge(dayOfYear: Int): DailyChallenge {
            val challenges = listOf(
                Triple("focus_30min", "Focus for 30 minutes", 30L),
                Triple("complete_3_sessions", "Complete 3 sessions", 3L),
                Triple("try_hiit", "Try HIIT timer", 1L),
                Triple("long_session", "Complete a 45-minute session", 45L),
                Triple("early_start", "Start before 9 AM", 1L),
                Triple("evening_focus", "Focus after 6 PM", 1L),
                Triple("perfect_sessions", "Complete 5 full sessions", 5L)
            )
            
            val challenge = challenges[dayOfYear % challenges.size]
            val tomorrow = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
            
            val baseReward = GameCurrency(
                focusPoints = 200L,
                growthTokens = 2L,
                experiencePoints = 100L
            )
            
            return DailyChallenge(
                id = challenge.first,
                title = challenge.second,
                description = challenge.second,
                target = challenge.third,
                reward = baseReward,
                expiresAt = tomorrow
            )
        }
    }
}