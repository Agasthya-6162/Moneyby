package com.example.moneyby.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupData(
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val savingGoals: List<SavingGoal> = emptyList(),
    val categories: List<Category> = emptyList(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val billReminders: List<BillReminder> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BackupMetaData(
    val appVersion: String,
    val exportDate: Long = System.currentTimeMillis(),
    val device: String = android.os.Build.MODEL
)
