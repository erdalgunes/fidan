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
    
    fun getTreeAt(x: Float, y: Float, tolerance: Float = 50f): Tree? {
        return trees.find { tree ->
            val dx = tree.x - x
            val dy = tree.y - y
            kotlin.math.sqrt(dx * dx + dy * dy) <= tolerance
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
        // Generate positions in a natural, scattered pattern
        // Using different areas based on number of existing trees
        val areaSize = 1000f
        val centerX = areaSize / 2
        val centerY = areaSize / 2
        
        // Create clusters of trees with some randomness
        val clusterRadius = 200f
        val numClusters = (trees.size / 5) + 1
        val clusterIndex = Random.nextInt(numClusters)
        
        val clusterAngle = (clusterIndex * 2 * kotlin.math.PI / numClusters).toFloat()
        val clusterDistance = Random.nextFloat() * clusterRadius
        
        val clusterCenterX = centerX + kotlin.math.cos(clusterAngle) * clusterDistance
        val clusterCenterY = centerY + kotlin.math.sin(clusterAngle) * clusterDistance
        
        // Add some randomness within the cluster
        val randomOffset = 80f
        val x = clusterCenterX + (Random.nextFloat() - 0.5f) * randomOffset
        val y = clusterCenterY + (Random.nextFloat() - 0.5f) * randomOffset
        
        return Pair(x, y)
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