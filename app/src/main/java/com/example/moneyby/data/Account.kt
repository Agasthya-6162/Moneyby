package com.example.moneyby.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val type: String, // "Cash", "Bank", "Credit"
    val initialBalance: Double,
    val accountNumberSuffix: String? = null // Last 4 digits for auto-matching
)

