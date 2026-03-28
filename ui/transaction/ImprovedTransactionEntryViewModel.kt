package com.example.moneyby.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyby.data.*
import com.example.moneyby.domain.repository.TransactionRepository
import com.example.moneyby.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ImprovedTransactionEntryViewModel(
    private val transactionRepository: TransactionRepository,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransactionEntryUiState>(TransactionEntryUiState.Initial)
    val uiState: StateFlow<TransactionEntryUiState> = _uiState.asStateFlow()

    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    private val _saveResult = MutableSharedFlow<Result<Unit>>()
    val saveResult: SharedFlow<Result<Unit>> = _saveResult.asSharedFlow()

    val accounts: StateFlow<List<Account>> = transactionRepository.getAllAccountsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = transactionRepository.getAllCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initializeForEdit(transactionId: Int) {
        viewModelScope.launch {
            try {
                val transaction = transactionRepository.getTransactionStream(transactionId)
                if (transaction != null) {
                    _uiState.value = TransactionEntryUiState.Editing(
                        id = transaction.id,
                        amount = transaction.amount.toString(),
                        category = transaction.category,
                        date = transaction.date,
                        type = transaction.type,
                        accountId = transaction.accountId,
                        notes = transaction.notes
                    )
                } else {
                    _uiState.value = TransactionEntryUiState.Error("Transaction not found")
                }
            } catch (e: Exception) {
                Logger.logException("TransactionVM", "Failed to load transaction", e, "edit")
                _uiState.value = TransactionEntryUiState.Error("Failed to load transaction: ${e.message}")
            }
        }
    }

    fun updateAmount(amount: String) {
        updateState { current ->
            when (current) {
                is TransactionEntryUiState.Creating -> current.copy(amount = amount)
                is TransactionEntryUiState.Editing -> current.copy(amount = amount)
                else -> current
            }
        }
        clearError("amount")
    }

    fun updateCategory(category: String) {
        updateState { current ->
            when (current) {
                is TransactionEntryUiState.Creating -> current.copy(category = category)
                is TransactionEntryUiState.Editing -> current.copy(category = category)
                else -> current
            }
        }
        clearError("category")
    }

    fun updateDate(date: Long) {
        updateState { current ->
            when (current) {
                is TransactionEntryUiState.Creating -> current.copy(date = date)
                is TransactionEntryUiState.Editing -> current.copy(date = date)
                else -> current
            }
        }
    }

    fun updateType(type: String) {
        updateState { current ->
            when (current) {
                is TransactionEntryUiState.Creating -> current.copy(type = type)
                is TransactionEntryUiState.Editing -> current.copy(type = type)
                else -> current
            }
        }
    }

    fun updateAccount(accountId: Int) {
        updateState { current ->
            when (current) {
                is TransactionEntryUiState.Creating -> current.copy(accountId = accountId)
                is TransactionEntryUiState.Editing -> current.copy(accountId = accountId)
                else -> current
            }
        }
    }

    fun updateNotes(notes: String) {
        updateState { current ->
            when (current) {
                is TransactionEntryUiState.Creating -> current.copy(notes = notes)
                is TransactionEntryUiState.Editing -> current.copy(notes = notes)
                else -> current
            }
        }
        clearError("notes")
    }

    fun saveTransaction() {
        val currentState = _uiState.value
        if (currentState !is TransactionEntryUiState.Creating && currentState !is TransactionEntryUiState.Editing) {
            return
        }

        // Validate
        if (!validateForm(currentState)) {
            return
        }

        _uiState.value = (currentState as? TransactionEntryUiState.Creating)?.copy(isLoading = true)
            ?: (currentState as? TransactionEntryUiState.Editing)?.copy(isLoading = true)
            ?: currentState

        viewModelScope.launch {
            try {
                val transaction = when (currentState) {
                    is TransactionEntryUiState.Creating -> {
                        Transaction(
                            amount = currentState.amount.toDouble(),
                            category = currentState.category,
                            date = currentState.date,
                            type = currentState.type,
                            accountId = currentState.accountId,
                            notes = currentState.notes
                        )
                    }
                    is TransactionEntryUiState.Editing -> {
                        Transaction(
                            id = currentState.id,
                            amount = currentState.amount.toDouble(),
                            category = currentState.category,
                            date = currentState.date,
                            type = currentState.type,
                            accountId = currentState.accountId,
                            notes = currentState.notes
                        )
                    }
                    else -> return@launch
                }

                when (currentState) {
                    is TransactionEntryUiState.Creating -> {
                        transactionRepository.insertTransaction(transaction)
                        Logger.logTransaction("TransactionVM", "create", currentState.category, currentState.type)
                    }
                    is TransactionEntryUiState.Editing -> {
                        transactionRepository.updateTransaction(transaction)
                        Logger.logTransaction("TransactionVM", "update", currentState.category, currentState.type)
                    }
                    else -> {}
                }

                _saveResult.emit(Result.Success(Unit))
            } catch (e: Exception) {
                Logger.logException("TransactionVM", "Failed to save transaction", e)
                _saveResult.emit(Result.Error(e, e.message ?: "Failed to save transaction"))
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(transaction)
                Logger.logTransaction("TransactionVM", "delete", transaction.category, transaction.type)
                _saveResult.emit(Result.Success(Unit))
            } catch (e: Exception) {
                Logger.logException("TransactionVM", "Failed to delete transaction", e)
                _saveResult.emit(Result.Error(e))
            }
        }
    }

    private fun validateForm(state: TransactionEntryUiState): Boolean {
        val errors = mutableMapOf<String, String>()
        
        when (state) {
            is TransactionEntryUiState.Creating, is TransactionEntryUiState.Editing -> {
                val amount = if (state is TransactionEntryUiState.Creating) state.amount else (state as TransactionEntryUiState.Editing).amount
                val category = if (state is TransactionEntryUiState.Creating) state.category else (state as TransactionEntryUiState.Editing).category
                val notes = if (state is TransactionEntryUiState.Creating) state.notes else (state as TransactionEntryUiState.Editing).notes

                // Validate amount
                val amountResult = Validator.validateAmount(amount)
                if (amountResult.isError) {
                    errors["amount"] = amountResult.errorMessage ?: "Invalid amount"
                }

                // Validate category
                if (category.isBlank()) {
                    errors["category"] = "Category is required"
                }

                // Validate notes
                val notesResult = Validator.validateNotes(notes)
                if (notesResult.isError) {
                    errors["notes"] = notesResult.errorMessage ?: "Invalid notes"
                }
            }
            else -> {}
        }

        _validationErrors.value = errors
        return errors.isEmpty()
    }

    private fun clearError(field: String) {
        _validationErrors.value = _validationErrors.value.toMutableMap().apply {
            remove(field)
        }
    }

    private fun updateState(transform: (TransactionEntryUiState) -> TransactionEntryUiState) {
        _uiState.value = transform(_uiState.value)
    }
}

sealed class TransactionEntryUiState {
    object Initial : TransactionEntryUiState()

    data class Creating(
        val amount: String = "",
        val category: String = "",
        val date: Long = System.currentTimeMillis(),
        val type: String = "Expense",
        val accountId: Int = 0,
        val notes: String = "",
        val isLoading: Boolean = false
    ) : TransactionEntryUiState()

    data class Editing(
        val id: Int,
        val amount: String = "",
        val category: String = "",
        val date: Long = System.currentTimeMillis(),
        val type: String = "Expense",
        val accountId: Int = 0,
        val notes: String = "",
        val isLoading: Boolean = false
    ) : TransactionEntryUiState()

    data class Error(val message: String) : TransactionEntryUiState()
}
