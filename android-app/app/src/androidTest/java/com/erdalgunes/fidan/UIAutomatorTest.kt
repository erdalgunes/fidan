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
class UIAutomatorTest {
    
    private lateinit var device: UiDevice
    private val LAUNCH_TIMEOUT = 5000L
    private val PACKAGE_NAME = "com.erdalgunes.fidan.debug"
    
    @Before
    fun setup() {
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Press home button to start from a known state
        device.pressHome()
        
        // Wait for launcher
        val launcherPackage = device.launcherPackageName
        assertNotNull(launcherPackage)
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT)
    }
    
    @Test
    fun testAppLaunches() {
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
    fun testMainScreenElements() {
        // Launch the app first
        testAppLaunches()
        
        // Look for the Fidan title text
        val titleText = device.wait(
            Until.findObject(By.text("Fidan")),
            LAUNCH_TIMEOUT
        )
        assertNotNull("Should find 'Fidan' title", titleText)
        
        // Look for the emoji
        val emojiText = device.findObject(By.text("🌱"))
        assertNotNull("Should find tree emoji", emojiText)
        
        // Look for the ready to focus message (timer is default tab)
        val readyText = device.findObject(By.textContains("Ready to focus?"))
        assertNotNull("Should find ready to focus message", readyText)
        
        // Look for the focus time text
        val focusTimeText = device.findObject(By.textContains("Focus Time"))
        assertNotNull("Should find focus time text", focusTimeText)
    }
    
    @Test
    fun testScreenOrientation() {
        // Launch the app
        testAppLaunches()
        
        // Test rotation to landscape
        device.setOrientationLeft()
        Thread.sleep(1000) // Wait for rotation animation
        
        // Verify app is still visible
        val isAppVisible = device.wait(
            Until.hasObject(By.pkg(PACKAGE_NAME)),
            LAUNCH_TIMEOUT
        )
        assertTrue("App should remain visible after rotation", isAppVisible)
        
        // Rotate back to portrait
        device.setOrientationNatural()
        Thread.sleep(1000)
        
        // Verify app is still functioning
        val titleAfterRotation = device.findObject(By.text("Fidan"))
        assertNotNull("Title should still be visible after rotation", titleAfterRotation)
    }
    
    @Test
    fun testAppResponsiveness() {
        // Launch the app
        testAppLaunches()
        
        // Try to interact with the screen (tap on center)
        val screenWidth = device.displayWidth
        val screenHeight = device.displayHeight
        device.click(screenWidth / 2, screenHeight / 2)
        
        // App should still be responsive
        val isStillResponsive = device.wait(
            Until.hasObject(By.pkg(PACKAGE_NAME)),
            1000
        )
        assertTrue("App should remain responsive after interaction", isStillResponsive)
        
        // Test back button (app should handle it or exit gracefully)
        device.pressBack()
        Thread.sleep(500)
        
        // Check if app closed or is still running
        val currentPackageAfterBack = device.currentPackageName
        // App might close or stay open depending on implementation
        println("Package after back press: $currentPackageAfterBack")
    }
}