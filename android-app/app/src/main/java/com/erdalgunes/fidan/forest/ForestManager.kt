package com.erdalgunes.fidan.forest

import android.content.Context
import android.content.SharedPreferences
import com.erdalgunes.fidan.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.Calendar
import java.util.Date
import kotlin.random.Random

class ForestManager(private val context: Context) {
    private val _forestState = MutableStateFlow(ForestState())
    val forestState: StateFlow<ForestState> = _forestState.asStateFlow()
    
    private val trees = mutableListOf<Tree>()
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("forest_data", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    init {
        loadPersistedTrees()
        updateDayNightCycle()
    }
    
    fun addTree(sessionData: SessionData) {
        val treeType = TreeType.getTreeTypeForSession(sessionData.wasCompleted)
        val position = generateTreePosition()
        
        val newTree = Tree(
            x = position.first,
            y = position.second,
            treeType = treeType,
            sessionData = sessionData
        )
        
        trees.add(newTree)
        updateForestState()
        persistTrees()
    }
    
    fun getTreeAtGridPosition(gridX: Int, gridY: Int): Tree? {
        return trees.find { tree ->
            val treeGridX = (tree.x / 100f).toInt()
            val treeGridY = (tree.y / 100f).toInt()
            treeGridX == gridX && treeGridY == gridY
        }
    }
    
    fun updatePanAndZoom(offsetX: Float, offsetY: Float, scale: Float) {
        val currentState = _forestState.value
        _forestState.value = currentState.copy(
            offsetX = offsetX,
            offsetY = offsetY,
            scale = scale.coerceIn(0.5f, 3f)
        )
    }
    
    fun updateDayNightCycle() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isDayTime = hour in 6..18
        
        val currentState = _forestState.value
        _forestState.value = currentState.copy(isDayTime = isDayTime)
    }
    
    private fun generateTreePosition(): Pair<Float, Float> {
        // Grid-based positioning for farm-style layout
        val treeIndex = trees.size
        val gridX = (treeIndex % 6).toFloat() * 100f // 6 columns
        val gridY = (treeIndex / 6).toFloat() * 100f
        
        return Pair(gridX, gridY)
    }
    
    private fun updateForestState() {
        val currentState = _forestState.value
        _forestState.value = currentState.copy(trees = trees.toList())
    }
    
    private fun persistTrees() {
        try {
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
            sharedPrefs.edit().putString("trees", jsonString).apply()
        } catch (e: Exception) {
            // Silent fail - not critical
        }
    }
    
    private fun loadPersistedTrees() {
        try {
            val jsonString = sharedPrefs.getString("trees", null) ?: return
            val persistableTrees = json.decodeFromString<List<PersistableTree>>(jsonString)
            
            trees.clear()
            trees.addAll(persistableTrees.map { persistableTree ->
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
            })
            updateForestState()
        } catch (e: Exception) {
            // Silent fail - start with empty forest
            trees.clear()
        }
    }
    
    fun clearForest() {
        trees.clear()
        updateForestState()
        sharedPrefs.edit().remove("trees").apply()
    }
    
    // For demo purposes - add some sample trees
    fun addSampleTrees() {
        repeat(5) { index ->
            val sessionData = SessionData(
                taskName = "Focus Session ${index + 1}",
                durationMillis = 25 * 60 * 1000L,
                completedDate = Date(),
                wasCompleted = index < 3 // First 3 completed, last 2 incomplete
            )
            addTree(sessionData)
        }
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