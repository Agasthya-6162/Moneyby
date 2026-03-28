package com.example.moneyby.domain.repository

import com.example.moneyby.data.Account
import com.example.moneyby.data.BackupData
import com.example.moneyby.data.BillReminder
import com.example.moneyby.data.Budget
import com.example.moneyby.data.Category
import com.example.moneyby.data.PendingTransaction
import com.example.moneyby.data.RecurringTransaction
import com.example.moneyby.data.SavingGoal
import com.example.moneyby.data.Transaction
import kotlinx.coroutines.flow.Flow

import com.example.moneyby.util.Result

interface TransactionRepository {
    // Transactions
    fun getAllTransactionsStream(): Flow<List<Transaction>>
    suspend fun getTransactionStream(id: Int): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Result<Unit>
    suspend fun deleteTransaction(transaction: Transaction): Result<Unit>
    suspend fun updateTransaction(transaction: Transaction): Result<Unit>

    // Accounts
    fun getAllAccountsStream(): Flow<List<Account>>
    suspend fun getAccountStream(id: Int): Account?
    suspend fun insertAccount(account: Account): Result<Unit>
    suspend fun updateAccount(account: Account): Result<Unit>
    suspend fun deleteAccount(account: Account): Result<Unit>

    // Budgets
    fun getBudgetsForMonthStream(monthYear: String): Flow<List<Budget>>
    suspend fun insertBudget(budget: Budget): Result<Unit>
    suspend fun updateBudget(budget: Budget): Result<Unit>
    suspend fun deleteBudget(budget: Budget): Result<Unit>

    // Saving Goals
    fun getAllSavingGoalsStream(): Flow<List<SavingGoal>>
    suspend fun getSavingGoalStream(id: Int): SavingGoal?
    suspend fun insertSavingGoal(savingGoal: SavingGoal): Result<Unit>
    suspend fun updateSavingGoal(savingGoal: SavingGoal): Result<Unit>
    suspend fun deleteSavingGoal(savingGoal: SavingGoal): Result<Unit>

    // Categories
    fun getAllCategoriesStream(): Flow<List<Category>>
    fun getCategoryStream(id: Int): Flow<Category?>
    suspend fun insertCategory(category: Category): Result<Unit>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun deleteCategory(category: Category): Result<Unit>

    // Recurring Transactions
    fun getAllRecurringTransactionsStream(): Flow<List<RecurringTransaction>>
    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction): Result<Unit>
    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction): Result<Unit>
    suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction): Result<Unit>

    // Goal History
    fun getTransactionsByGoalIdStream(goalId: Int): Flow<List<Transaction>>

    // Backup & Restore
    suspend fun getBackupData(): BackupData
    suspend fun restoreData(data: BackupData): Result<Unit>

    // Bill Reminders
    fun getAllBillRemindersStream(): Flow<List<BillReminder>>
    suspend fun insertBillReminder(billReminder: BillReminder): Result<Unit>
    suspend fun updateBillReminder(billReminder: BillReminder): Result<Unit>
    suspend fun deleteBillReminder(billReminder: BillReminder): Result<Unit>

    // Auto-detection
    suspend fun getTransactionByHash(hash: String): Transaction?
    suspend fun getAccountBySuffix(suffix: String): Account?

    // Pending Transactions
    fun getAllPendingTransactionsStream(): Flow<List<PendingTransaction>>
    suspend fun insertPendingTransaction(pending: PendingTransaction): Result<Unit>
    suspend fun deletePendingTransaction(pending: PendingTransaction): Result<Unit>
    suspend fun getPendingTransactionByHash(hash: String): PendingTransaction?
}
