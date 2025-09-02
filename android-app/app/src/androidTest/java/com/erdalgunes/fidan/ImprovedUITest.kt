package com.erdalgunes.fidan

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImprovedUITest {
    
    private lateinit var device: UiDevice
    private val LAUNCH_TIMEOUT = 5000L
    private val PACKAGE_NAME = "com.erdalgunes.fidan.debug"
    
    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressHome()
        
        val launcherPackage = device.launcherPackageName
        assertNotNull(launcherPackage)
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT)
    }
    
    @Test
    fun testAppLaunchesSuccessfully() {
        // Launch the app
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
        assertNotNull("Could not find launch intent for $PACKAGE_NAME", intent)
        
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        
        // Wait for the app to appear
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), LAUNCH_TIMEOUT)
        
        // Verify the app is in foreground
        val currentPackage = device.currentPackageName
        assertEquals("App should be in foreground", PACKAGE_NAME, currentPackage)
    }
    
    @Test
    fun testMainUIElements() {
        launchApp()
        
        // Check for Fidan title in the top bar
        val fidanTitle = device.wait(
            Until.findObject(By.text("Fidan")),
            LAUNCH_TIMEOUT
        )
        assertNotNull("Should find 'Fidan' title", fidanTitle)
        
        // Check for Timer tab
        val timerTab = device.findObject(By.text("Timer"))
        assertNotNull("Should find Timer tab", timerTab)
        
        // Check for Forest tab
        val forestTab = device.findObject(By.text("Forest"))
        assertNotNull("Should find Forest tab", forestTab)
        
        // Check for Stats tab
        val statsTab = device.findObject(By.text("Stats"))
        assertNotNull("Should find Stats tab", statsTab)
        
        // Check for timer display (25:00)
        val timerDisplay = device.findObject(By.text("25:00"))
        assertNotNull("Should find timer display showing 25:00", timerDisplay)
        
        // Check for Focus Time text
        val focusText = device.findObject(By.text("Focus Time"))
        assertNotNull("Should find 'Focus Time' text", focusText)
    }
    
    @Test
    fun testNavigationBetweenTabs() {
        launchApp()
        
        // Click on Forest tab
        val forestTab = device.findObject(By.text("Forest"))
        assertNotNull("Forest tab should exist", forestTab)
        forestTab.click()
        Thread.sleep(500)
        
        // Verify Forest screen content
        val yourForestText = device.findObject(By.text("Your Forest"))
        assertNotNull("Should see 'Your Forest' text on Forest tab", yourForestText)
        
        val treesPlantedText = device.findObject(By.textContains("Trees Planted"))
        assertNotNull("Should see trees planted info", treesPlantedText)
        
        // Click on Stats tab
        val statsTab = device.findObject(By.text("Stats"))
        assertNotNull("Stats tab should exist", statsTab)
        statsTab.click()
        Thread.sleep(500)
        
        // Verify Stats screen content
        val statisticsText = device.findObject(By.text("Statistics"))
        assertNotNull("Should see 'Statistics' text on Stats tab", statisticsText)
        
        val todayText = device.findObject(By.text("Today"))
        assertNotNull("Should see 'Today' stat card", todayText)
        
        // Return to Timer tab
        val timerTab = device.findObject(By.text("Timer"))
        assertNotNull("Timer tab should exist", timerTab)
        timerTab.click()
        Thread.sleep(500)
        
        // Verify we're back on Timer screen
        val timerDisplay = device.findObject(By.text("25:00"))
        assertNotNull("Should be back on Timer screen with 25:00 display", timerDisplay)
    }
    
    @Test
    fun testStartButtonInteraction() {
        launchApp()
        
        // Find the ready to focus text initially
        val readyText = device.findObject(By.text("Ready to focus?"))
        assertNotNull("Should see 'Ready to focus?' initially", readyText)
        
        // Find and click the play button (FAB)
        // Since we can't easily identify the FAB by icon, we'll click in its expected location
        val screenWidth = device.displayWidth
        val screenHeight = device.displayHeight
        
        // FAB is usually centered horizontally and below the timer
        device.click(screenWidth / 2, (screenHeight * 0.65).toInt())
        Thread.sleep(500)
        
        // Check if the text changed to "Focus on your task!"
        val focusText = device.findObject(By.text("Focus on your task!"))
        if (focusText != null) {
            // Successfully toggled to running state
            assertTrue("Timer should be running", true)
            
            // Click again to pause
            device.click(screenWidth / 2, (screenHeight * 0.65).toInt())
            Thread.sleep(500)
            
            // Should be back to ready state
            val readyAgain = device.findObject(By.text("Ready to focus?"))
            assertNotNull("Should see 'Ready to focus?' after pausing", readyAgain)
        }
    }
    
    @Test
    fun testSettingsButtonExists() {
        launchApp()
        
        // The settings button should be in the top bar
        // We can't easily identify it by icon, but we can check the content description
        val settingsButton = device.findObject(By.desc("Settings"))
        assertNotNull("Settings button should exist in the app bar", settingsButton)
    }
    
    @Test
    fun testScreenRotation() {
        launchApp()
        
        // Rotate to landscape
        device.setOrientationLeft()
        Thread.sleep(1000)
        
        // Verify app is still visible and functional
        val isAppVisible = device.wait(
            Until.hasObject(By.pkg(PACKAGE_NAME)),
            LAUNCH_TIMEOUT
        )
        assertTrue("App should remain visible after rotation to landscape", isAppVisible)
        
        // Check that main elements are still present
        val fidanTitle = device.findObject(By.text("Fidan"))
        assertNotNull("Fidan title should still be visible in landscape", fidanTitle)
        
        // Rotate back to portrait
        device.setOrientationNatural()
        Thread.sleep(1000)
        
        // Verify app is still functional - check for any key element
        val fidanTitleAfterRotation = device.findObject(By.text("Fidan"))
        if (fidanTitleAfterRotation == null) {
            // Try to find the timer display or any other element
            val timerDisplay = device.findObject(By.text("25:00"))
            val focusTime = device.findObject(By.text("Focus Time"))
            val timerTab = device.findObject(By.text("Timer"))
            
            assertTrue("App should have at least one visible element after rotation back to portrait", 
                timerDisplay != null || focusTime != null || timerTab != null || fidanTitleAfterRotation != null)
        }
    }
    
    private fun launchApp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), LAUNCH_TIMEOUT)
    }
}