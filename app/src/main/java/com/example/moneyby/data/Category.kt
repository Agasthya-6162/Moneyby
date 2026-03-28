package com.example.moneyby.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val iconName: String = "category", // Name of the icon drawable
    val color: Int = 0XFF000000.toInt(), // Color as ARGB Int
    val type: String = "Expense" // "Income" or "Expense"
)
