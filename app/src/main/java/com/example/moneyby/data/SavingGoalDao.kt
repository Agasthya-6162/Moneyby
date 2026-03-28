package com.example.moneyby.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingGoalDao {
    @Query("SELECT * FROM saving_goals ORDER BY isCompleted ASC, targetDate ASC")
    fun getAllSavingGoals(): Flow<List<SavingGoal>>

    @Query("SELECT * FROM saving_goals WHERE id = :id")
    suspend fun getSavingGoalById(id: Int): SavingGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingGoal(savingGoal: SavingGoal)

    @Update
    suspend fun updateSavingGoal(savingGoal: SavingGoal)

    @Delete
    suspend fun deleteSavingGoal(savingGoal: SavingGoal)

    @Query("SELECT * FROM saving_goals")
    suspend fun getAllSavingGoalsOnce(): List<SavingGoal>

    @Query("DELETE FROM saving_goals")
    suspend fun deleteAllSavingGoals()
}
