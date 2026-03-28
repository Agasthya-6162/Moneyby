package com.example.moneyby.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["transactionHash"], unique = true)
    ]

)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val category: String,
    val date: Long,
    val type: String, // "Income", "Expense", "Transfer"
    val accountId: Int,
    val toAccountId: Int? = null, // For Transfer type
    val goalId: Int? = null, // For Saving Goal history
    val notes: String = "",
    val transactionHash: String? = null, // For deduplication
    val isAutoDetected: Boolean = false // To distinguish auto-added transactions
)

