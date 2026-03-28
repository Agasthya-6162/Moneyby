package com.example.moneyby.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsOnce(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE goalId = :goalId ORDER BY date DESC")
    fun getTransactionsByGoalId(goalId: Int): Flow<List<Transaction>>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT * FROM transactions WHERE transactionHash = :hash LIMIT 1")
    suspend fun getTransactionByHash(hash: String): Transaction?
}

@Dao
interface PendingTransactionDao {
    @Query("SELECT * FROM pending_transactions ORDER BY date DESC")
    fun getAllPendingTransactions(): Flow<List<PendingTransaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPending(pending: PendingTransaction)

    @Delete
    suspend fun deletePending(pending: PendingTransaction)

    @Query("SELECT * FROM pending_transactions WHERE transactionHash = :hash LIMIT 1")
    suspend fun getPendingByHash(hash: String): PendingTransaction?

    @Query("DELETE FROM pending_transactions")
    suspend fun deleteAllPending()
}


