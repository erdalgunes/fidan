package com.erdalgunes.fidan.common

/**
 * A generic wrapper for operations that can succeed or fail.
 * Follows DRY principle by providing consistent error handling across the app.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    /**
     * Returns true if the result is a success.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if the result is an error.
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Returns true if the result is loading.
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Returns the data if the result is a success, null otherwise.
     */
    fun getOrNull(): T? {
        return when (this) {
            is Success -> data
            else -> null
        }
    }

    /**
     * Returns the data if the result is a success, or the default value otherwise.
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T {
        return when (this) {
            is Success -> data
            else -> defaultValue
        }
    }

    /**
     * Executes the given action if the result is a success.
     */
    fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    /**
     * Executes the given action if the result is an error.
     */
    fun onError(action: (Throwable, String?) -> Unit): Result<T> {
        if (this is Error) {
            action(exception, message)
        }
        return this
    }

    /**
     * Maps the data if the result is a success.
     */
    fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> Error(exception, message)
            is Loading -> Loading
        }
    }
}

/**
 * Extension function to safely execute a suspending operation and wrap it in a Result.
 * Provides consistent error handling following KISS principle.
 */
suspend fun <T> safeCall(
    action: suspend () -> T
): Result<T> {
    return try {
        Result.Success(action())
    } catch (e: Exception) {
        Result.Error(
            exception = e,
            message = e.message ?: "An unknown error occurred"
        )
    }
}

/**
 * Extension function to safely execute a regular operation and wrap it in a Result.
 */
fun <T> safeTry(
    action: () -> T
): Result<T> {
    return try {
        Result.Success(action())
    } catch (e: Exception) {
        Result.Error(
            exception = e,
            message = e.message ?: "An unknown error occurred"
        )
    }
}