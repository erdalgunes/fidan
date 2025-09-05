package com.erdalgunes.fidan.util

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handling following SOLID principles.
 * DRY: Single place for all error handling logic.
 * KISS: Simple error categorization and logging.
 */
@Singleton
class ErrorHandler @Inject constructor() {
    
    companion object {
        private const val TAG = "FidanError"
    }
    
    sealed class FidanError(
        val message: String,
        val cause: Throwable? = null,
        val isRecoverable: Boolean = true
    ) {
        class NetworkError(message: String, cause: Throwable? = null) : 
            FidanError(message, cause, true)
        
        class DatabaseError(message: String, cause: Throwable? = null) : 
            FidanError(message, cause, true)
        
        class TimerError(message: String, cause: Throwable? = null) : 
            FidanError(message, cause, true)
        
        class PermissionError(message: String, cause: Throwable? = null) : 
            FidanError(message, cause, false)
        
        class UnknownError(message: String, cause: Throwable? = null) : 
            FidanError(message, cause, false)
    }
    
    fun handleError(error: FidanError, callback: ((FidanError) -> Unit)? = null) {
        // Log error based on severity
        when (error) {
            is FidanError.NetworkError -> {
                Log.w(TAG, "Network error: ${error.message}", error.cause)
            }
            is FidanError.DatabaseError -> {
                Log.e(TAG, "Database error: ${error.message}", error.cause)
            }
            is FidanError.TimerError -> {
                Log.e(TAG, "Timer error: ${error.message}", error.cause)
            }
            is FidanError.PermissionError -> {
                Log.e(TAG, "Permission error: ${error.message}", error.cause)
            }
            is FidanError.UnknownError -> {
                Log.e(TAG, "Unknown error: ${error.message}", error.cause)
            }
        }
        
        // Execute callback if provided
        callback?.invoke(error)
    }
    
    fun getCoroutineExceptionHandler(
        onError: (Throwable) -> Unit = {}
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            val error = mapThrowableToError(throwable)
            handleError(error)
            onError(throwable)
        }
    }
    
    private fun mapThrowableToError(throwable: Throwable): FidanError {
        return when (throwable) {
            is java.net.UnknownHostException,
            is java.net.SocketTimeoutException -> {
                FidanError.NetworkError("Network connection error", throwable)
            }
            is android.database.SQLException -> {
                FidanError.DatabaseError("Database operation failed", throwable)
            }
            is SecurityException -> {
                FidanError.PermissionError("Permission denied", throwable)
            }
            else -> {
                FidanError.UnknownError("An unexpected error occurred", throwable)
            }
        }
    }
    
    fun getUserFriendlyMessage(error: FidanError): String {
        return when (error) {
            is FidanError.NetworkError -> 
                "Please check your internet connection and try again"
            is FidanError.DatabaseError -> 
                "Unable to save data. Please try again"
            is FidanError.TimerError -> 
                "Timer error occurred. Please restart the session"
            is FidanError.PermissionError -> 
                "Permission required to continue. Please check app settings"
            is FidanError.UnknownError -> 
                "Something went wrong. Please try again"
        }
    }
}