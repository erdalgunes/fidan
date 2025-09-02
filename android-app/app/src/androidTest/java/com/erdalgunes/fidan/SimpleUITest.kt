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
import java.io.File

@RunWith(AndroidJUnit4::class)
class SimpleUITest {
    
    private lateinit var device: UiDevice
    
    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }
    
    @Test
    fun testTakeScreenshot() {
        // Take a screenshot of current state
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshotFile = File(context.filesDir, "test_screenshot.png")
        device.takeScreenshot(screenshotFile)
        assertTrue("Screenshot should be created", screenshotFile.exists())
        
        // Print current package
        val currentPackage = device.currentPackageName
        println("Current package: $currentPackage")
        
        // Try to find any text on screen
        val texts = device.findObjects(By.textContains(""))
        println("Found ${texts.size} text elements on screen")
        texts.forEach { 
            println("Text: ${it.text}")
        }
    }
    
    @Test
    fun testLaunchAppDirectly() {
        // Use instrumentation context to launch the app
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage("com.erdalgunes.fidan.debug")
        
        if (intent != null) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            
            // Wait a bit
            Thread.sleep(2000)
            
            // Check current package
            val currentPackage = device.currentPackageName
            println("After launch, current package: $currentPackage")
            
            // Take screenshot
            val screenshotFile = File(context.filesDir, "app_launched.png")
            device.takeScreenshot(screenshotFile)
            
            // Try to find Fidan text
            val fidanText = device.findObject(By.textContains("Fidan"))
            if (fidanText != null) {
                println("Found Fidan text: ${fidanText.text}")
            } else {
                println("Fidan text not found")
            }
            
        } else {
            println("Could not get launch intent for com.erdalgunes.fidan.debug")
            
            // Try to list all packages
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)
            packages.filter { it.packageName.contains("fidan") }.forEach {
                println("Found package: ${it.packageName}")
            }
        }
    }
}