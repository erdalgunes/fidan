package com.erdalgunes.fidan.common

/**
 * Sealed class representing different UI states.
 * Follows SOLID principles by providing clear state contracts.
 */
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
}

/**
 * Extension function to convert Result to UiState.
 * Promotes code reuse (DRY principle).
 */
fun <T> Result<T>.toUiState(): UiState<T> {
    return when (this) {
        is Result.Loading -> UiState.Loading
        is Result.Success -> UiState.Success(data)
        is Result.Error -> UiState.Error(
            message = message ?: exception.message ?: "Unknown error",
            throwable = exception
        )
    }
}

/**
 * Timer-specific UI states for more granular control.
 * Follows YAGNI principle - only what we need for timer functionality.
 */
sealed class TimerUiState {
    data object Idle : TimerUiState()
    data class Running(
        val timeLeftMillis: Long,
        val totalDurationMillis: Long,
        val progress: Float = 1f - (timeLeftMillis.toFloat() / totalDurationMillis)
    ) : TimerUiState()
    
    data class Paused(
        val timeLeftMillis: Long,
        val totalDurationMillis: Long,
        val progress: Float = 1f - (timeLeftMillis.toFloat() / totalDurationMillis)
    ) : TimerUiState()
    
    data class Completed(
        val totalDurationMillis: Long,
        val wasInterrupted: Boolean = false
    ) : TimerUiState()
    
    data class BackgroundWarning(
        val timeLeftMillis: Long,
        val graceTimeLeftMillis: Long
    ) : TimerUiState()
    
    data class Error(val message: String, val canRecover: Boolean = true) : TimerUiState()
}

/**
 * Forest-specific UI states for tree management.
 */
sealed class ForestUiState {
    data object Loading : ForestUiState()
    data class Success(
        val trees: List<com.erdalgunes.fidan.data.Tree>,
        val totalTrees: Int,
        val completedSessions: Int,
        val isDayTime: Boolean
    ) : ForestUiState()
    
    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val fallbackData: List<com.erdalgunes.fidan.data.Tree>? = null
    ) : ForestUiState()
    
    data class PersistenceWarning(
        val trees: List<com.erdalgunes.fidan.data.Tree>,
        val message: String
    ) : ForestUiState()
}

/**
 * App-wide error types for better error classification.
 * Follows SOLID's Open/Closed principle - easy to extend.
 */
sealed class AppError(
    open val message: String,
    open val throwable: Throwable? = null,
    open val isRecoverable: Boolean = true
) {
    data class NetworkError(
        override val message: String = "Network connection error",
        override val throwable: Throwable? = null
    ) : AppError(message, throwable, true)
    
    data class PersistenceError(
        override val message: String = "Data storage error",
        override val throwable: Throwable? = null,
        val canUseMemoryFallback: Boolean = true
    ) : AppError(message, throwable, canUseMemoryFallback)
    
    data class TimerError(
        override val message: String = "Timer operation failed",
        override val throwable: Throwable? = null
    ) : AppError(message, throwable, true)
    
    data class ValidationError(
        override val message: String,
        override val throwable: Throwable? = null
    ) : AppError(message, throwable, false)
    
    data class UnknownError(
        override val message: String = "An unexpected error occurred",
        override val throwable: Throwable? = null
    ) : AppError(message, throwable, true)
}

/**
 * Extension to convert exceptions to AppError types.
 * Provides consistent error mapping across the app.
 */
fun Throwable.toAppError(): AppError {
    return when (this) {
        is java.io.IOException -> AppError.PersistenceError(
            message = "Storage operation failed: ${message}",
            throwable = this
        )
        is SecurityException -> AppError.ValidationError(
            message = "Security error: ${message}",
            throwable = this
        )
        is IllegalArgumentException, is IllegalStateException -> AppError.ValidationError(
            message = "Invalid operation: ${message}",
            throwable = this
        )
        else -> AppError.UnknownError(
            message = message ?: "Unknown error occurred",
            throwable = this
        )
    }
}