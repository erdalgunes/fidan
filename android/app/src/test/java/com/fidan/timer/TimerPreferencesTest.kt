package com.fidan.timer

import android.content.Context
import android.content.SharedPreferences
import com.fidan.timer.utils.TimerPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

@ExperimentalCoroutinesApi
class TimerPreferencesTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var timerPreferences: TimerPreferences

    @Before
    fun setup() {
        mockContext = mockk()
        mockPrefs = mockk()
        mockEditor = mockk()
        
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
        
        // Default values
        every { mockPrefs.getLong("session_start_time", 0) } returns 0
        every { mockPrefs.getInt("session_count", 0) } returns 0
        every { mockPrefs.getLong("total_focus_time", 0) } returns 0L
    }

    @Test
    fun saveAndRetrieveSessionStartTime() {
        timerPreferences = TimerPreferences.getInstance(mockContext)
        val testTime = 1234567890L
        
        timerPreferences.saveSessionStartTime(testTime)
        
        verify { mockEditor.putLong("session_start_time", testTime) }
        verify { mockEditor.apply() }
    }

    @Test
    fun recordCompletedSession_incrementsStats() = runTest {
        every { mockPrefs.getInt("session_count", 0) } returns 5
        every { mockPrefs.getLong("total_focus_time", 0) } returns 100L
        
        timerPreferences = TimerPreferences.getInstance(mockContext)
        timerPreferences.recordCompletedSession(25)
        
        verify { mockEditor.putInt("session_count", 6) }
        verify { mockEditor.putLong("total_focus_time", 125L) }
    }

    @Test
    fun clearSessionStartTime_removesKey() {
        timerPreferences = TimerPreferences.getInstance(mockContext)
        
        timerPreferences.clearSessionStartTime()
        
        verify { mockEditor.remove("session_start_time") }
        verify { mockEditor.apply() }
    }
}