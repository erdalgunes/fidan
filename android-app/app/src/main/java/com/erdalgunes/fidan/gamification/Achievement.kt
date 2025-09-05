package com.erdalgunes.fidan.gamification

import kotlinx.serialization.Serializable
import java.util.Date

/**
 * Achievement system for gamification.
 * Follows SOLID principles and provides extensible achievement types.
 */
@Serializable
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val tier: AchievementTier,
    val category: AchievementCategory,
    val icon: String,
    val isUnlocked: Boolean = false,
    val progress: Long = 0L,
    val target: Long,
    val unlockedDate: Long? = null,
    val isHidden: Boolean = false // Secret achievements
) {
    val progressPercentage: Float
        get() = if (target > 0) minOf(100f, (progress.toFloat() / target) * 100f) else 100f
    
    val isCompleted: Boolean
        get() = progress >= target
    
    fun updateProgress(newProgress: Long): Achievement {
        return copy(
            progress = maxOf(progress, newProgress),
            isUnlocked = newProgress >= target,
            unlockedDate = if (newProgress >= target && unlockedDate == null) {
                System.currentTimeMillis()
            } else unlockedDate
        )
    }
    
    /**
     * Get reward for completing this achievement.
     */
    fun getReward(): GameCurrency {
        return EarningsCalculator().calculateAchievementReward(tier)
    }
}

/**
 * Achievement categories for organization.
 */
enum class AchievementCategory {
    FOCUS_TIME,     // Time-based achievements
    CONSISTENCY,    // Streak-based achievements  
    EFFICIENCY,     // Completion rate achievements
    EXPLORATION,    // Feature usage achievements
    MASTERY,        // Advanced skill achievements
    SOCIAL,         // Community/sharing achievements
    SPECIAL         // Limited-time or secret achievements
}

/**
 * Factory for creating standard achievements.
 * Follows Factory pattern and DRY principle.
 */
object AchievementFactory {
    
    /**
     * Create all standard achievements.
     * Follows YAGNI - start with essential achievements, extend as needed.
     */
    fun createStandardAchievements(): List<Achievement> {
        return buildList {
            // Focus Time Achievements
            addAll(createFocusTimeAchievements())
            
            // Consistency Achievements  
            addAll(createConsistencyAchievements())
            
            // Efficiency Achievements
            addAll(createEfficiencyAchievements())
            
            // Exploration Achievements
            addAll(createExplorationAchievements())
            
            // Mastery Achievements
            addAll(createMasteryAchievements())
            
            // Special Achievements
            addAll(createSpecialAchievements())
        }
    }
    
    private fun createFocusTimeAchievements(): List<Achievement> {
        return listOf(
            // Minutes focused
            Achievement(
                id = "focus_first_session",
                title = "First Steps",
                description = "Complete your first focus session",
                tier = AchievementTier.BRONZE,
                category = AchievementCategory.FOCUS_TIME,
                icon = "🌱",
                target = 1L
            ),
            Achievement(
                id = "focus_1_hour", 
                title = "Hour of Power",
                description = "Focus for a total of 1 hour",
                tier = AchievementTier.BRONZE,
                category = AchievementCategory.FOCUS_TIME,
                icon = "⏰",
                target = 60L // minutes
            ),
            Achievement(
                id = "focus_10_hours",
                title = "Dedicated Learner", 
                description = "Focus for a total of 10 hours",
                tier = AchievementTier.SILVER,
                category = AchievementCategory.FOCUS_TIME,
                icon = "📚",
                target = 600L
            ),
            Achievement(
                id = "focus_100_hours",
                title = "Focus Master",
                description = "Focus for a total of 100 hours",
                tier = AchievementTier.GOLD,
                category = AchievementCategory.FOCUS_TIME,
                icon = "🧠",
                target = 6000L
            ),
            Achievement(
                id = "focus_1000_hours",
                title = "Zen Master",
                description = "Focus for a total of 1000 hours", 
                tier = AchievementTier.PLATINUM,
                category = AchievementCategory.FOCUS_TIME,
                icon = "🧘",
                target = 60000L
            )
        )
    }
    
