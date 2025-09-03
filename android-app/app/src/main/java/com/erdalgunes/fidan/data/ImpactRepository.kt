package com.erdalgunes.fidan.data

import kotlinx.coroutines.delay

data class ImpactData(
    val realTreesPlanted: Int,
    val totalDonations: Double,
    val partnersCount: Int,
    val lastUpdated: String
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
            delay(1000)
            
            // Mock data - replace with real API call
            val data = ImpactData(
                realTreesPlanted = 1247,
                totalDonations = 3741.50,
                partnersCount = 3,
                lastUpdated = "January 2025"
            )
            
            Result.Success(data)
        } catch (e: Exception) {
            Result.Error("Failed to load impact data: ${e.message}")
        }
    }
}