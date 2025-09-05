package com.erdalgunes.fidan.gamification

import android.content.Context
import android.content.SharedPreferences
import com.erdalgunes.fidan.common.Result
import com.erdalgunes.fidan.common.safeCall
import com.erdalgunes.fidan.data.SessionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.Calendar

/**
 * Central gamification manager following SOLID principles.
 * Coordinates all gamification systems and manages state.
 */
class GamificationManager(
    private val context: Context
) {
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("gamification_data", Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // State management
    private val _playerProgress = MutableStateFlow(loadPlayerProgress())
    val playerProgress: StateFlow<PlayerProgress> = _playerProgress.asStateFlow()
    
    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()
    
    private val _dailyChallenge = MutableStateFlow<DailyChallenge?>(null)
    val dailyChallenge: StateFlow<DailyChallenge?> = _dailyChallenge.asStateFlow()
    
    private val _recentUnlocks = MutableStateFlow<List<Achievement>>(emptyList())
    val recentUnlocks: StateFlow<List<Achievement>> = _recentUnlocks.asStateFlow()
    
    // Components
    private lateinit var achievementTracker: AchievementTracker
    private val offlineCalculator = OfflineProgressCalculator()
    private val earningsCalculator = EarningsCalculator()
    
    init {
        initializeGamificationSystems()
    }
    
    /**
     * Initialize all gamification systems.
     * Follows YAGNI - starts simple, extends as needed.
     */
    private fun initializeGamificationSystems() {
        // Load or create achievements
        val savedAchievements = loadAchievements()
        val allAchievements = if (savedAchievements.isEmpty()) {
            AchievementFactory.createStandardAchievements()
        } else {
            savedAchievements
        }
        
        achievementTracker = AchievementTracker(allAchievements.toMutableList())
        _achievements.value = achievementTracker.getAllAchievements()
        
        // Load or create daily challenge
        _dailyChallenge.value = loadOrCreateDailyChallenge()
        
        // Check for offline progress
        checkOfflineProgress()
    }
    
    /**
     * Process completed focus session and update all gamification systems.
     */
    suspend fun processSessionComplete(sessionData: SessionData): Result<SessionResult> = safeCall {
        val durationMinutes = sessionData.durationMillis / (60 * 1000L)
        val sessionType = when {
            sessionData.taskName?.contains("HIIT") == true -> SessionType.HIIT
            sessionData.taskName?.contains("Stopwatch") == true -> SessionType.STOPWATCH
            else -> SessionType.FOCUS
        }
        
        // Calculate base earnings
        val baseEarnings = earningsCalculator.calculateSessionEarnings(
            durationMillis = sessionData.durationMillis,
            wasCompleted = sessionData.wasCompleted,
            sessionType = sessionType,
            multiplier = _playerProgress.value.effectiveMultiplier
        )
        
        // Update player progress
        val currentProgress = _playerProgress.value
        val isNewDay = isNewDay(currentProgress.lastSessionDate)
        val updatedProgress = currentProgress
            .addSessionEarnings(baseEarnings)
            .updateStreak(isNewDay)
        
        _playerProgress.value = updatedProgress
        
        // Track achievements
        val newAchievements = mutableListOf<Achievement>()
        newAchievements.addAll(
            achievementTracker.trackSessionComplete(durationMinutes, sessionType)
        )
        newAchievements.addAll(
            achievementTracker.trackDailyStreak(updatedProgress.currentStreak)
        )
        newAchievements.addAll(
            achievementTracker.trackLevelProgress(updatedProgress.level.level)
        )
        
        // Update achievements state
        _achievements.value = achievementTracker.getAllAchievements()
        if (newAchievements.isNotEmpty()) {
            _recentUnlocks.value = newAchievements
        }
        
        // Update daily challenge
        updateDailyChallenge(sessionData, sessionType)
        
        // Save state
        savePlayerProgress(updatedProgress)
        saveAchievements(achievementTracker.getAllAchievements(includeHidden = true))
        
        // Calculate total rewards including achievement rewards
        val achievementRewards = newAchievements.map { it.getReward() }
            .fold(GameCurrency()) { acc, reward -> acc.earn(reward) }
        
        SessionResult(
            baseEarnings = baseEarnings,
            achievementRewards = achievementRewards,
            newAchievements = newAchievements,
            newLevel = if (updatedProgress.level.level > currentProgress.level.level) {
                updatedProgress.level.level
            } else null,
            streakUpdated = updatedProgress.currentStreak > currentProgress.currentStreak
        )
    }
    
    /**
     * Purchase upgrade if player can afford it.
     */
    suspend fun purchaseUpgrade(upgradeType: TreeUpgradeType): Result<Boolean> = safeCall {
        val currentProgress = _playerProgress.value
        val cost = currentProgress.treeUpgrades.getUpgradeCost(upgradeType)
        
        if (!currentProgress.currency.canAfford(cost) || 
            !currentProgress.treeUpgrades.canUpgrade(upgradeType)) {
            return@safeCall false
        }
        
        val updatedProgress = currentProgress.copy(
            currency = currentProgress.currency.spend(cost),
            treeUpgrades = currentProgress.treeUpgrades.upgrade(upgradeType)
        )
        
        _playerProgress.value = updatedProgress
        savePlayerProgress(updatedProgress)
        
        true
    }
    
    /**
     * Execute prestige if conditions are met.
     */
    suspend fun executePrestige(): Result<PrestigeResult> = safeCall {
        val currentProgress = _playerProgress.value
        
        if (!currentProgress.canPrestige) {
            throw IllegalStateException("Cannot prestige yet")
        }
        
        val preview = currentProgress.getPrestigePreview()
        val prestigedProgress = currentProgress.prestige()
        
        _playerProgress.value = prestigedProgress
        savePlayerProgress(prestigedProgress)
        
        PrestigeResult(
            prestigeSeedsGained = preview.prestigeSeeds,
            newPrestigeLevel = prestigedProgress.prestigeLevel,
            newPrestigeBonus = prestigedProgress.prestigeBonus,
            resetStats = ProgressResetStats(
                lifetimeFPReset = currentProgress.totalLifetimeFP,
                levelReset = currentProgress.level.level,
                currencyReset = currentProgress.currency
            )
        )
    }
    
    /**
     * Clear recent achievement notifications.
     */
    fun clearRecentUnlocks() {
        _recentUnlocks.value = emptyList()
    }
    
    /**
     * Get comprehensive player statistics.
     */
    fun getPlayerStats(): PlayerStats {
        val progress = _playerProgress.value
        val achievementStats = achievementTracker.getStats()
        
        return PlayerStats(
            level = progress.level,
            currency = progress.currency,
            totalLifetimeFP = progress.totalLifetimeFP,
            totalSessions = progress.totalSessionsCompleted,
            currentStreak = progress.currentStreak,
            longestStreak = progress.longestStreak,
            prestigeLevel = progress.prestigeLevel,
            prestigeBonus = progress.prestigeBonus,
            achievementStats = achievementStats,
            canPrestige = progress.canPrestige,
            prestigePreview = if (progress.canPrestige) progress.getPrestigePreview() else null
        )
    }
    
    private fun checkOfflineProgress() {
        val lastSessionTime = _playerProgress.value.lastSessionDate
        if (lastSessionTime == 0L) return
        
        val offlineTime = System.currentTimeMillis() - lastSessionTime
        if (offlineTime < 60_000L) return // Less than 1 minute
        
        val offlineProgress = offlineCalculator.calculateOfflineProgress(
            _playerProgress.value,
            offlineTime
        )
        
        if (offlineProgress.earnings.focusPoints > 0) {
            val updatedProgress = _playerProgress.value.copy(
                currency = _playerProgress.value.currency.earn(offlineProgress.earnings)
            )
            _playerProgress.value = updatedProgress
            savePlayerProgress(updatedProgress)
            
            // Could show offline progress popup here
        }
    }
    
    private fun updateDailyChallenge(sessionData: SessionData, sessionType: SessionType) {
        val currentChallenge = _dailyChallenge.value ?: return
        if (currentChallenge.isCompleted || System.currentTimeMillis() > currentChallenge.expiresAt) {
            return
        }
        
        val updatedChallenge = when (currentChallenge.id) {
            "focus_30min" -> {
                val minutes = sessionData.durationMillis / (60 * 1000L)
                currentChallenge.updateProgress(minutes)
            }
            "complete_3_sessions", "complete_5_sessions" -> {
                if (sessionData.wasCompleted) {
                    currentChallenge.updateProgress(1L)
                } else currentChallenge
            }
            "try_hiit" -> {
                if (sessionType == SessionType.HIIT) {
                    currentChallenge.updateProgress(1L)
                } else currentChallenge
            }
            "long_session" -> {
                val minutes = sessionData.durationMillis / (60 * 1000L)
                if (minutes >= 45) {
                    currentChallenge.updateProgress(1L)
                } else currentChallenge
            }
            else -> currentChallenge
        }
        
        _dailyChallenge.value = updatedChallenge
        saveDailyChallenge(updatedChallenge)
        
        // Award challenge completion reward
        if (updatedChallenge.isCompleted && !currentChallenge.isCompleted) {
            val newProgress = _playerProgress.value.copy(
                currency = _playerProgress.value.currency.earn(updatedChallenge.reward)
            )
            _playerProgress.value = newProgress
            savePlayerProgress(newProgress)
        }
    }
    
    private fun loadOrCreateDailyChallenge(): DailyChallenge {
        val savedChallenge = sharedPrefs.getString("daily_challenge", null)
        if (savedChallenge != null) {
            try {
                val challenge = json.decodeFromString<DailyChallenge>(savedChallenge)
                // Check if challenge is still valid
                if (System.currentTimeMillis() < challenge.expiresAt) {
                    return challenge
                }
            } catch (e: Exception) {
                // Ignore and create new challenge
            }
        }
        
        // Create new daily challenge
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val newChallenge = DailyChallenge.generateDailyChallenge(dayOfYear)
        saveDailyChallenge(newChallenge)
        return newChallenge
    }
    
    private fun isNewDay(lastSessionTime: Long): Boolean {
        if (lastSessionTime == 0L) return true
        
        val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000)
        val lastDay = lastSessionTime / (24 * 60 * 60 * 1000)
        return today > lastDay
    }
    
    // Persistence methods
    private fun loadPlayerProgress(): PlayerProgress {
        val saved = sharedPrefs.getString("player_progress", null)
        return if (saved != null) {
            try {
                json.decodeFromString<PlayerProgress>(saved)
            } catch (e: Exception) {
                PlayerProgress() // Default if corrupted
            }
        } else {
            PlayerProgress()
        }
    }
    
    private fun savePlayerProgress(progress: PlayerProgress) {
        try {
            val jsonString = json.encodeToString(progress)
            sharedPrefs.edit().putString("player_progress", jsonString).apply()
        } catch (e: Exception) {
            // Handle save failure gracefully
        }
    }
    
    private fun loadAchievements(): List<Achievement> {
        val saved = sharedPrefs.getString("achievements", null)
        return if (saved != null) {
            try {
                json.decodeFromString<List<Achievement>>(saved)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    private fun saveAchievements(achievements: List<Achievement>) {
        try {
            val jsonString = json.encodeToString(achievements)
            sharedPrefs.edit().putString("achievements", jsonString).apply()
        } catch (e: Exception) {
            // Handle save failure gracefully
        }
    }
    
    private fun saveDailyChallenge(challenge: DailyChallenge) {
        try {
            val jsonString = json.encodeToString(challenge)
            sharedPrefs.edit().putString("daily_challenge", jsonString).apply()
        } catch (e: Exception) {
            // Handle save failure gracefully  
        }
    }
}

/**
 * Result of processing a completed session.
 */
data class SessionResult(
    val baseEarnings: GameCurrency,
    val achievementRewards: GameCurrency,
    val newAchievements: List<Achievement>,
    val newLevel: Int? = null,
    val streakUpdated: Boolean = false
) {
    val totalEarnings: GameCurrency
        get() = baseEarnings.earn(achievementRewards)
}

/**
 * Result of prestige operation.
 */
data class PrestigeResult(
    val prestigeSeedsGained: Long,
    val newPrestigeLevel: Int,
    val newPrestigeBonus: Double,
    val resetStats: ProgressResetStats
)

/**
 * Stats that were reset during prestige.
 */
data class ProgressResetStats(
    val lifetimeFPReset: Long,
    val levelReset: Int,
    val currencyReset: GameCurrency
)

/**
 * Comprehensive player statistics.
 */
data class PlayerStats(
    val level: PlayerLevel,
    val currency: GameCurrency,
    val totalLifetimeFP: Long,
    val totalSessions: Long,
    val currentStreak: Long,
    val longestStreak: Long,
    val prestigeLevel: Int,
    val prestigeBonus: Double,
    val achievementStats: AchievementStats,
    val canPrestige: Boolean,
    val prestigePreview: PrestigePreview?
)