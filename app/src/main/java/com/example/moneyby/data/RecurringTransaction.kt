package com.example.moneyby.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "recurring_transactions")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = "",
    val amount: Double,
    val category: String,
    val type: String, // "Income" or "Expense"
    val accountId: Int,
    val frequency: String, // "Daily", "Weekly", "Monthly"
    val nextRunDate: Long = 0L,
    val isActive: Boolean = true,
    val lastProcessedDate: Long = 0L,
    val notes: String = ""
)
