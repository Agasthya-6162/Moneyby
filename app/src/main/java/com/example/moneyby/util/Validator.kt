package com.example.moneyby.util

/**
 * Validation utilities for ensuring data integrity throughout the app.
 */
object Validator {
    
    /**
     * Validate transaction amount.
     * Must be positive number with max 2 decimal places.
     */
    fun validateAmount(amount: String): ValidationResult {
        return when {
            amount.isBlank() -> ValidationResult.Error("Amount cannot be empty")
            amount.toDoubleOrNull() == null -> ValidationResult.Error("Amount must be a valid number")
            amount.toDouble() <= 0 -> ValidationResult.Error("Amount must be greater than 0")
            amount.toDouble() > 10_000_000 -> ValidationResult.Error("Amount exceeds maximum limit (₹1 Cr)")
            !amount.matches(Regex("^\\d+(\\.\\d{1,2})?$")) -> ValidationResult.Error("Amount can have max 2 decimal places")
            else -> ValidationResult.Success(amount.toDouble())
        }
    }

    /**
     * Validate category name.
     */
    fun validateCategory(category: String): ValidationResult {
        return when {
            category.isBlank() -> ValidationResult.Error("Category name cannot be empty")
            category.length < 2 -> ValidationResult.Error("Category name must be at least 2 characters")
            category.length > 50 -> ValidationResult.Error("Category name cannot exceed 50 characters")
            !category.matches(Regex("^[a-zA-Z0-9 &-]*$")) -> ValidationResult.Error("Category contains invalid characters")
            else -> ValidationResult.Success(category)
        }
    }

    /**
     * Validate transaction notes.
     */
    fun validateNotes(notes: String): ValidationResult {
        return when {
            notes.length > 500 -> ValidationResult.Error("Notes cannot exceed 500 characters")
            else -> ValidationResult.Success(notes)
        }
    }

    /**
     * Validate budget limit.
     */
    fun validateBudgetLimit(limit: String): ValidationResult {
        return when {
            limit.isBlank() -> ValidationResult.Error("Budget limit cannot be empty")
            limit.toDoubleOrNull() == null -> ValidationResult.Error("Budget limit must be a valid number")
            limit.toDouble() <= 0 -> ValidationResult.Error("Budget limit must be greater than 0")
            else -> ValidationResult.Success(limit.toDouble())
        }
    }

    /**
     * Validate PIN (4-6 digits).
     */
    fun validatePIN(pin: String): ValidationResult {
        return when {
            pin.length < 4 -> ValidationResult.Error("PIN must be at least 4 digits")
            pin.length > 6 -> ValidationResult.Error("PIN cannot exceed 6 digits")
            !pin.all { it.isDigit() } -> ValidationResult.Error("PIN must contain only numbers")
            else -> ValidationResult.Success(pin)
        }
    }

    /**
     * Validate password strength.
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.length < 6 -> ValidationResult.Error("Password must be at least 6 characters")
            password.length > 128 -> ValidationResult.Error("Password cannot exceed 128 characters")
            !password.any { it.isUpperCase() } -> ValidationResult.Error("Password must contain at least one uppercase letter")
            !password.any { it.isDigit() } -> ValidationResult.Error("Password must contain at least one digit")
            else -> ValidationResult.Success(password)
        }
    }

    /**
     * Validate account name.
     */
    fun validateAccountName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Account name cannot be empty")
            name.length < 2 -> ValidationResult.Error("Account name must be at least 2 characters")
            name.length > 50 -> ValidationResult.Error("Account name cannot exceed 50 characters")
            else -> ValidationResult.Success(name)
        }
    }

    /**
     * Validate account number suffix (last 4 digits).
     */
    fun validateAccountSuffix(suffix: String): ValidationResult {
        return when {
            suffix.isBlank() -> ValidationResult.Error("Account suffix cannot be empty")
            suffix.length != 4 -> ValidationResult.Error("Account suffix must be exactly 4 digits")
            !suffix.all { it.isDigit() } -> ValidationResult.Error("Account suffix must contain only numbers")
            else -> ValidationResult.Success(suffix)
        }
    }

    /**
     * Validate initial balance.
     */
    fun validateInitialBalance(balance: String): ValidationResult {
        return when {
            balance.isBlank() -> ValidationResult.Error("Balance cannot be empty")
            balance.toDoubleOrNull() == null -> ValidationResult.Error("Balance must be a valid number")
            else -> ValidationResult.Success(balance.toDouble())
        }
    }

    /**
     * Validate goal name.
     */
    fun validateGoalName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Goal name cannot be empty")
            name.length < 2 -> ValidationResult.Error("Goal name must be at least 2 characters")
            name.length > 100 -> ValidationResult.Error("Goal name cannot exceed 100 characters")
            else -> ValidationResult.Success(name)
        }
    }

    /**
     * Validate goal target amount.
     */
    fun validateGoalTarget(target: String): ValidationResult {
        return when {
            target.isBlank() -> ValidationResult.Error("Target amount cannot be empty")
            target.toDoubleOrNull() == null -> ValidationResult.Error("Target amount must be a valid number")
            target.toDouble() <= 0 -> ValidationResult.Error("Target amount must be greater than 0")
            else -> ValidationResult.Success(target.toDouble())
        }
    }

    /**
     * Validate bill reminder name.
     */
    fun validateBillName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Bill name cannot be empty")
            name.length < 2 -> ValidationResult.Error("Bill name must be at least 2 characters")
            name.length > 100 -> ValidationResult.Error("Bill name cannot exceed 100 characters")
            else -> ValidationResult.Success(name)
        }
    }

    /**
     * Validate bill amount.
     */
    fun validateBillAmount(amount: String): ValidationResult {
        return validateAmount(amount)
    }

    /**
     * Validate reminder days before.
     */
    fun validateReminderDays(days: String): ValidationResult {
        return when {
            days.isBlank() -> ValidationResult.Error("Reminder days cannot be empty")
            days.toIntOrNull() == null -> ValidationResult.Error("Reminder days must be a valid number")
            days.toInt() < 0 -> ValidationResult.Error("Reminder days cannot be negative")
            days.toInt() > 365 -> ValidationResult.Error("Reminder days cannot exceed 365")
            else -> ValidationResult.Success(days.toInt())
        }
    }
}

/**
 * Result of validation operation.
 */
sealed class ValidationResult {
    /**
     * Validation passed.
     */
    data class Success(val value: Any) : ValidationResult()

    /**
     * Validation failed with error message.
     */
    data class Error(val message: String) : ValidationResult()

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    val errorMessage: String?
        get() = (this as? Error)?.message

    fun getOrNull(): Any? = (this as? Success)?.value
}
