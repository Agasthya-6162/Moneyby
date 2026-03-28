package com.example.moneyby.data.repository

import com.example.moneyby.data.*
import com.example.moneyby.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import io.mockk.mockkStatic

class OfflineTransactionRepositoryTest {

    private lateinit var repository: OfflineTransactionRepository
    private val database: AppDatabase = mockk()
    private val transactionDao: TransactionDao = mockk()
    private val pendingTransactionDao: PendingTransactionDao = mockk()
    private val accountDao: AccountDao = mockk()
    private val budgetDao: BudgetDao = mockk()
    private val savingGoalDao: SavingGoalDao = mockk()
    private val categoryDao: CategoryDao = mockk()
    private val recurringTransactionDao: RecurringTransactionDao = mockk()
    private val billReminderDao: BillReminderDao = mockk()

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        repository = OfflineTransactionRepository(
            database, transactionDao, pendingTransactionDao, accountDao,
            budgetDao, savingGoalDao, categoryDao, recurringTransactionDao, billReminderDao
        )
    }

    @Test
    fun `getAllTransactionsStream returns data from DAO`() = runTest {
        // Arrange
        val transactionList = listOf(
            Transaction(id = 1, amount = 100.0, category = "Food", date = 123456L, type = "Expense", accountId = 1)
        )
        every { transactionDao.getAllTransactions() } returns flowOf(transactionList)

        // Act
        val result = repository.getAllTransactionsStream().first()

        // Assert
        assertEquals(transactionList, result)
        assertEquals(1, result.size)
    }

    @Test
    fun `insertTransaction success returns Result Success`() = runTest {
        // Arrange
        val transaction = Transaction(id = 1, amount = 100.0, category = "Food", date = 123456L, type = "Expense", accountId = 1)
        coEvery { transactionDao.insertTransaction(transaction) } returns Unit

        // Act
        val result = repository.insertTransaction(transaction)

        // Assert
        assertTrue(result is Result.Success<*>)
        coVerify(exactly = 1) { transactionDao.insertTransaction(transaction) }
    }

    @Test
    fun `insertTransaction exception simulates network or DB error returning Result Error`() = runTest {
        // Arrange
        val transaction = Transaction(id = 1, amount = 100.0, category = "Food", date = 123456L, type = "Expense", accountId = 1)
        // Simulate an IOException matching a failing local database or offline scenario
        coEvery { transactionDao.insertTransaction(transaction) } throws IOException("Disk is full or Unavailable")

        // Act
        val result = repository.insertTransaction(transaction)

        // Assert
        assertTrue(result is Result.Error)
        val errorResult = result as Result.Error
        assertTrue(errorResult.message.contains("Storage me file save nahi ho paayi"))
    }
}
