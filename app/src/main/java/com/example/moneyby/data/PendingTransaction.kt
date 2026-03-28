package com.example.moneyby.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "pending_transactions",
    indices = [Index(value = ["transactionHash"], unique = true)]
)

data class PendingTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val category: String,
    val date: Long,
    val type: String, // "Income", "Expense"
    val accountSuffix: String? = null,
    val merchant: String? = null,
    val rawText: String = "",
    val transactionHash: String // For deduplication
)
