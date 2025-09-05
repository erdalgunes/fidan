package com.erdalgunes.fidan.repository

import android.content.Context
import android.content.SharedPreferences
import com.erdalgunes.fidan.common.Result
import com.erdalgunes.fidan.common.safeCall
import com.erdalgunes.fidan.data.SessionData
import com.erdalgunes.fidan.data.Tree
import com.erdalgunes.fidan.data.TreeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import java.util.Calendar
import java.util.Date

/**
 * Repository interface for forest/tree operations.
 * Follows Interface Segregation Principle.
 */
interface ForestRepository {
    suspend fun addTree(sessionData: SessionData): Result<Tree>
    suspend fun getTrees(): Result<List<Tree>>
    fun getTreesFlow(): Flow<Result<List<Tree>>>
    suspend fun clearForest(): Result<Unit>
    suspend fun getForestStats(): Result<ForestStats>
    suspend fun validateForestIntegrity(): Result<ValidationResult>
    suspend fun recoverFromCorruption(): Result<RecoveryResult>
}

/**
 * Forest statistics data class.
 */
data class ForestStats(
    val totalTrees: Int,
    val completedSessions: Int,
    val incompleteSessions: Int,
    val totalFocusTimeMillis: Long,
    val averageSessionDurationMillis: Long,
    val isDayTime: Boolean
) {
    val completionRate: Float
        get() = if (totalTrees > 0) completedSessions.toFloat() / totalTrees else 0f
    
    val totalFocusHours: Float
        get() = totalFocusTimeMillis / (1000f * 60f * 60f)
}

/**
 * Validation result for forest data integrity.
 */
data class ValidationResult(
    val isValid: Boolean,
    val issues: List<ValidationIssue> = emptyList(),
    val canRecover: Boolean = true
)

data class ValidationIssue(
    val type: IssueType,
    val description: String,
    val severity: Severity
) {
    enum class IssueType {
        CORRUPTED_DATA, MISSING_FIELDS, INVALID_DATES, DUPLICATE_ENTRIES, FORMAT_ERROR
    }
    
    enum class Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}

/**
 * Recovery result from corruption handling.
 */
data class RecoveryResult(
    val wasSuccessful: Boolean,
    val recoveredTrees: List<Tree>,
    val lostDataCount: Int,
    val recoveryActions: List<String>
)

/**
 * Default implementation of ForestRepository with robust error handling.
 */
