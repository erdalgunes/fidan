package com.erdalgunes.fidan.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.erdalgunes.fidan.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class ForestService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "ForestService"
        private const val PREFS_NAME = "forest_state"
        private const val KEY_TREES = "trees"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_LONGEST_STREAK = "longest_streak"
        private const val KEY_TOTAL_COMPLETED = "total_completed"
        private const val KEY_IS_DAY_TIME = "is_day_time"
        private const val KEY_ACTIVE_TASKS = "active_tasks"
        
        // Maintenance timing constants (in milliseconds)
        private const val WATERING_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
        private const val WEEDING_INTERVAL = 36 * 60 * 60 * 1000L  // 36 hours  
        private const val PEST_INTERVAL = 48 * 60 * 60 * 1000L     // 48 hours
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _forestState = MutableStateFlow(loadPersistedState())
    val forestState: StateFlow<ForestState> = _forestState.asStateFlow()
    
    init {
        // Initialize with demo trees if forest is empty (for testing maintenance system)
        if (_forestState.value.trees.isEmpty()) {
            initializeDemoTrees()
        }
    }
    
    fun addTree(sessionData: SessionData) {
        val currentState = _forestState.value
        
        // Update streak tracking
        val newStreak = if (sessionData.wasCompleted) {
            currentState.currentStreak + 1
        } else {
            0 // Reset streak on failed session
        }
        
        val newLongestStreak = maxOf(currentState.longestStreak, newStreak)
        val newTotalCompleted = if (sessionData.wasCompleted) {
            currentState.totalCompletedSessions + 1
        } else {
            currentState.totalCompletedSessions
        }
        
        // Calculate position based on current number of trees (grid layout)
        val treeIndex = currentState.trees.size
        val x = (treeIndex % 6).toFloat() * 100f // 6 columns
        val y = (treeIndex / 6).toFloat() * 100f
        
        // Enhanced session data with streak information
        val enhancedSessionData = sessionData.copy(
            streakPosition = newStreak,
            wasPerfectFocus = sessionData.wasCompleted && !sessionData.taskName.isNullOrEmpty()
        )
        
        // Use enhanced tree selection based on streak
        val treeType = TreeType.getTreeTypeForSession(
            wasCompleted = sessionData.wasCompleted,
            streakPosition = newStreak,
            wasPerfectFocus = enhancedSessionData.wasPerfectFocus
        )
        
        val newTree = Tree(
            id = UUID.randomUUID().toString(),
            x = x,
            y = y,
            treeType = treeType,
            sessionData = enhancedSessionData
        )
        
        val updatedTrees = currentState.trees + newTree
        
        val newState = currentState.copy(
            trees = updatedTrees,
            currentStreak = newStreak,
            longestStreak = newLongestStreak,
            totalCompletedSessions = newTotalCompleted
        )
        
        _forestState.value = newState
        saveState(newState)
    }
    
    fun updateDayNightCycle() {
        val newState = _forestState.value.copy(
            isDayTime = !_forestState.value.isDayTime
        )
        _forestState.value = newState
        saveState(newState)
    }
    
    fun getCompletedTreesCount(): Int {
        return _forestState.value.trees.count { it.sessionData.wasCompleted }
    }
    
    fun getIncompleteTreesCount(): Int {
        return _forestState.value.trees.count { !it.sessionData.wasCompleted }
    }
    
    fun getTotalFocusTime(): Long {
        return _forestState.value.trees.sumOf { tree ->
            if (tree.sessionData.wasCompleted) {
                25 * 60 * 1000L // 25 minutes for completed sessions
            } else {
                tree.sessionData.durationMillis
            }
        }
    }
    
    fun getCompletionRate(): Double {
        val totalSessions = _forestState.value.trees.size
        val completedSessions = getCompletedTreesCount()
        return if (totalSessions > 0) {
            (completedSessions.toDouble() / totalSessions.toDouble()) * 100
        } else {
            0.0
        }
    }
    
    fun getCurrentStreak(): Int {
        return _forestState.value.currentStreak
    }
    
    fun getLongestStreak(): Int {
        return _forestState.value.longestStreak
    }
    
    fun getSpecialTreesCount(): Int {
        return _forestState.value.trees.count { it.treeType.isSpecial }
    }
    
    fun updateMaintenanceNeeds() {
        val currentState = _forestState.value
        val updatedTrees = currentState.trees.map { tree ->
            updateTreeMaintenance(tree)
        }
        
        val newTasks = generateMaintenanceTasks(updatedTrees)
        
        val newState = currentState.copy(
            trees = updatedTrees,
            activeTasks = newTasks
        )
        _forestState.value = newState
        saveState(newState)
    }
    
    private fun updateTreeMaintenance(tree: Tree): Tree {
        if (!tree.sessionData.wasCompleted) return tree // Only maintain healthy trees
        
        val now = Date()
        val maintenance = tree.maintenanceState
        
        val needsWatering = (now.time - maintenance.lastWatered.time) > WATERING_INTERVAL
        val hasWeeds = (now.time - maintenance.lastWeeded.time) > WEEDING_INTERVAL
        val hasPests = (now.time - maintenance.lastPestControl.time) > PEST_INTERVAL
        
        // Calculate health based on maintenance needs
        var healthLevel = 1.0f
        if (needsWatering) healthLevel -= 0.3f
        if (hasWeeds) healthLevel -= 0.2f
        if (hasPests) healthLevel -= 0.2f
        healthLevel = healthLevel.coerceAtLeast(0.1f) // Never completely dead
        
        return tree.copy(
            maintenanceState = maintenance.copy(
                needsWatering = needsWatering,
                hasWeeds = hasWeeds,
                hasPests = hasPests,
                healthLevel = healthLevel
            )
        )
    }
    
    private fun generateMaintenanceTasks(trees: List<Tree>): List<ActiveMaintenanceTask> {
        val tasks = mutableListOf<ActiveMaintenanceTask>()
        
        trees.forEach { tree ->
            val maintenance = tree.maintenanceState
            
            if (maintenance.needsWatering) {
                tasks.add(ActiveMaintenanceTask(
                    treeId = tree.id,
                    task = MaintenanceTask.WATERING,
                    urgency = if (maintenance.healthLevel < 0.5f) 1.0f else 0.7f
                ))
            }
            
            if (maintenance.hasWeeds) {
                tasks.add(ActiveMaintenanceTask(
                    treeId = tree.id,
                    task = MaintenanceTask.WEEDING,
                    urgency = 0.5f
                ))
            }
            
            if (maintenance.hasPests) {
                tasks.add(ActiveMaintenanceTask(
                    treeId = tree.id,
                    task = MaintenanceTask.PEST_CONTROL,
                    urgency = 0.8f
                ))
            }
        }
        
        return tasks.sortedByDescending { it.urgency }
    }
    
    fun completeMaintenanceTask(taskId: String, treeId: String, task: MaintenanceTask) {
        val currentState = _forestState.value
        val now = Date()
        
        val updatedTrees = currentState.trees.map { tree ->
            if (tree.id == treeId) {
                val maintenance = tree.maintenanceState
                val updatedMaintenance = when (task) {
                    MaintenanceTask.WATERING -> maintenance.copy(
                        needsWatering = false,
                        lastWatered = now,
                        healthLevel = (maintenance.healthLevel + 0.3f).coerceAtMost(1.0f)
                    )
                    MaintenanceTask.WEEDING -> maintenance.copy(
                        hasWeeds = false,
                        lastWeeded = now,
                        healthLevel = (maintenance.healthLevel + 0.2f).coerceAtMost(1.0f)
                    )
                    MaintenanceTask.PEST_CONTROL -> maintenance.copy(
                        hasPests = false,
                        lastPestControl = now,
                        healthLevel = (maintenance.healthLevel + 0.2f).coerceAtMost(1.0f)
                    )
                    MaintenanceTask.FERTILIZING -> maintenance.copy(
                        healthLevel = 1.0f // Fertilizing always brings to full health
                    )
                }
                tree.copy(maintenanceState = updatedMaintenance)
            } else {
                tree
            }
        }
        
        val updatedTasks = currentState.activeTasks.filter { 
            !(it.treeId == treeId && it.task == task) 
        }
        
        val newState = currentState.copy(
            trees = updatedTrees,
            activeTasks = updatedTasks
        )
        _forestState.value = newState
        saveState(newState)
    }
    
    fun getCurrentMaintenanceTask(): ActiveMaintenanceTask? {
        return _forestState.value.activeTasks.firstOrNull()
    }
    
    fun getMaintenanceTasksCount(): Int {
        return _forestState.value.activeTasks.size
    }

    fun clearForest() {
        val newState = ForestState(
            trees = emptyList(),
            isDayTime = true,
            currentStreak = 0,
            longestStreak = 0,
            totalCompletedSessions = 0
        )
        _forestState.value = newState
        saveState(newState)
    }
    
    private fun loadPersistedState(): ForestState {
        return try {
            val trees = loadTrees()
            val activeTasks = loadActiveTasks()
            val currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
            val longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0)
            val totalCompleted = prefs.getInt(KEY_TOTAL_COMPLETED, 0)
            val isDayTime = prefs.getBoolean(KEY_IS_DAY_TIME, true)
            
            ForestState(
                trees = trees,
                isDayTime = isDayTime,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                totalCompletedSessions = totalCompleted,
                activeTasks = activeTasks
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading persisted state, using default", e)
            ForestState(
                trees = emptyList(),
                isDayTime = true,
                currentStreak = 0,
                longestStreak = 0,
                totalCompletedSessions = 0,
                activeTasks = emptyList()
            )
        }
    }
    
    private fun saveState(state: ForestState) {
        try {
            saveTrees(state.trees)
            saveActiveTasks(state.activeTasks)
            prefs.edit()
                .putInt(KEY_CURRENT_STREAK, state.currentStreak)
                .putInt(KEY_LONGEST_STREAK, state.longestStreak)
                .putInt(KEY_TOTAL_COMPLETED, state.totalCompletedSessions)
                .putBoolean(KEY_IS_DAY_TIME, state.isDayTime)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving forest state", e)
        }
    }
    
    private fun loadTrees(): List<Tree> {
        return try {
            val jsonString = prefs.getString(KEY_TREES, null) ?: return emptyList()
            if (jsonString.isBlank()) return emptyList()
            
            val jsonArray = JSONArray(jsonString)
            val trees = mutableListOf<Tree>()
            
            for (i in 0 until jsonArray.length()) {
                try {
                    val treeJson = jsonArray.getJSONObject(i)
                    val tree = parseTreeFromJson(treeJson)
                    trees.add(tree)
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping corrupted tree at index $i", e)
                }
            }
            
            trees
        } catch (e: Exception) {
            Log.e(TAG, "Error loading trees, returning empty list", e)
            emptyList()
        }
    }
    
    private fun saveTrees(trees: List<Tree>) {
        try {
            val jsonArray = JSONArray()
            
            for (tree in trees) {
                try {
                    val treeJson = convertTreeToJson(tree)
                    jsonArray.put(treeJson)
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping tree due to serialization error: ${tree.id}", e)
                }
            }
            
            prefs.edit()
                .putString(KEY_TREES, jsonArray.toString())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving trees", e)
        }
    }
    
    private fun parseTreeFromJson(json: JSONObject): Tree {
        val sessionJson = json.getJSONObject("sessionData")
        
        // Parse maintenance state (with defaults for backward compatibility)
        val maintenanceJson = json.optJSONObject("maintenanceState")
        val maintenanceState = if (maintenanceJson != null) {
            MaintenanceState(
                needsWatering = maintenanceJson.getBoolean("needsWatering"),
                hasWeeds = maintenanceJson.getBoolean("hasWeeds"),
                hasPests = maintenanceJson.getBoolean("hasPests"),
                lastWatered = Date(maintenanceJson.getLong("lastWatered")),
                lastWeeded = Date(maintenanceJson.getLong("lastWeeded")),
                lastPestControl = Date(maintenanceJson.getLong("lastPestControl")),
                healthLevel = maintenanceJson.getDouble("healthLevel").toFloat()
            )
        } else {
            MaintenanceState() // Default state for old data
        }
        
        return Tree(
            id = json.getString("id"),
            x = json.getDouble("x").toFloat(),
            y = json.getDouble("y").toFloat(),
            treeType = TreeType.valueOf(json.getString("treeType")),
            sessionData = SessionData(
                taskName = sessionJson.optString("taskName").takeIf { it.isNotEmpty() },
                durationMillis = sessionJson.getLong("durationMillis"),
                completedDate = Date(sessionJson.getLong("completedDate")),
                wasCompleted = sessionJson.getBoolean("wasCompleted"),
                streakPosition = sessionJson.getInt("streakPosition"),
                wasPerfectFocus = sessionJson.getBoolean("wasPerfectFocus")
            ),
            plantedDate = Date(json.getLong("plantedDate")),
            maintenanceState = maintenanceState
        )
    }
    
    private fun convertTreeToJson(tree: Tree): JSONObject {
        return JSONObject().apply {
            put("id", tree.id)
            put("x", tree.x.toDouble())
            put("y", tree.y.toDouble())
            put("treeType", tree.treeType.name)
            put("plantedDate", tree.plantedDate.time)
            put("sessionData", JSONObject().apply {
                put("taskName", tree.sessionData.taskName ?: "")
                put("durationMillis", tree.sessionData.durationMillis)
                put("completedDate", tree.sessionData.completedDate.time)
                put("wasCompleted", tree.sessionData.wasCompleted)
                put("streakPosition", tree.sessionData.streakPosition)
                put("wasPerfectFocus", tree.sessionData.wasPerfectFocus)
            })
            put("maintenanceState", JSONObject().apply {
                put("needsWatering", tree.maintenanceState.needsWatering)
                put("hasWeeds", tree.maintenanceState.hasWeeds)
                put("hasPests", tree.maintenanceState.hasPests)
                put("lastWatered", tree.maintenanceState.lastWatered.time)
                put("lastWeeded", tree.maintenanceState.lastWeeded.time)
                put("lastPestControl", tree.maintenanceState.lastPestControl.time)
                put("healthLevel", tree.maintenanceState.healthLevel.toDouble())
            })
        }
    }
    
    private fun saveActiveTasks(tasks: List<ActiveMaintenanceTask>) {
        try {
            val jsonArray = JSONArray()
            
            for (task in tasks) {
                try {
                    val taskJson = JSONObject().apply {
                        put("treeId", task.treeId)
                        put("task", task.task.name)
                        put("urgency", task.urgency.toDouble())
                        put("createdDate", task.createdDate.time)
                    }
                    jsonArray.put(taskJson)
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping task due to serialization error: ${task.treeId}", e)
                }
            }
            
            prefs.edit()
                .putString(KEY_ACTIVE_TASKS, jsonArray.toString())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving active tasks", e)
        }
    }
    
    private fun loadActiveTasks(): List<ActiveMaintenanceTask> {
        return try {
            val jsonString = prefs.getString(KEY_ACTIVE_TASKS, null) ?: return emptyList()
            if (jsonString.isBlank()) return emptyList()
            
            val jsonArray = JSONArray(jsonString)
            val tasks = mutableListOf<ActiveMaintenanceTask>()
            
            for (i in 0 until jsonArray.length()) {
                try {
                    val taskJson = jsonArray.getJSONObject(i)
                    val task = ActiveMaintenanceTask(
                        treeId = taskJson.getString("treeId"),
                        task = MaintenanceTask.valueOf(taskJson.getString("task")),
                        urgency = taskJson.getDouble("urgency").toFloat(),
                        createdDate = Date(taskJson.getLong("createdDate"))
                    )
                    tasks.add(task)
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping corrupted task at index $i", e)
                }
            }
            
            tasks
        } catch (e: Exception) {
            Log.e(TAG, "Error loading active tasks, returning empty list", e)
            emptyList()
        }
    }
    
    private fun initializeDemoTrees() {
        Log.d(TAG, "Initializing demo trees for maintenance system testing")
        
        val now = Date()
        val yesterday = Date(now.time - 24 * 60 * 60 * 1000L) // 24 hours ago
        val twoDaysAgo = Date(now.time - 48 * 60 * 60 * 1000L) // 48 hours ago
        
        // Create demo trees with different maintenance needs
        val demoTrees = listOf(
            // Tree that needs watering (last watered 30 hours ago)
            Tree(
                x = 50f, y = 100f,
                treeType = TreeType.OAK,
                sessionData = SessionData(
                    taskName = "Demo Focus Session",
                    durationMillis = 25 * 60 * 1000L,
                    completedDate = yesterday,
                    wasCompleted = true,
                    streakPosition = 1,
                    wasPerfectFocus = true
                ),
                plantedDate = yesterday,
                maintenanceState = MaintenanceState(
                    needsWatering = false, // Will be calculated by updateMaintenanceNeeds
                    hasWeeds = false,
                    hasPests = false,
                    lastWatered = Date(now.time - 30 * 60 * 60 * 1000L), // 30 hours ago
                    lastWeeded = now,
                    lastPestControl = now,
                    healthLevel = 1.0f
                )
            ),
            // Tree that needs weeding (last weeded 40 hours ago)
            Tree(
                x = 150f, y = 100f,
                treeType = TreeType.CHERRY,
                sessionData = SessionData(
                    taskName = "Demo Focus Session",
                    durationMillis = 25 * 60 * 1000L,
                    completedDate = twoDaysAgo,
                    wasCompleted = true,
                    streakPosition = 2,
                    wasPerfectFocus = true
                ),
                plantedDate = twoDaysAgo,
                maintenanceState = MaintenanceState(
                    needsWatering = false,
                    hasWeeds = false, // Will be calculated by updateMaintenanceNeeds
                    hasPests = false,
                    lastWatered = now,
                    lastWeeded = Date(now.time - 40 * 60 * 60 * 1000L), // 40 hours ago
                    lastPestControl = now,
                    healthLevel = 1.0f
                )
            ),
            // Healthy tree (recently maintained)
            Tree(
                x = 250f, y = 100f,
                treeType = TreeType.PINE,
                sessionData = SessionData(
                    taskName = "Demo Focus Session",
                    durationMillis = 25 * 60 * 1000L,
                    completedDate = now,
                    wasCompleted = true,
                    streakPosition = 3,
                    wasPerfectFocus = true
                ),
                plantedDate = now,
                maintenanceState = MaintenanceState(
                    needsWatering = false,
                    hasWeeds = false,
                    hasPests = false,
                    lastWatered = now,
                    lastWeeded = now,
                    lastPestControl = now,
                    healthLevel = 1.0f
                )
            )
        )
        
        val newState = _forestState.value.copy(
            trees = demoTrees,
            currentStreak = 3,
            longestStreak = 3,
            totalCompletedSessions = 3
        )
        _forestState.value = newState
        saveState(newState)
        
        // Update maintenance needs to generate tasks
        updateMaintenanceNeeds()
    }
}