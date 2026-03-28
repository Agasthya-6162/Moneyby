package com.example.moneyby.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyby.data.Account
import com.example.moneyby.data.Transaction
import com.example.moneyby.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class TransactionEntryViewModel(private val transactionRepository: TransactionRepository) : ViewModel() {

    // ── UI state ─────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    // Keep for backward-compat
    val transactionUiState: TransactionUiState get() = _uiState.value

    // ── One-shot events ───────────────────────────────────────────────────────
    private val _event = MutableStateFlow<TransactionEvent?>(null)
    val event: StateFlow<TransactionEvent?> = _event.asStateFlow()
    fun consumeEvent() { _event.value = null }

    // ── Reference data ────────────────────────────────────────────────────────
    val accountsState: StateFlow<List<Account>> = transactionRepository.getAllAccountsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categoriesState: StateFlow<List<com.example.moneyby.data.Category>> =
        transactionRepository.getAllCategoriesStream()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Form mutations ────────────────────────────────────────────────────────
    fun updateUiState(details: TransactionDetails) {
        _uiState.update {
            TransactionUiState(transactionDetails = details, isEntryValid = validateInput(details))
        }
    }

    private fun resetForm() {
        _uiState.value = TransactionUiState()
    }

    // ── Persistence ───────────────────────────────────────────────────────────
    suspend fun saveTransaction() {
        if (!validateInput()) return
        _uiState.update { it.copy(isSaving = true) }
        
        val result = transactionRepository.insertTransaction(
            _uiState.value.transactionDetails.toTransaction()
        )
        
        if (result is com.example.moneyby.util.Result.Success) {
            resetForm()
            _event.value = TransactionEvent.SaveSuccess
        } else if (result is com.example.moneyby.util.Result.Error) {
            _uiState.update { it.copy(isSaving = false) }
            _event.value = TransactionEvent.Error(result.message)
        }
    }

    suspend fun loadTransaction(id: Int) {
        val transaction = transactionRepository.getTransactionStream(id) ?: return
        _uiState.value = TransactionUiState(
            transactionDetails = transaction.toTransactionDetails(),
            isEntryValid = true
        )
    }

    suspend fun updateTransaction() {
        if (!validateInput()) return
        _uiState.update { it.copy(isSaving = true) }
        
        val result = transactionRepository.updateTransaction(
            _uiState.value.transactionDetails.toTransaction()
        )
        
        if (result is com.example.moneyby.util.Result.Success) {
            _event.value = TransactionEvent.SaveSuccess
        } else if (result is com.example.moneyby.util.Result.Error) {
            _uiState.update { it.copy(isSaving = false) }
            _event.value = TransactionEvent.Error(result.message)
        }
    }

    private fun validateInput(
        details: TransactionDetails = _uiState.value.transactionDetails
    ): Boolean = with(details) {
        val baseValid = amount.isNotBlank()
            && (amount.toDoubleOrNull() ?: 0.0) > 0.0
            && accountId != 0
        if (!baseValid) return false
        if (type == "Transfer") toAccountId != 0 && toAccountId != accountId
        else category.isNotBlank()
    }
}

sealed class TransactionEvent {
    object SaveSuccess : TransactionEvent()
    data class Error(val message: String) : TransactionEvent()
}

data class TransactionUiState(
    val transactionDetails: TransactionDetails = TransactionDetails(),
    val isEntryValid: Boolean = false,
    val isSaving: Boolean = false
)

data class TransactionDetails(
    val id: Int = 0,
    val amount: String = "",
    val category: String = "",
    val date: Long = System.currentTimeMillis(),
    val type: String = "Expense",
    val accountId: Int = 0,
    val toAccountId: Int = 0,
    val goalId: Int? = null,
    val notes: String = ""
)

fun TransactionDetails.toTransaction(): Transaction = Transaction(
    id = id,
    amount = amount.toDoubleOrNull() ?: 0.0,
    category = if (type == "Transfer") "Transfer" else category,
    date = date,
    type = type,
    accountId = accountId,
    toAccountId = if (type == "Transfer") toAccountId else null,
    goalId = goalId,
    notes = notes
)

fun Transaction.toTransactionDetails(): TransactionDetails = TransactionDetails(
    id = id,
    amount = amount.toString(),
    category = category,
    date = date,
    type = type,
    accountId = accountId,
    toAccountId = toAccountId ?: 0,
    goalId = goalId,
    notes = notes
)
