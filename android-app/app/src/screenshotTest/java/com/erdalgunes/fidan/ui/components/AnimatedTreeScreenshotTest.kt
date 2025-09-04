package com.erdalgunes.fidan.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.erdalgunes.fidan.data.*
import com.erdalgunes.fidan.ui.components.AnimatedTree
import com.erdalgunes.fidan.ui.components.PerformanceLevel
import com.erdalgunes.fidan.ui.theme.FidanTheme
import java.util.*

/**
 * Screenshot tests for AnimatedTree component visual validation
 * Addresses critical gap in visual regression testing identified in testing assessment
 */
class AnimatedTreeScreenshotTest {

    @Preview(name = "Tree with Watering Needed")
    @Composable
    fun TreeWithWateringNeeded() {
        FidanTheme {
            val tree = Tree(
                id = "test-watering",
                x = 0f,
                y = 0f,
                treeType = TreeType.OAK,
                sessionData = SessionData(
                    durationMillis = 25 * 60 * 1000L,
                    completedDate = Date(),
                    wasCompleted = true,
                    streakPosition = 2
                ),
                maintenanceState = MaintenanceState(
                    needsWatering = true,
                    hasWeeds = false,
                    hasPests = false,
                    lastWatered = Date(System.currentTimeMillis() - 25 * 60 * 60 * 1000L), // 25 hours ago
                    lastWeeded = Date(),
                    lastPestControl = Date(),
                    healthLevel = 0.6f
                )
            )
            
            Box(modifier = Modifier.size(120.dp)) {
                AnimatedTree(
                    tree = tree,
                    performanceLevel = PerformanceLevel.HIGH,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }

    @Preview(name = "Tree with Weeds and Pests")
    @Composable
    fun TreeWithWeedsAndPests() {
        FidanTheme {
            val tree = Tree(
                id = "test-multiple-issues",
                x = 0f,
                y = 0f,
                treeType = TreeType.PINE,
                sessionData = SessionData(
                    durationMillis = 25 * 60 * 1000L,
                    completedDate = Date(),
                    wasCompleted = true,
                    streakPosition = 3
                ),
                maintenanceState = MaintenanceState(
                    needsWatering = false,
                    hasWeeds = true,
                    hasPests = true,
                    lastWatered = Date(),
                    lastWeeded = Date(System.currentTimeMillis() - 37 * 60 * 60 * 1000L), // 37 hours ago
                    lastPestControl = Date(System.currentTimeMillis() - 49 * 60 * 60 * 1000L), // 49 hours ago
                    healthLevel = 0.3f
                )
            )
            
            Box(modifier = Modifier.size(120.dp)) {
                AnimatedTree(
                    tree = tree,
                    performanceLevel = PerformanceLevel.HIGH,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }

    @Preview(name = "Healthy Mature Tree")
    @Composable
    fun HealthyMatureTree() {
        FidanTheme {
            val tree = Tree(
                id = "test-healthy",
                x = 0f,
                y = 0f,
                treeType = TreeType.CHERRY,
                sessionData = SessionData(
                    durationMillis = 25 * 60 * 1000L,
                    completedDate = Date(),
                    wasCompleted = true,
                    streakPosition = 5
                ),
                maintenanceState = MaintenanceState(
                    needsWatering = false,
                    hasWeeds = false,
                    hasPests = false,
                    lastWatered = Date(),
                    lastWeeded = Date(),
                    lastPestControl = Date(),
                    healthLevel = 1.0f
                )
            )
            
            Box(modifier = Modifier.size(120.dp)) {
                AnimatedTree(
                    tree = tree,
                    performanceLevel = PerformanceLevel.HIGH,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }

    @Preview(name = "Incomplete Tree Sapling")
    @Composable
    fun IncompleteTreeSapling() {
        FidanTheme {
            val tree = Tree(
                id = "test-sapling",
                x = 0f,
                y = 0f,
                treeType = TreeType.SAPLING,
                sessionData = SessionData(
                    durationMillis = 5 * 60 * 1000L, // Only 5 minutes
                    completedDate = Date(),
                    wasCompleted = false,
                    streakPosition = 0
                ),
                maintenanceState = MaintenanceState(
                    needsWatering = false,
                    hasWeeds = false,
                    hasPests = false,
                    lastWatered = Date(),
                    lastWeeded = Date(),
                    lastPestControl = Date(),
                    healthLevel = 0.8f
                )
            )
            
            Box(modifier = Modifier.size(120.dp)) {
                AnimatedTree(
                    tree = tree,
                    performanceLevel = PerformanceLevel.HIGH,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }

    @Preview(name = "Special Golden Tree")
    @Composable
    fun SpecialGoldenTree() {
        FidanTheme {
            val tree = Tree(
                id = "test-special",
                x = 0f,
                y = 0f,
                treeType = TreeType.GOLDEN_OAK,
                sessionData = SessionData(
                    durationMillis = 25 * 60 * 1000L,
                    completedDate = Date(),
                    wasCompleted = true,
                    streakPosition = 7
                ),
                maintenanceState = MaintenanceState(
                    needsWatering = false,
                    hasWeeds = false,
                    hasPests = false,
                    lastWatered = Date(),
                    lastWeeded = Date(),
                    lastPestControl = Date(),
                    healthLevel = 1.0f
                )
            )
            
            Box(modifier = Modifier.size(120.dp)) {
                AnimatedTree(
                    tree = tree,
                    performanceLevel = PerformanceLevel.HIGH,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }

    @Preview(name = "Tree Performance Levels")
    @Composable
    fun TreePerformanceLevels() {
        FidanTheme {
            val tree = Tree(
                id = "test-performance",
                x = 0f,
                y = 0f,
                treeType = TreeType.PALM,
                sessionData = SessionData(
                    durationMillis = 25 * 60 * 1000L,
                    completedDate = Date(),
                    wasCompleted = true,
                    streakPosition = 3
                ),
                maintenanceState = MaintenanceState(
                    needsWatering = false,
                    hasWeeds = false,
                    hasPests = false,
                    lastWatered = Date(),
                    lastWeeded = Date(),
                    lastPestControl = Date(),
                    healthLevel = 1.0f
                )
            )
            
            // Test different performance levels
            Box(modifier = Modifier.size(120.dp)) {
                AnimatedTree(
                    tree = tree,
                    performanceLevel = PerformanceLevel.MEDIUM,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }
}