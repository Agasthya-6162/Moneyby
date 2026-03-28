package com.example.moneyby.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BillReminderDao {
    @Query("SELECT * FROM bill_reminders ORDER BY dueDate ASC")
    fun getAllBillReminders(): Flow<List<BillReminder>>

    @Query("SELECT * FROM bill_reminders")
    suspend fun getAllBillRemindersOnce(): List<BillReminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillReminder(billReminder: BillReminder)

    @Update
    suspend fun updateBillReminder(billReminder: BillReminder)

    @Delete
    suspend fun deleteBillReminder(billReminder: BillReminder)

    @Query("DELETE FROM bill_reminders")
    suspend fun deleteAllBillReminders()
}
