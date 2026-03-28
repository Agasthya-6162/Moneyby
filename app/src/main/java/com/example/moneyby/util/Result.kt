package com.example.moneyby.util

/**
 * A sealed class to represent the result of an operation that can succeed or fail.
 * This provides type-safe error handling throughout the app.
 */
sealed class Result<out T> {
    /**
     * Operation succeeded with data.
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Operation failed with an error.
     */
    data class Error(val exception: Throwable, val message: String = exception.message ?: "Unknown error") : Result<Nothing>()

    /**
     * Operation is in progress.
     */
    object Loading : Result<Nothing>()

    /**
     * Check if result is success.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Check if result is error.
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Check if result is loading.
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Get data or null.
     */
    fun getOrNull(): T? = (this as? Success)?.data

    /**
     * Get exception or null.
     */
    fun getExceptionOrNull(): Throwable? = (this as? Error)?.exception

    /**
     * Get message or null.
     */
    fun getMessageOrNull(): String? = (this as? Error)?.message

    /**
     * Transform success data, keep error as-is.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            Loading -> Loading
        }
    }

    /**
     * Transform error, keep success as-is.
     */
    inline fun mapError(transform: (Error) -> Unit): Result<T> {
        if (this is Error) {
            transform(this)
        }
        return this
    }

    /**
     * Chain operations - flatMap pattern.
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> {
        return when (this) {
            is Success -> transform(data)
            is Error -> this
            Loading -> Loading
        }
    }

    /**
     * Execute block based on result type.
     */
    inline fun fold(
        onSuccess: (T) -> Unit,
        onError: (Error) -> Unit,
        onLoading: () -> Unit = {}
    ) {
        when (this) {
            is Success -> onSuccess(data)
            is Error -> onError(this)
            Loading -> onLoading()
        }
    }
}

/**
 * Convert kotlin Result to our Result type.
 */
fun <T> kotlin.Result<T>.toAppResult(): Result<T> {
    val exception = exceptionOrNull()
    return if (exception != null) {
        Result.Error(exception, exception.message ?: "Operation failed")
    } else {
        val value = getOrNull()
        @Suppress("UNCHECKED_CAST")
        Result.Success(value as T)
    }
}

/**
 * Run a suspend function and wrap result.
 */
suspend fun <T> runCatching(block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e)
    }
}

/**
 * Executes a block and wraps it in a Result, providing user-friendly messages for common errors.
 */
suspend fun <T> safeExecute(
    tag: String = "App", 
    action: String = "Operation",
    block: suspend () -> T
): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: android.database.SQLException) {
        android.util.Log.e(tag, "Database error during $action", e)
        Result.Error(e, "Bhai, database me koi dikkat aa gayi hai. Phir se try karein.")
    } catch (e: java.io.IOException) {
        android.util.Log.e(tag, "IO error during $action", e)
        Result.Error(e, "Storage me file save nahi ho paayi. Space check karein.")
    } catch (e: SecurityException) {
        android.util.Log.e(tag, "Security error during $action", e)
        Result.Error(e, "Permission nahi mili. Settings me jaakar permissions allow karein.")
    } catch (e: Exception) {
        android.util.Log.e(tag, "Unknown error during $action", e)
        Result.Error(e, e.message ?: "Kuch galat ho gaya. Phir se koshish karein.")
    }
}
