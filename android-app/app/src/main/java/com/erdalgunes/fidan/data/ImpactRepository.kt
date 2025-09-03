package com.erdalgunes.fidan.data

import kotlinx.coroutines.delay
import com.erdalgunes.fidan.config.NetworkConfig
import com.erdalgunes.fidan.config.MockData

data class ImpactData(
    val realTreesPlanted: Int,
    val totalDonations: Double,
    val partnersCount: Int,
    val lastUpdated: String,
    val monthlyGrowth: Double = 0.0,
    val certificates: Int = 0
)

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class ImpactRepository {
    
    suspend fun getImpactData(): Result<ImpactData> {
        return try {
            // Simulate network delay
            delay(NetworkConfig.DEFAULT_TIMEOUT)
            
            // Mock data - replace with real API call
            val data = ImpactData(
                realTreesPlanted = MockData.MOCK_TREES_PLANTED,
                totalDonations = MockData.MOCK_DONATIONS,
                partnersCount = MockData.MOCK_PARTNERS_COUNT,
                lastUpdated = MockData.MOCK_LAST_UPDATED,
                monthlyGrowth = MockData.MOCK_MONTHLY_GROWTH,
                certificates = MockData.MOCK_CERTIFICATES
            )
            
            Result.Success(data)
        } catch (e: Exception) {
            Result.Error("Network timeout. Please check your connection and try again.")
        }
    }
}