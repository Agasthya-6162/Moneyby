package com.example.moneyby.data

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "bill_reminders")
data class BillReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val amount: Double,
    val dueDate: Long,
    val category: String,
    val isPaid: Boolean = false,
    val reminderDaysBefore: Int = 1
)
