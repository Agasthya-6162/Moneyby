package com.example.moneyby.util

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ──────────────────────────────────────────────────────────────────────────────
// Date Extensions
// ──────────────────────────────────────────────────────────────────────────────

fun Long.formatDate(pattern: String = "dd MMM yyyy"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

fun Long.formatDateTime(pattern: String = "dd MMM yyyy, HH:mm"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

fun Long.formatTime(pattern: String = "HH:mm"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

fun Long.isToday(): Boolean {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val timestamp = Calendar.getInstance().apply {
        timeInMillis = this@isToday
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return timestamp.timeInMillis == today.timeInMillis
}

fun Long.isThisMonth(): Boolean {
    val thisMonth = Calendar.getInstance()
    val timestamp = Calendar.getInstance().apply { timeInMillis = this@isThisMonth }
    return thisMonth.get(Calendar.MONTH) == timestamp.get(Calendar.MONTH) &&
           thisMonth.get(Calendar.YEAR) == timestamp.get(Calendar.YEAR)
}

fun Long.isThisYear(): Boolean {
    val thisYear = Calendar.getInstance()
    val timestamp = Calendar.getInstance().apply { timeInMillis = this@isThisYear }
    return thisYear.get(Calendar.YEAR) == timestamp.get(Calendar.YEAR)
}

// ──────────────────────────────────────────────────────────────────────────────
// Currency/Amount Extensions
// ──────────────────────────────────────────────────────────────────────────────

fun Double.formatCurrency(
    currencySymbol: String = "₹",
    decimalPlaces: Int = 2
): String {
    val format = "%.${decimalPlaces}f"
    return "$currencySymbol ${format.format(this)}"
}

fun Double.toAbsoluteValue(): Double = abs(this)

fun Double.isPositive(): Boolean = this > 0

fun Double.isNegative(): Boolean = this < 0

fun Double.isZero(): Boolean = this == 0.0

// Safe arithmetic operations
fun Double.safeAdd(other: Double): Double = (this * 100 + other * 100) / 100

fun Double.safeSubtract(other: Double): Double = (this * 100 - other * 100) / 100

fun Double.safeMultiply(other: Double): Double = (this * other * 100).toLong() / 100.0

// ──────────────────────────────────────────────────────────────────────────────
// String Extensions
// ──────────────────────────────────────────────────────────────────────────────

fun String.isValidEmail(): Boolean {
    return this.contains("@") && this.contains(".") && this.length > 5
}

fun String.isValidPhoneNumber(): Boolean {
    return this.length >= 10 && this.all { it.isDigit() }
}

fun String.truncate(maxLength: Int): String {
    return if (this.length > maxLength) {
        this.substring(0, maxLength) + "..."
    } else {
        this
    }
}

fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}

fun String.removeWhitespace(): String = this.replace(Regex("\\s"), "")

// ──────────────────────────────────────────────────────────────────────────────
// List Extensions
// ──────────────────────────────────────────────────────────────────────────────

fun <T> List<T>.second(): T? = if (this.size > 1) this[1] else null

fun <T> List<T>.third(): T? = if (this.size > 2) this[2] else null

fun <T> List<T>.isEmpty(): Boolean = this.size == 0

fun <T> List<T>.isNotEmpty(): Boolean = this.size > 0

fun <T> List<T>.sumByDouble(selector: (T) -> Double): Double {
    return this.fold(0.0) { acc, element ->
        acc.safeAdd(selector(element))
    }
}

fun <T> List<T>.groupByAndCount(keySelector: (T) -> String): Map<String, Int> {
    return this.groupBy(keySelector).mapValues { it.value.size }
}

// ──────────────────────────────────────────────────────────────────────────────
// Context Extensions
// ──────────────────────────────────────────────────────────────────────────────

fun Context.getVersionName(): String {
    return try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }
}

fun Context.getVersionCode(): Long {
    return try {
        packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
    } catch (e: Exception) {
        1L
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Compose Extensions
// ──────────────────────────────────────────────────────────────────────────────

suspend fun SnackbarHostState.showMessage(
    message: String,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short
) {
    this.showSnackbar(
        message = message,
        actionLabel = actionLabel,
        duration = duration
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Number Extensions
// ──────────────────────────────────────────────────────────────────────────────

fun Int.toDp(): Float = this.toFloat()

fun Float.roundTo(decimals: Int): Float {
    var multiplier = 1f
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

fun Int.toPercentage(total: Int): Float {
    return if (total == 0) 0f else (this.toFloat() / total) * 100
}