class DefaultForestRepository(
    private val context: Context
) : ForestRepository {
    
    private val trees = mutableListOf<Tree>()
    private val _treesFlow = MutableStateFlow<Result<List<Tree>>>(Result.Loading)
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("forest_data_v2", Context.MODE_PRIVATE)
    private val backupPrefs: SharedPreferences = context.getSharedPreferences("forest_backup", Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val mutex = Mutex() // Thread safety
    
    // Memory fallback for when persistence fails
    private val memoryFallback = mutableListOf<Tree>()
    private var isUsingMemoryFallback = false
    
    init {
        initializeRepository()
    }
    
    private fun initializeRepository() {
        try {
            loadPersistedTrees()
            _treesFlow.value = Result.Success(trees.toList())
        } catch (e: Exception) {
            _treesFlow.value = Result.Error(e, "Failed to initialize forest repository")
            // Try recovery
            attemptDataRecovery()
        }
    }
    
    override suspend fun addTree(sessionData: SessionData): Result<Tree> = safeCall {
        mutex.withLock {
            val position = generateTreePosition()
            val treeType = TreeType.getTreeTypeForSession(sessionData.wasCompleted)
            
            val newTree = Tree(
                x = position.first,
                y = position.second,
                treeType = treeType,
                sessionData = sessionData
            )
            
            // Add to memory first (fail-fast)
            trees.add(newTree)
            
            // Try to persist, but don't fail the operation if persistence fails
            val persistResult = persistTrees()
            if (persistResult.isError) {
                // Fall back to memory-only mode
                isUsingMemoryFallback = true
                memoryFallback.add(newTree)
            }
            
            _treesFlow.value = Result.Success(trees.toList())
            newTree
        }
    }
    
    override suspend fun getTrees(): Result<List<Tree>> = safeCall {
        mutex.withLock {
            if (isUsingMemoryFallback) {
                memoryFallback.toList()
            } else {
                trees.toList()
            }
        }
    }
    
    override fun getTreesFlow(): Flow<Result<List<Tree>>> = _treesFlow.asStateFlow()
    
    override suspend fun clearForest(): Result<Unit> = safeCall {
        mutex.withLock {
            trees.clear()
            memoryFallback.clear()
            isUsingMemoryFallback = false
            
            // Clear persistent storage
            sharedPrefs.edit()
                .remove("trees")
                .remove("last_backup_timestamp")
                .apply()
            
            backupPrefs.edit().clear().apply()
            
            _treesFlow.value = Result.Success(emptyList())
        }
    }
    
    override suspend fun getForestStats(): Result<ForestStats> = safeCall {
        val treeList = if (isUsingMemoryFallback) memoryFallback else trees
        
        val totalTrees = treeList.size
        val completedSessions = treeList.count { it.sessionData.wasCompleted }
        val incompleteSessions = totalTrees - completedSessions
        val totalFocusTime = treeList.sumOf { it.sessionData.durationMillis }
        val averageDuration = if (totalTrees > 0) totalFocusTime / totalTrees else 0L
        
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isDayTime = hour in 6..18
        
        ForestStats(
            totalTrees = totalTrees,
            completedSessions = completedSessions,
            incompleteSessions = incompleteSessions,
            totalFocusTimeMillis = totalFocusTime,
            averageSessionDurationMillis = averageDuration,
            isDayTime = isDayTime
        )
    }
    
    override suspend fun validateForestIntegrity(): Result<ValidationResult> = safeCall {
        mutex.withLock {
            val issues = mutableListOf<ValidationIssue>()
            val treeList = if (isUsingMemoryFallback) memoryFallback else trees
            
            // Check for corrupted data
            treeList.forEach { tree ->
                if (tree.id.isBlank()) {
                    issues.add(ValidationIssue(
                        type = ValidationIssue.IssueType.MISSING_FIELDS,
                        description = "Tree has empty ID",
                        severity = ValidationIssue.Severity.HIGH
                    ))
                }
                
                if (tree.sessionData.durationMillis < 0) {
                    issues.add(ValidationIssue(
                        type = ValidationIssue.IssueType.INVALID_DATES,
                        description = "Tree has negative duration",
                        severity = ValidationIssue.Severity.MEDIUM
                    ))
                }
            }
            
            // Check for duplicates
            val duplicateIds = treeList.groupBy { it.id }
                .filter { it.value.size > 1 }
                .keys
            
            if (duplicateIds.isNotEmpty()) {
                issues.add(ValidationIssue(
                    type = ValidationIssue.IssueType.DUPLICATE_ENTRIES,
                    description = "Found ${duplicateIds.size} duplicate tree IDs",
                    severity = ValidationIssue.Severity.HIGH
                ))
            }
            
            ValidationResult(
                isValid = issues.isEmpty(),
                issues = issues,
                canRecover = issues.all { it.severity != ValidationIssue.Severity.CRITICAL }
            )
        }
    }
    
    override suspend fun recoverFromCorruption(): Result<RecoveryResult> = safeCall {
        mutex.withLock {
            val validTrees = mutableListOf<Tree>()
            val recoveryActions = mutableListOf<String>()
            var lostDataCount = 0
            
            // Attempt to recover from backup first
            val backupRecovery = recoverFromBackup()
            if (backupRecovery.isSuccess) {
                backupRecovery.getOrNull()?.let { backupTrees ->
                    validTrees.addAll(backupTrees)
                    recoveryActions.add("Recovered ${backupTrees.size} trees from backup")
                }
            }
            
            // Clean current data
            val currentTrees = if (isUsingMemoryFallback) memoryFallback else trees
            currentTrees.forEach { tree ->
                if (isTreeValid(tree)) {
                    // Avoid duplicates from backup
                    if (validTrees.none { it.id == tree.id }) {
                        validTrees.add(tree)
                    }
                } else {
                    lostDataCount++
                }
            }
            
            // Remove duplicates based on ID and session data
            val cleanedTrees = validTrees.distinctBy { "${it.id}_${it.sessionData.completedDate.time}" }
            lostDataCount += validTrees.size - cleanedTrees.size
            
            // Update repository state
            trees.clear()
            trees.addAll(cleanedTrees)
            memoryFallback.clear()
            isUsingMemoryFallback = false
            
            // Persist recovered data
            persistTrees()
            _treesFlow.value = Result.Success(trees.toList())
            
            recoveryActions.add("Cleaned and validated ${cleanedTrees.size} trees")
            if (lostDataCount > 0) {
                recoveryActions.add("Lost $lostDataCount corrupted entries")
            }
            
            RecoveryResult(
                wasSuccessful = true,
                recoveredTrees = cleanedTrees,
                lostDataCount = lostDataCount,
                recoveryActions = recoveryActions
            )
        }
    }
    
    private suspend fun persistTrees(): Result<Unit> = safeCall {
        val persistableTrees = trees.map { tree ->
            PersistableTree(
                id = tree.id,
                x = tree.x,
                y = tree.y,
                treeType = tree.treeType.name,
                taskName = tree.sessionData.taskName ?: "Focus Session",
                durationMillis = tree.sessionData.durationMillis,
                completedDateMillis = tree.sessionData.completedDate.time,
                wasCompleted = tree.sessionData.wasCompleted,
                plantedDateMillis = tree.plantedDate.time
            )
        }
        
        val jsonString = json.encodeToString(persistableTrees)
        
        // Main storage
        val mainResult = safeTryPersist(sharedPrefs, "trees", jsonString)
        
        // Create backup every 5 trees or every hour
        val shouldBackup = trees.size % 5 == 0 || shouldCreateTimedBackup()
        if (shouldBackup) {
            safeTryPersist(backupPrefs, "trees_backup", jsonString)
            sharedPrefs.edit().putLong("last_backup_timestamp", System.currentTimeMillis()).apply()
        }
        
        if (mainResult.isError) {
            isUsingMemoryFallback = true
            val error = mainResult as Result.Error
            throw error.exception
        }
    }
    
    private fun safeTryPersist(prefs: SharedPreferences, key: String, value: String): Result<Unit> {
        return try {
            prefs.edit().putString(key, value).apply()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to persist to $key")
        }
    }
    
    private fun loadPersistedTrees() {
        try {
            val jsonString = sharedPrefs.getString("trees", null) ?: return
            val persistableTrees = json.decodeFromString<List<PersistableTree>>(jsonString)
            
            trees.clear()
            trees.addAll(persistableTrees.mapNotNull { persistableTree ->
                try {
                    Tree(
                        id = persistableTree.id,
                        x = persistableTree.x,
                        y = persistableTree.y,
                        treeType = TreeType.valueOf(persistableTree.treeType),
                        sessionData = SessionData(
                            taskName = persistableTree.taskName,
                            durationMillis = persistableTree.durationMillis,
                            completedDate = Date(persistableTree.completedDateMillis),
                            wasCompleted = persistableTree.wasCompleted
                        ),
                        plantedDate = Date(persistableTree.plantedDateMillis)
                    )
                } catch (e: Exception) {
                    // Skip corrupted entries, log for debugging
                    null
                }
            })
        } catch (e: Exception) {
            // Attempt backup recovery
            attemptDataRecovery()
        }
    }
    
    private fun recoverFromBackup(): Result<List<Tree>> {
        return try {
            val jsonString = backupPrefs.getString("trees_backup", null)
                ?: return Result.Error(Exception("No backup found"), "No backup available")
                
            val persistableTrees = json.decodeFromString<List<PersistableTree>>(jsonString)
            
            val recoveredTrees = persistableTrees.mapNotNull { persistableTree ->
                try {
                    Tree(
                        id = persistableTree.id,
                        x = persistableTree.x,
                        y = persistableTree.y,
                        treeType = TreeType.valueOf(persistableTree.treeType),
                        sessionData = SessionData(
                            taskName = persistableTree.taskName,
                            durationMillis = persistableTree.durationMillis,
                            completedDate = Date(persistableTree.completedDateMillis),
                            wasCompleted = persistableTree.wasCompleted
                        ),
                        plantedDate = Date(persistableTree.plantedDateMillis)
                    )
                } catch (e: Exception) {
                    null // Skip corrupted backup entries
                }
            }
            
            Result.Success(recoveredTrees)
        } catch (e: Exception) {
            Result.Error(e, "Backup recovery failed")
        }
    }
    
    private fun attemptDataRecovery() {
        // Try backup first
        val backupResult = recoverFromBackup()
        if (backupResult.isSuccess) {
            backupResult.getOrNull()?.let { backupTrees ->
                trees.addAll(backupTrees)
                _treesFlow.value = Result.Success(trees.toList())
                return
            }
        }
        
        // Fall back to memory-only mode
        isUsingMemoryFallback = true
        _treesFlow.value = Result.Success(emptyList())
    }
    
    private fun isTreeValid(tree: Tree): Boolean {
        return tree.id.isNotBlank() &&
                tree.sessionData.durationMillis >= 0 &&
                tree.sessionData.completedDate.time > 0
    }
    
    private fun generateTreePosition(): Pair<Float, Float> {
        val treeIndex = trees.size
        val gridX = (treeIndex % 6).toFloat() * 100f
        val gridY = (treeIndex / 6).toFloat() * 100f
        return Pair(gridX, gridY)
    }
    
    private fun shouldCreateTimedBackup(): Boolean {
        val lastBackup = sharedPrefs.getLong("last_backup_timestamp", 0)
        val oneHour = 60 * 60 * 1000L
        return System.currentTimeMillis() - lastBackup > oneHour
    }
}

@Serializable
private data class PersistableTree(
    val id: String,
    val x: Float,
    val y: Float,
    val treeType: String,
    val taskName: String,
    val durationMillis: Long,
    val completedDateMillis: Long,
    val wasCompleted: Boolean,
    val plantedDateMillis: Long
)