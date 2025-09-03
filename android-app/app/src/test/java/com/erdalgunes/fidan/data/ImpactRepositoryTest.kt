package com.erdalgunes.fidan.data

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import com.erdalgunes.fidan.config.MockData

class ImpactRepositoryTest {
    
    private val repository = ImpactRepository()
    
    @Test
    fun `getImpactData returns success with mock data`() = runTest {
        // When
        val result = repository.getImpactData()
        
        // Then
        assertTrue("Result should be Success", result is Result.Success)
        val data = (result as Result.Success).data
        
        assertEquals(MockData.MOCK_TREES_PLANTED, data.realTreesPlanted)
        assertEquals(MockData.MOCK_DONATIONS, data.totalDonations, 0.01)
        assertEquals(MockData.MOCK_PARTNERS_COUNT, data.partnersCount)
        assertEquals(MockData.MOCK_LAST_UPDATED, data.lastUpdated)
        assertEquals(MockData.MOCK_MONTHLY_GROWTH, data.monthlyGrowth, 0.01)
        assertEquals(MockData.MOCK_CERTIFICATES, data.certificates)
    }
    
    @Test
    fun `getImpactData returns valid impact data structure`() = runTest {
        // When
        val result = repository.getImpactData()
        
        // Then
        assertTrue("Result should be Success", result is Result.Success)
        val data = (result as Result.Success).data
        
        // Validate data constraints
        assertTrue("Trees planted should be non-negative", data.realTreesPlanted >= 0)
        assertTrue("Total donations should be non-negative", data.totalDonations >= 0.0)
        assertTrue("Partners count should be positive", data.partnersCount > 0)
        assertTrue("Monthly growth should be reasonable", data.monthlyGrowth >= 0.0 && data.monthlyGrowth <= 100.0)
        assertTrue("Certificates should be non-negative", data.certificates >= 0)
        assertNotNull("Last updated should not be null", data.lastUpdated)
    }
    
    @Test
    fun `impact data has reasonable values`() = runTest {
        // When
        val result = repository.getImpactData()
        
        // Then
        assertTrue("Result should be Success", result is Result.Success)
        val data = (result as Result.Success).data
        
        // Business logic validation
        assertTrue("Should have planted at least some trees", data.realTreesPlanted > 0)
        assertTrue("Should have received some donations", data.totalDonations > 0.0)
        assertTrue("Should have at least one partner", data.partnersCount >= 1)
        assertTrue("Should have some certificates", data.certificates > 0)
        assertTrue("Last updated should not be empty", data.lastUpdated.isNotEmpty())
    }
}