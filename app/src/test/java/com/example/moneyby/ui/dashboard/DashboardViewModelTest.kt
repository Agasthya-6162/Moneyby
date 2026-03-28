package com.example.moneyby.ui.dashboard

import com.example.moneyby.data.Account
import com.example.moneyby.data.SecurityManager
import com.example.moneyby.data.Transaction
import com.example.moneyby.domain.repository.TransactionRepository
import com.example.moneyby.util.MainDispatcherRule
import com.example.moneyby.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: TransactionRepository = mockk()
    private val securityManager: SecurityManager = mockk()

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        // Setup default mocks for flows so that ViewModel initially starts cleanly
        every { securityManager.isBiometricEnabled } returns flowOf(false)
        every { securityManager.isBiometricAvailable() } returns true
        every { securityManager.resetDay } returns flowOf(1)
        
        every { repository.getAllTransactionsStream() } returns flowOf(emptyList())
        every { repository.getAllAccountsStream() } returns flowOf(
            listOf(Account(id = 1, name = "Cash", initialBalance = 1000.0, type = "Wallet"))
        )
        every { repository.getAllSavingGoalsStream() } returns flowOf(emptyList())
        every { repository.getAllPendingTransactionsStream() } returns flowOf(emptyList())
        every { repository.getBudgetsForMonthStream(any()) } returns flowOf(emptyList())

        viewModel = DashboardViewModel(repository, securityManager)
    }

    @Test
    fun `initial toggleBiometric sets value in SecurityManager`() = runTest {
        coEvery { securityManager.setBiometricEnabled(true) } returns Unit

        viewModel.toggleBiometric(true)
        advanceUntilIdle() // let coroutine finish

        coVerify(exactly = 1) { securityManager.setBiometricEnabled(true) }
    }

    @Test
    fun `dashboardUiState collects and calculates netWorth correctly`() = runTest {
        // Arrange
        val account = Account(id = 1, name = "Test Account", type = "Bank", initialBalance = 1000.0)
        val transaction = Transaction(id = 1, amount = 200.0, category = "Food", type = "Expense", date = System.currentTimeMillis(), accountId = account.id)

        every { repository.getAllAccountsStream() } returns flowOf(listOf(account))
        every { repository.getAllTransactionsStream() } returns flowOf(listOf(transaction))

        // Create new ViewModel to catch the mocked emissions directly
        val newViewModel = DashboardViewModel(repository, securityManager)
        
        val job = launch(UnconfinedTestDispatcher()) { newViewModel.dashboardUiState.collect() }

        advanceUntilIdle()

        // Act
        val uiState = newViewModel.dashboardUiState.value

        // Assert
        assertFalse(uiState.isLoading) // Initial load should finish
        assertEquals(800.0, uiState.netWorth, 0.0) // 1000 initial - 200 expense
        
        job.cancel()
    }
}
