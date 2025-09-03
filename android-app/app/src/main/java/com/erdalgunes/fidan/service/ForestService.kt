package com.erdalgunes.fidan.service

import com.erdalgunes.fidan.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForestService @Inject constructor() {
    
    private val _forestState = MutableStateFlow(
        ForestState(
            trees = emptyList(),
            isDayTime = true
        )
    )
    val forestState: StateFlow<ForestState> = _forestState.asStateFlow()
    
    fun addTree(sessionData: SessionData) {
        val currentState = _forestState.value
        
        // Calculate position based on current number of trees (grid layout)
        val treeIndex = currentState.trees.size
        val x = (treeIndex % 6).toFloat() * 100f // 6 columns
        val y = (treeIndex / 6).toFloat() * 100f
        
        val treeType = if (sessionData.wasCompleted) {
            TreeType.getRandomCompletedTreeType()
        } else {
            TreeType.SAPLING
        }
        
        val newTree = Tree(
            id = UUID.randomUUID().toString(),
            x = x,
            y = y,
            treeType = treeType,
            sessionData = sessionData
        )
        
        val updatedTrees = currentState.trees + newTree
        
        _forestState.value = currentState.copy(
            trees = updatedTrees
        )
    }
    
    fun updateDayNightCycle() {
        _forestState.value = _forestState.value.copy(
            isDayTime = !_forestState.value.isDayTime
        )
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
    
    fun clearForest() {
        _forestState.value = ForestState(
            trees = emptyList(),
            isDayTime = true
        )
    }
}