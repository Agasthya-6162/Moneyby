package com.example.moneyby.ui.transaction

import com.example.moneyby.domain.repository.TransactionRepository
import com.example.moneyby.util.MainDispatcherRule
import com.example.moneyby.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: TransactionRepository = mockk()
    private lateinit var viewModel: TransactionEntryViewModel

    @Before
    fun setup() {
        every { repository.getAllAccountsStream() } returns flowOf(emptyList())
        every { repository.getAllCategoriesStream() } returns flowOf(emptyList())
        viewModel = TransactionEntryViewModel(repository)
    }

    @Test
    fun `updateUiState sets isEntryValid to true for valid input`() {
        val details = TransactionDetails(
            amount = "100.0",
            category = "Food",
            accountId = 1,
            type = "Expense"
        )

        viewModel.updateUiState(details)

        assertTrue(viewModel.uiState.value.isEntryValid)
        assertEquals(details, viewModel.uiState.value.transactionDetails)
    }

    @Test
    fun `updateUiState sets isEntryValid to false for invalid input`() {
        val details = TransactionDetails(
            amount = "", // Invalid amount
            category = "Food",
            accountId = 1,
            type = "Expense"
        )

        viewModel.updateUiState(details)

        assertFalse(viewModel.uiState.value.isEntryValid)
    }

    @Test
    fun `saveTransaction emits SaveSuccess on Result Success`() = runTest {
        val details = TransactionDetails(
            amount = "150.0",
            category = "Salary",
            accountId = 1,
            type = "Income"
        )
        viewModel.updateUiState(details)

        // Mock repository returning Success
        coEvery { repository.insertTransaction(any()) } returns Result.Success(Unit)

        viewModel.saveTransaction()
        advanceUntilIdle()

        // After success, form resets
        assertEquals("", viewModel.uiState.value.transactionDetails.amount)
        assertEquals(TransactionEvent.SaveSuccess, viewModel.event.value)
    }

    @Test
    fun `saveTransaction emits Error on network or db failure`() = runTest {
        val details = TransactionDetails(
            amount = "150.0",
            category = "Salary",
            accountId = 1,
            type = "Income"
        )
        viewModel.updateUiState(details)

        // Mock repository returning Error simulating DB exception
        coEvery { repository.insertTransaction(any()) } returns Result.Error(Exception("Database insertion failed"), "Database insertion failed")

        viewModel.saveTransaction()
        advanceUntilIdle()

        // Should contain Error Event
        val event = viewModel.event.value
        assertTrue(event is TransactionEvent.Error)
        assertEquals("Database insertion failed", (event as TransactionEvent.Error).message)
    }
}
