package com.erdalgunes.fidan.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import com.erdalgunes.fidan.data.ImpactRepository
import com.erdalgunes.fidan.data.ImpactData
import com.erdalgunes.fidan.data.Result

@OptIn(ExperimentalCoroutinesApi::class)
class ImpactViewModelTest {
    
    @Mock
    private lateinit var mockRepository: ImpactRepository
    
    private lateinit var viewModel: ImpactViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is Loading`() = runTest {
        // Given
        whenever(mockRepository.getImpactData()).thenReturn(Result.Loading)
        
        // When
        viewModel = ImpactViewModel(mockRepository)
        
        // Then
        assertTrue(viewModel.uiState.first() is ImpactUiState.Loading)
    }
    
    @Test
    fun `successful data load updates state to Success`() = runTest {
        // Given
        val mockData = ImpactData(
            realTreesPlanted = 100,
            totalDonations = 500.0,
            partnersCount = 3,
            lastUpdated = "Test Update",
            monthlyGrowth = 5.0,
            certificates = 10
        )
        whenever(mockRepository.getImpactData()).thenReturn(Result.Success(mockData))
        
        // When
        viewModel = ImpactViewModel(mockRepository)
        
        // Then
        val state = viewModel.uiState.first()
        assertTrue("State should be Success", state is ImpactUiState.Success)
        assertEquals(mockData, (state as ImpactUiState.Success).data)
    }
    
    @Test
    fun `error during data load updates state to Error`() = runTest {
        // Given
        val errorMessage = "Network error occurred"
        whenever(mockRepository.getImpactData()).thenReturn(Result.Error(errorMessage))
        
        // When
        viewModel = ImpactViewModel(mockRepository)
        
        // Then
        val state = viewModel.uiState.first()
        assertTrue("State should be Error", state is ImpactUiState.Error)
        assertEquals(errorMessage, (state as ImpactUiState.Error).message)
    }
    
    @Test
    fun `refresh calls repository again`() = runTest {
        // Given
        val initialData = ImpactData(
            realTreesPlanted = 100,
            totalDonations = 500.0,
            partnersCount = 3,
            lastUpdated = "Initial",
            monthlyGrowth = 5.0,
            certificates = 10
        )
        val refreshedData = ImpactData(
            realTreesPlanted = 150,
            totalDonations = 750.0,
            partnersCount = 4,
            lastUpdated = "Refreshed",
            monthlyGrowth = 7.0,
            certificates = 15
        )
        
        whenever(mockRepository.getImpactData())
            .thenReturn(Result.Success(initialData))
            .thenReturn(Result.Success(refreshedData))
        
        // When
        viewModel = ImpactViewModel(mockRepository)
        val initialState = viewModel.uiState.first()
        
        viewModel.refresh()
        val refreshedState = viewModel.uiState.first()
        
        // Then
        assertTrue("Initial state should be Success", initialState is ImpactUiState.Success)
        assertTrue("Refreshed state should be Success", refreshedState is ImpactUiState.Success)
        
        assertEquals(150, (refreshedState as ImpactUiState.Success).data.realTreesPlanted)
        assertEquals("Refreshed", refreshedState.data.lastUpdated)
    }
    
    @Test
    fun `error type is correctly determined for timeout errors`() = runTest {
        // Given
        val timeoutErrorMessage = "Request timed out. Please check your connection and try again."
        whenever(mockRepository.getImpactData()).thenReturn(Result.Error(timeoutErrorMessage))
        
        // When
        viewModel = ImpactViewModel(mockRepository)
        
        // Then
        val state = viewModel.uiState.first()
        assertTrue("State should be Error", state is ImpactUiState.Error)
        assertEquals(ErrorType.TIMEOUT, (state as ImpactUiState.Error).errorType)
    }
    
    @Test
    fun `error type is correctly determined for network errors`() = runTest {
        // Given
        val networkErrorMessage = "Network error. Please check your connection and try again."
        whenever(mockRepository.getImpactData()).thenReturn(Result.Error(networkErrorMessage))
        
        // When
        viewModel = ImpactViewModel(mockRepository)
        
        // Then
        val state = viewModel.uiState.first()
        assertTrue("State should be Error", state is ImpactUiState.Error)
        assertEquals(ErrorType.NETWORK, (state as ImpactUiState.Error).errorType)
    }
}