    private fun createConsistencyAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "streak_3_days",
                title = "Getting Started",
                description = "Maintain a 3-day focus streak",
                tier = AchievementTier.BRONZE,
                category = AchievementCategory.CONSISTENCY,
                icon = "🔥",
                target = 3L
            ),
            Achievement(
                id = "streak_7_days",
                title = "Week Warrior",
                description = "Maintain a 7-day focus streak",
                tier = AchievementTier.SILVER,
                category = AchievementCategory.CONSISTENCY,
                icon = "📅",
                target = 7L
            ),
            Achievement(
                id = "streak_30_days",
                title = "Month Master",
                description = "Maintain a 30-day focus streak",
                tier = AchievementTier.GOLD,
                category = AchievementCategory.CONSISTENCY,
                icon = "🗓️",
                target = 30L
            ),
            Achievement(
                id = "streak_100_days",
                title = "Unstoppable",
                description = "Maintain a 100-day focus streak",
                tier = AchievementTier.PLATINUM,
                category = AchievementCategory.CONSISTENCY,
                icon = "⚡",
                target = 100L
            )
        )
    }
    
    private fun createEfficiencyAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "complete_10_sessions",
                title = "Finisher",
                description = "Complete 10 focus sessions",
                tier = AchievementTier.BRONZE,
                category = AchievementCategory.EFFICIENCY,
                icon = "✅",
                target = 10L
            ),
            Achievement(
                id = "complete_100_sessions",
                title = "Completion Expert",
                description = "Complete 100 focus sessions", 
                tier = AchievementTier.SILVER,
                category = AchievementCategory.EFFICIENCY,
                icon = "🎯",
                target = 100L
            ),
            Achievement(
                id = "perfect_day",
                title = "Perfect Day",
                description = "Complete all planned sessions in a day",
                tier = AchievementTier.GOLD,
                category = AchievementCategory.EFFICIENCY,
                icon = "💯",
                target = 1L
            )
        )
    }
    
    private fun createExplorationAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "try_hiit",
                title = "HIIT Explorer",
                description = "Try the HIIT timer",
                tier = AchievementTier.BRONZE,
                category = AchievementCategory.EXPLORATION,
                icon = "🏃",
                target = 1L
            ),
            Achievement(
                id = "try_stopwatch",
                title = "Time Keeper",
                description = "Use the stopwatch feature",
                tier = AchievementTier.BRONZE,
                category = AchievementCategory.EXPLORATION,
                icon = "⏱️",
                target = 1L
            ),
            Achievement(
                id = "customize_forest",
                title = "Forest Designer",
                description = "Customize your forest view",
                tier = AchievementTier.SILVER,
                category = AchievementCategory.EXPLORATION,
                icon = "🎨",
                target = 1L
            )
        )
    }
    
    private fun createMasteryAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "level_10",
                title = "Rising Star",
                description = "Reach level 10",
                tier = AchievementTier.SILVER,
                category = AchievementCategory.MASTERY,
                icon = "⭐",
                target = 10L
            ),
            Achievement(
                id = "level_25",
                title = "Expert Focuser",
                description = "Reach level 25",
                tier = AchievementTier.GOLD,
                category = AchievementCategory.MASTERY,
                icon = "🏆",
                target = 25L
            ),
            Achievement(
                id = "level_50",
                title = "Legendary Focus",
                description = "Reach level 50",
                tier = AchievementTier.PLATINUM,
                category = AchievementCategory.MASTERY,
                icon = "👑",
                target = 50L
            )
        )
    }
    
    private fun createSpecialAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = "midnight_session",
                title = "Night Owl",
                description = "Complete a session after midnight",
                tier = AchievementTier.SILVER,
                category = AchievementCategory.SPECIAL,
                icon = "🦉",
                target = 1L,
                isHidden = true
            ),
            Achievement(
                id = "early_bird",
                title = "Early Bird",
                description = "Complete a session before 6 AM",
                tier = AchievementTier.SILVER,
                category = AchievementCategory.SPECIAL,
                icon = "🐦",
                target = 1L,
                isHidden = true
            ),
            Achievement(
                id = "marathon_session",
                title = "Marathon Focus",
                description = "Complete a 4-hour focus session",
                tier = AchievementTier.PLATINUM,
                category = AchievementCategory.SPECIAL,
                icon = "🏃‍♂️",
                target = 1L,
                isHidden = true
            )
        )
    }
}

/**
 * Achievement tracker that monitors user activities.
 * Follows Observer pattern to track multiple achievement types.
 */
