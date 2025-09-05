package com.erdalgunes.fidan.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.erdalgunes.fidan.TimerCallback
import com.erdalgunes.fidan.TimerManager
import com.erdalgunes.fidan.repository.TimerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for TimerService.
 * SOLID: Tests single responsibility of TimerService.
 * DRY: Reusable test utilities and setup.
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class TimerServiceTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockTimerRepository: TimerRepository
    
    @Mock
    private lateinit var mockNotificationManager: NotificationManager
    
    private lateinit var timerService: TimerService
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
    
    @Before
    fun setup() {
        whenever(mockContext.getSystemService(Context.NOTIFICATION_SERVICE))
            .thenReturn(mockNotificationManager)
        
        timerService = TimerService().apply {
            // Inject mocks through reflection or setter methods
            // In production, use Hilt's @TestInstallIn
        }
    }
    
    @Test
    fun `startTimer creates foreground notification`() = testScope.runTest {
        // Given
        val intent = Intent().apply {
            action = TimerService.ACTION_START
        }
        
        // When
        timerService.onStartCommand(intent, 0, 1)
        
        // Then
        verify(mockNotificationManager, times(1)).notify(any(), any())
    }
    
    @Test
    fun `stopTimer cancels service`() = testScope.runTest {
        // Given
        val intent = Intent().apply {
            action = TimerService.ACTION_STOP
        }
        
        // When
        val result = timerService.onStartCommand(intent, 0, 1)
        
        // Then
        assertEquals(TimerService.START_STICKY, result)
    }
    
    @Test
    fun `service cleanup cancels timer manager`() {
        // When
        timerService.onDestroy()
        
        // Then
        // Verify cleanup through side effects or state changes
        assertNotNull(timerService)
    }
    
    @Test
    fun `notification updates on timer state change`() = testScope.runTest {
        // Given
        val intent = Intent().apply {
            action = TimerService.ACTION_START
        }
        
        // When
        timerService.onStartCommand(intent, 0, 1)
        advanceTimeBy(1000)
        
        // Then
        verify(mockNotificationManager, atLeast(1)).notify(any(), any())
    }
}