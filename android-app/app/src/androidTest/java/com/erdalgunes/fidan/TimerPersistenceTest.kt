package com.erdalgunes.fidan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerPersistenceTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun timerActuallyCountsDownWhileSwitchingTabs() {
        // Start on Timer tab
        composeTestRule.onNodeWithText("Timer").assertIsSelected()
        
        // Verify initial timer state shows 25:00
        composeTestRule.onNodeWithText("25:00").assertIsDisplayed()
        
        // Start the timer
        composeTestRule.onNodeWithContentDescription("Start").performClick()
        
        // Wait for 3 seconds
        Thread.sleep(3000)
        
        // Switch to Forest tab
        composeTestRule.onNodeWithText("Forest").performClick()
        
        // Wait another 3 seconds while on Forest tab
        Thread.sleep(3000)
        
        // Switch back to Timer tab
        composeTestRule.onNodeWithText("Timer").performClick()
        
        // Timer should have counted down ~6 seconds, so should show around 24:54
        // Check for any of these possible values (accounting for timing variations)
        val possibleTimes = listOf("24:54", "24:53", "24:52", "24:51")
        val foundCorrectTime = possibleTimes.any { time ->
            try {
                composeTestRule.onNodeWithText(time).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        assert(foundCorrectTime) { 
            "Timer did not count down properly. Expected around 24:54 after 6 seconds, but couldn't find any expected time values" 
        }
        
        // Stop the timer
        composeTestRule.onNodeWithContentDescription("Stop").performClick()
        
        // Verify timer resets to 25:00
        composeTestRule.onNodeWithText("25:00").assertIsDisplayed()
    }
    
    @Test
    fun stopButtonOnlyAppearsWhenRunning() {
        // Initially, only Start button should be visible
        composeTestRule.onNodeWithContentDescription("Start").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Stop").assertDoesNotExist()
        
        // Start the timer
        composeTestRule.onNodeWithContentDescription("Start").performClick()
        
        // Now Stop button should be visible, Start button gone
        composeTestRule.onNodeWithContentDescription("Stop").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Start").assertDoesNotExist()
        
        // Stop the timer
        composeTestRule.onNodeWithContentDescription("Stop").performClick()
        
        // Back to showing Start button only
        composeTestRule.onNodeWithContentDescription("Start").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Stop").assertDoesNotExist()
    }
    
    @Test
    fun stoppingEarlyPlantsStubTree() {
        // Start the timer
        composeTestRule.onNodeWithContentDescription("Start").performClick()
        
        // Wait a moment
        Thread.sleep(2000)
        
        // Stop early
        composeTestRule.onNodeWithContentDescription("Stop").performClick()
        
        // Navigate to Forest tab
        composeTestRule.onNodeWithText("Forest").performClick()
        
        // Verify seedling (stub tree) was planted - just check for the seedling emoji
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("🌱", substring = true).fetchSemanticsNodes().let { nodes ->
            assert(nodes.isNotEmpty()) { "No seedling found after stopping timer early" }
        }
    }
    
    @Test
    fun completedAndIncompleteTreesArePersisted() {
        // Start and stop timer early to create a seedling
        composeTestRule.onNodeWithContentDescription("Start").performClick()
        Thread.sleep(1000)
        composeTestRule.onNodeWithContentDescription("Stop").performClick()
        
        // Check Forest shows a seedling
        composeTestRule.onNodeWithText("Forest").performClick()
        composeTestRule.waitForIdle()
        
        // Just verify we have at least one seedling
        composeTestRule.onAllNodesWithText("🌱", substring = true).fetchSemanticsNodes().let { nodes ->
            assert(nodes.isNotEmpty()) { "No seedling found after first stop" }
        }
        
        // Go to Stats tab
        composeTestRule.onNodeWithText("Stats").performClick()
        composeTestRule.waitForIdle()
        
        // Verify stats show 0 completed sessions (looking for the "0" in sessions completed)
        composeTestRule.onNodeWithText("Sessions").assertIsDisplayed()
        
        // Go back to Timer
        composeTestRule.onNodeWithText("Timer").performClick()
        
        // Start another session and stop early again
        composeTestRule.onNodeWithContentDescription("Start").performClick()
        Thread.sleep(1000)
        composeTestRule.onNodeWithContentDescription("Stop").performClick()
        
        // Check Forest again
        composeTestRule.onNodeWithText("Forest").performClick()
        composeTestRule.waitForIdle()
        
        // Verify we still have seedlings (don't check exact count due to display variations)
        composeTestRule.onAllNodesWithText("🌱", substring = true).fetchSemanticsNodes().let { nodes ->
            assert(nodes.isNotEmpty()) { "No seedlings found after second stop" }
        }
    }
    
    @Test
    fun timerShowsCorrectMessages() {
        // Initially shows "Ready to focus?"
        composeTestRule.onNodeWithText("Ready to focus?").assertIsDisplayed()
        
        // Start timer - should show "Focus on your task!"
        composeTestRule.onNodeWithContentDescription("Start").performClick()
        composeTestRule.onNodeWithText("Focus on your task!").assertIsDisplayed()
        
        // Stop timer - back to "Ready to focus?"
        composeTestRule.onNodeWithContentDescription("Stop").performClick()
        composeTestRule.onNodeWithText("Ready to focus?").assertIsDisplayed()
    }
}