class AchievementTracker(
    private var achievements: MutableList<Achievement>
) {
    
    /**
     * Track focus session completion.
     */
    fun trackSessionComplete(durationMinutes: Long, sessionType: SessionType): List<Achievement> {
        val unlockedAchievements = mutableListOf<Achievement>()
        
        // Update focus time achievements
        achievements.filter { it.category == AchievementCategory.FOCUS_TIME && !it.isUnlocked }
            .forEach { achievement ->
                val updated = achievement.updateProgress(achievement.progress + durationMinutes)
                if (!achievement.isUnlocked && updated.isUnlocked) {
                    unlockedAchievements.add(updated)
                }
                achievements[achievements.indexOf(achievement)] = updated
            }
        
        // Update completion achievements
        achievements.filter { it.id.startsWith("complete_") && !it.isUnlocked }
            .forEach { achievement ->
                val updated = achievement.updateProgress(achievement.progress + 1)
                if (!achievement.isUnlocked && updated.isUnlocked) {
                    unlockedAchievements.add(updated)
                }
                achievements[achievements.indexOf(achievement)] = updated
            }
        
        // Track first session
        if (achievements.find { it.id == "focus_first_session" }?.progress == 0L) {
            val firstSession = achievements.find { it.id == "focus_first_session" }!!
            val updated = firstSession.updateProgress(1L)
            achievements[achievements.indexOf(firstSession)] = updated
            if (updated.isUnlocked) {
                unlockedAchievements.add(updated)
            }
        }
        
        // Track session type exploration
        when (sessionType) {
            SessionType.HIIT -> trackExploration("try_hiit", unlockedAchievements)
            SessionType.STOPWATCH -> trackExploration("try_stopwatch", unlockedAchievements)
            else -> { /* Focus type is default */ }
        }
        
        // Track special time-based achievements
        trackSpecialTimeAchievements(unlockedAchievements)
        
        return unlockedAchievements
    }
    
    /**
     * Track daily streak progress.
     */
    fun trackDailyStreak(currentStreak: Long): List<Achievement> {
        val unlockedAchievements = mutableListOf<Achievement>()
        
        achievements.filter { it.category == AchievementCategory.CONSISTENCY && !it.isUnlocked }
            .forEach { achievement ->
                val updated = achievement.updateProgress(currentStreak)
                if (!achievement.isUnlocked && updated.isUnlocked) {
                    unlockedAchievements.add(updated)
                }
                achievements[achievements.indexOf(achievement)] = updated
            }
        
        return unlockedAchievements
    }
    
    /**
     * Track level progress.
     */
    fun trackLevelProgress(currentLevel: Int): List<Achievement> {
        val unlockedAchievements = mutableListOf<Achievement>()
        
        achievements.filter { it.id.startsWith("level_") && !it.isUnlocked }
            .forEach { achievement ->
                val updated = achievement.updateProgress(currentLevel.toLong())
                if (!achievement.isUnlocked && updated.isUnlocked) {
                    unlockedAchievements.add(updated)
                }
                achievements[achievements.indexOf(achievement)] = updated
            }
        
        return unlockedAchievements
    }
    
    private fun trackExploration(achievementId: String, unlockedList: MutableList<Achievement>) {
        val achievement = achievements.find { it.id == achievementId }
        if (achievement != null && !achievement.isUnlocked) {
            val updated = achievement.updateProgress(1L)
            achievements[achievements.indexOf(achievement)] = updated
            if (updated.isUnlocked) {
                unlockedList.add(updated)
            }
        }
    }
    
    private fun trackSpecialTimeAchievements(unlockedList: MutableList<Achievement>) {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        
        when {
            currentHour >= 0 && currentHour < 6 -> trackExploration("early_bird", unlockedList)
            currentHour >= 23 || currentHour < 1 -> trackExploration("midnight_session", unlockedList)
        }
    }
    
    /**
     * Get all achievements, filtering hidden ones if not unlocked.
     */
    fun getAllAchievements(includeHidden: Boolean = false): List<Achievement> {
        return if (includeHidden) {
            achievements
        } else {
            achievements.filter { !it.isHidden || it.isUnlocked }
        }
    }
    
    /**
     * Get achievements by category.
     */
    fun getAchievementsByCategory(category: AchievementCategory): List<Achievement> {
        return achievements.filter { it.category == category }
    }
    
    /**
     * Get completion statistics.
     */
    fun getStats(): AchievementStats {
        val total = achievements.size
        val unlocked = achievements.count { it.isUnlocked }
        val totalRewards = achievements.filter { it.isUnlocked }
            .map { it.getReward() }
            .fold(GameCurrency()) { acc, reward -> acc.earn(reward) }
        
        return AchievementStats(
            totalAchievements = total,
            unlockedAchievements = unlocked,
            completionPercentage = (unlocked.toFloat() / total) * 100f,
            totalRewardsEarned = totalRewards
        )
    }
}

/**
 * Achievement statistics data class.
 */
data class AchievementStats(
    val totalAchievements: Int,
    val unlockedAchievements: Int, 
    val completionPercentage: Float,
    val totalRewardsEarned: GameCurrency
)