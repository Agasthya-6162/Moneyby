package com.example.moneyby.util

import android.util.Log
import com.example.moneyby.BuildConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Logging utility that respects production environment.
 * Never logs sensitive financial data.
 */
object Logger {
    
    private const val DEFAULT_TAG = "MoneybyApp"
    private val isDebug = BuildConfig.DEBUG
    
    // Log levels
    enum class Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }
    
    /**
     * Log verbose message (debug builds only).
     */
    fun v(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (isDebug) {
            Log.v(tag, formatMessage(message), throwable)
        }
    }
    
    /**
     * Log debug message (debug builds only).
     */
    fun d(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (isDebug) {
            Log.d(tag, formatMessage(message), throwable)
        }
    }
    
    /**
     * Log info message (debug builds only for production safety).
     */
    fun i(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (isDebug) {
            Log.i(tag, formatMessage(message), throwable)
        }
    }
    
    /**
     * Log warning message (debug builds only for production safety).
     */
    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (isDebug) {
            Log.w(tag, formatMessage(message), throwable)
        }
    }
    
    /**
     * Log error message (debug builds only for production safety).
     */
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (isDebug) {
            Log.e(tag, formatMessage(message), throwable)
        }
    }
    
    /**
     * Log transaction event (safe for production).
     */
    fun logTransaction(
        tag: String = DEFAULT_TAG,
        action: String,
        category: String,
        type: String
    ) {
        i(tag, "Transaction: action=$action, category=$category, type=$type")
    }
    
    /**
     * Log backup event.
     */
    fun logBackup(tag: String = DEFAULT_TAG, action: String, success: Boolean) {
        i(tag, "Backup: action=$action, success=$success")
    }
    
    /**
     * Log authentication event.
     */
    fun logAuth(tag: String = DEFAULT_TAG, action: String, success: Boolean) {
        i(tag, "Auth: action=$action, success=$success")
    }
    
    /**
     * Log worker execution.
     */
    fun logWorker(tag: String = DEFAULT_TAG, workerName: String, status: String) {
        i(tag, "Worker: name=$workerName, status=$status")
    }
    
    /**
     * Log database operation.
     */
    fun logDatabase(tag: String = DEFAULT_TAG, operation: String, table: String, success: Boolean) {
        d(tag, "Database: operation=$operation, table=$table, success=$success")
    }
    
    /**
     * Log network operation.
     */
    fun logNetwork(tag: String = DEFAULT_TAG, operation: String, statusCode: Int? = null, duration: Long? = null) {
        i(tag, "Network: operation=$operation, statusCode=$statusCode, duration=${duration}ms")
    }
    
    /**
     * Log UI event.
     */
    fun logUIEvent(tag: String = DEFAULT_TAG, screen: String, event: String) {
        d(tag, "UIEvent: screen=$screen, event=$event")
    }
    
    /**
     * Log performance metric.
     */
    fun logPerformance(tag: String = DEFAULT_TAG, operation: String, durationMs: Long, threshold: Long = 1000) {
        val level = if (durationMs > threshold) Level.WARN else Level.INFO
        val message = "Performance: operation=$operation, duration=${durationMs}ms"
        when (level) {
            Level.WARN -> w(tag, message)
            else -> i(tag, message)
        }
    }
    
    /**
     * Log exception safely without exposing sensitive data.
     */
    fun logException(
        tag: String = DEFAULT_TAG,
        message: String,
        exception: Throwable,
        context: String = ""
    ) {
        val safeMessage = sanitizeExceptionMessage(exception.message ?: "Unknown error")
        e(tag, "$message: $safeMessage ${if (context.isNotEmpty()) "($context)" else ""}", exception)
    }
    
    /**
     * Log crash event.
     */
    fun logCrash(tag: String = DEFAULT_TAG, throwable: Throwable) {
        e(tag, "CRASH: ${sanitizeExceptionMessage(throwable.message ?: "Unknown crash")}", throwable)
    }
    
    /**
     * Format log message with timestamp.
     */
    private fun formatMessage(message: String): String {
        return if (isDebug) {
            "[${LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))}] $message"
        } else {
            message
        }
    }
    
    /**
     * Remove sensitive data from exception messages.
     */
    private fun sanitizeExceptionMessage(message: String): String {
        // Remove common sensitive patterns
        var sanitized = message
        // Remove email addresses
        sanitized = sanitized.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "[EMAIL]")
        // Remove phone numbers (10+ digits)
        sanitized = sanitized.replace(Regex("\\d{10,}"), "[PHONE]")
        // Remove account numbers
        sanitized = sanitized.replace(Regex("\\d{16,}"), "[ACCOUNT]")
        return sanitized
    }
}

/**
 * Measure execution time of a block.
 */
inline fun <T> measureTime(
    tag: String = "MoneybyApp",
    operation: String = "Operation",
    threshold: Long = 1000,
    block: () -> T
): T {
    val start = System.currentTimeMillis()
    return try {
        block()
    } finally {
        val duration = System.currentTimeMillis() - start
        Logger.logPerformance(tag, operation, duration, threshold)
    }
}

/**
 * Measure execution time of a suspend block.
 */
suspend inline fun <T> measureTimeSuspend(
    tag: String = "MoneybyApp",
    operation: String = "Operation",
    threshold: Long = 1000,
    block: suspend () -> T
): T {
    val start = System.currentTimeMillis()
    return try {
        block()
    } finally {
        val duration = System.currentTimeMillis() - start
        Logger.logPerformance(tag, operation, duration, threshold)
    }
}
