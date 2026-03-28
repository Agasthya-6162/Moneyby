package com.example.moneyby.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyby.data.RecurringTransaction
import com.example.moneyby.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecurringTransactionViewModel(private val transactionRepository: TransactionRepository) : ViewModel() {

    val uiState: StateFlow<RecurringTransactionUiState> = transactionRepository.getAllRecurringTransactionsStream()
        .map { RecurringTransactionUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecurringTransactionUiState()
        )

    val accounts = transactionRepository.getAllAccountsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── One-shot events ───────────────────────────────────────────────────────
    private val _event = MutableStateFlow<RecurringEvent?>(null)
    val event: StateFlow<RecurringEvent?> = _event.asStateFlow()
    fun consumeEvent() { _event.value = null }

    fun saveRecurringTransaction(
        name: String,
        amount: Double,
        category: String,
        type: String,
        accountId: Int,
        frequency: String,
        nextRunDate: Long
    ) {
        viewModelScope.launch {
            val recurring = RecurringTransaction(
                name = name,
                amount = amount,
                category = category,
                type = type,
                accountId = accountId,
                frequency = frequency,
                nextRunDate = nextRunDate,
                isActive = true
            )
            val result = transactionRepository.insertRecurringTransaction(recurring)
            if (result is com.example.moneyby.util.Result.Error) {
                _event.value = RecurringEvent.Error(result.message)
            }
        }
    }

    fun deleteRecurringTransaction(transaction: RecurringTransaction) {
        viewModelScope.launch {
            val result = transactionRepository.deleteRecurringTransaction(transaction)
            if (result is com.example.moneyby.util.Result.Error) {
                _event.value = RecurringEvent.Error(result.message)
            }
        }
    }
}

sealed class RecurringEvent {
    data class Error(val message: String) : RecurringEvent()
}

data class RecurringTransactionUiState(
    val transactions: List<RecurringTransaction> = emptyList()
)
