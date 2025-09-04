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
            isDayTime = true,
            currentStreak = 0,
            longestStreak = 0,
            totalCompletedSessions = 0
        )
    )
    val forestState: StateFlow<ForestState> = _forestState.asStateFlow()
    
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
        
        _forestState.value = currentState.copy(
            trees = updatedTrees,
            currentStreak = newStreak,
            longestStreak = newLongestStreak,
            totalCompletedSessions = newTotalCompleted
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
    
    fun getCurrentStreak(): Int {
        return _forestState.value.currentStreak
    }
    
    fun getLongestStreak(): Int {
        return _forestState.value.longestStreak
    }
    
    fun getSpecialTreesCount(): Int {
        return _forestState.value.trees.count { it.treeType.isSpecial }
    }
    
    fun clearForest() {
        _forestState.value = ForestState(
            trees = emptyList(),
            isDayTime = true,
            currentStreak = 0,
            longestStreak = 0,
            totalCompletedSessions = 0
        )
    }
}