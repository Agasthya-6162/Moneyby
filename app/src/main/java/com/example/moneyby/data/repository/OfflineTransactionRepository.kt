package com.example.moneyby.data.repository

import com.example.moneyby.data.*
import com.example.moneyby.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

import androidx.room.withTransaction

import com.example.moneyby.util.Result
import com.example.moneyby.util.safeExecute

class OfflineTransactionRepository(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val pendingTransactionDao: PendingTransactionDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao,
    private val savingGoalDao: SavingGoalDao,
    private val categoryDao: CategoryDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val billReminderDao: BillReminderDao
) : TransactionRepository {

    override fun getAllTransactionsStream(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    override suspend fun getTransactionStream(id: Int): Transaction? = transactionDao.getTransactionById(id)
    
    override suspend fun insertTransaction(transaction: Transaction): Result<Unit> = 
        safeExecute("Repo", "Insert Transaction") { transactionDao.insertTransaction(transaction) }
        
    override suspend fun deleteTransaction(transaction: Transaction): Result<Unit> = 
        safeExecute("Repo", "Delete Transaction") { transactionDao.deleteTransaction(transaction) }
        
    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> = 
        safeExecute("Repo", "Update Transaction") { transactionDao.updateTransaction(transaction) }

    override fun getAllAccountsStream(): Flow<List<Account>> = accountDao.getAllAccounts()
    override suspend fun getAccountStream(id: Int): Account? = accountDao.getAccountById(id)
    
    override suspend fun insertAccount(account: Account): Result<Unit> = 
        safeExecute("Repo", "Insert Account") { accountDao.insertAccount(account) }
        
    override suspend fun updateAccount(account: Account): Result<Unit> = 
        safeExecute("Repo", "Update Account") { accountDao.updateAccount(account) }
        
    override suspend fun deleteAccount(account: Account): Result<Unit> = 
        safeExecute("Repo", "Delete Account") { accountDao.deleteAccount(account) }

    override fun getBudgetsForMonthStream(monthYear: String): Flow<List<Budget>> = budgetDao.getBudgetsForMonth(monthYear)
    
    override suspend fun insertBudget(budget: Budget): Result<Unit> = 
        safeExecute("Repo", "Insert Budget") { budgetDao.insertBudget(budget) }
        
    override suspend fun updateBudget(budget: Budget): Result<Unit> = 
        safeExecute("Repo", "Update Budget") { budgetDao.updateBudget(budget) }
        
    override suspend fun deleteBudget(budget: Budget): Result<Unit> = 
        safeExecute("Repo", "Delete Budget") { budgetDao.deleteBudget(budget) }

    override fun getAllSavingGoalsStream(): Flow<List<SavingGoal>> = savingGoalDao.getAllSavingGoals()
    override suspend fun getSavingGoalStream(id: Int): SavingGoal? = savingGoalDao.getSavingGoalById(id)
    
    override suspend fun insertSavingGoal(savingGoal: SavingGoal): Result<Unit> = 
        safeExecute("Repo", "Insert Goal") { savingGoalDao.insertSavingGoal(savingGoal) }
        
    override suspend fun updateSavingGoal(savingGoal: SavingGoal): Result<Unit> = 
        safeExecute("Repo", "Update Goal") { savingGoalDao.updateSavingGoal(savingGoal) }
        
    override suspend fun deleteSavingGoal(savingGoal: SavingGoal): Result<Unit> = 
        safeExecute("Repo", "Delete Goal") { savingGoalDao.deleteSavingGoal(savingGoal) }

    override fun getAllCategoriesStream(): Flow<List<Category>> = categoryDao.getAllCategories()
    override fun getCategoryStream(id: Int): Flow<Category?> = categoryDao.getCategory(id)
    
    override suspend fun insertCategory(category: Category): Result<Unit> = 
        safeExecute("Repo", "Insert Category") { categoryDao.insert(category) }
        
    override suspend fun updateCategory(category: Category): Result<Unit> = 
        safeExecute("Repo", "Update Category") { categoryDao.update(category) }
        
    override suspend fun deleteCategory(category: Category): Result<Unit> = 
        safeExecute("Repo", "Delete Category") { categoryDao.delete(category) }

    override fun getAllRecurringTransactionsStream(): Flow<List<RecurringTransaction>> = recurringTransactionDao.getAllRecurringTransactions()
    
    override suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction): Result<Unit> = 
        safeExecute("Repo", "Insert Recurring") { recurringTransactionDao.insertRecurringTransaction(recurringTransaction) }
        
    override suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction): Result<Unit> = 
        safeExecute("Repo", "Update Recurring") { recurringTransactionDao.updateRecurringTransaction(recurringTransaction) }
        
    override suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction): Result<Unit> = 
        safeExecute("Repo", "Delete Recurring") { recurringTransactionDao.deleteRecurringTransaction(recurringTransaction) }

    override fun getTransactionsByGoalIdStream(goalId: Int): Flow<List<Transaction>> = transactionDao.getTransactionsByGoalId(goalId)

    override fun getAllBillRemindersStream(): Flow<List<BillReminder>> = billReminderDao.getAllBillReminders()
    
    override suspend fun insertBillReminder(billReminder: BillReminder): Result<Unit> = 
        safeExecute("Repo", "Insert Reminder") { billReminderDao.insertBillReminder(billReminder) }
        
    override suspend fun updateBillReminder(billReminder: BillReminder): Result<Unit> = 
        safeExecute("Repo", "Update Reminder") { billReminderDao.updateBillReminder(billReminder) }
        
    override suspend fun deleteBillReminder(billReminder: BillReminder): Result<Unit> = 
        safeExecute("Repo", "Delete Reminder") { billReminderDao.deleteBillReminder(billReminder) }

    override suspend fun getBackupData(): BackupData {
        return BackupData(
            transactions = transactionDao.getAllTransactionsOnce(),
            accounts = accountDao.getAllAccountsOnce(),
            budgets = budgetDao.getAllBudgetsOnce(),
            savingGoals = savingGoalDao.getAllSavingGoalsOnce(),
            categories = categoryDao.getAllCategoriesOnce(),
            recurringTransactions = recurringTransactionDao.getAllRecurringTransactionsOnce(),
            billReminders = billReminderDao.getAllBillRemindersOnce()
        )
    }

    override suspend fun restoreData(data: BackupData): Result<Unit> = safeExecute("Repo", "Restore Data") {
        database.withTransaction {
            // Clear all existing data
            transactionDao.deleteAllTransactions()
            accountDao.deleteAllAccounts()
            budgetDao.deleteAllBudgets()
            savingGoalDao.deleteAllSavingGoals()
            categoryDao.deleteAllCategories()
            recurringTransactionDao.deleteAllRecurringTransactions()
            billReminderDao.deleteAllBillReminders()

            // Restore in order to maintain potential foreign key relationships
            data.categories.forEach { categoryDao.insert(it) }
            data.accounts.forEach { accountDao.insertAccount(it) }
            data.budgets.forEach { budgetDao.insertBudget(it) }
            data.savingGoals.forEach { savingGoalDao.insertSavingGoal(it) }
            data.transactions.forEach { transactionDao.insertTransaction(it) }
            data.recurringTransactions.forEach { recurringTransactionDao.insertRecurringTransaction(it) }
            data.billReminders.forEach { billReminderDao.insertBillReminder(it) }
        }
    }

    override suspend fun getTransactionByHash(hash: String): Transaction? = transactionDao.getTransactionByHash(hash)
    
    override suspend fun getAccountBySuffix(suffix: String): Account? {
        return accountDao.getAllAccountsOnce().find { it.accountNumberSuffix == suffix }
    }

    override fun getAllPendingTransactionsStream(): Flow<List<PendingTransaction>> = pendingTransactionDao.getAllPendingTransactions()
    
    override suspend fun insertPendingTransaction(pending: PendingTransaction): Result<Unit> = 
        safeExecute("Repo", "Insert Pending") { pendingTransactionDao.insertPending(pending) }
        
    override suspend fun deletePendingTransaction(pending: PendingTransaction): Result<Unit> = 
        safeExecute("Repo", "Delete Pending") { pendingTransactionDao.deletePending(pending) }
        
    override suspend fun getPendingTransactionByHash(hash: String): PendingTransaction? = pendingTransactionDao.getPendingByHash(hash)
}
