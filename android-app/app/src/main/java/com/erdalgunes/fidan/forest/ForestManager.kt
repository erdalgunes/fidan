package com.erdalgunes.fidan.forest

import com.erdalgunes.fidan.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Date
import kotlin.random.Random

class ForestManager {
    private val _forestState = MutableStateFlow(ForestState())
    val forestState: StateFlow<ForestState> = _forestState.asStateFlow()
    
    private val trees = mutableListOf<Tree>()
    
    init {
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