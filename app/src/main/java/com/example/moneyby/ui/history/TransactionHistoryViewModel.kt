package com.example.moneyby.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyby.data.Account
import com.example.moneyby.data.Transaction
import com.example.moneyby.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class TransactionHistoryViewModel(private val transactionRepository: TransactionRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<Int?>(null)
    val selectedAccountId: StateFlow<Int?> = _selectedAccountId.asStateFlow()

    private val _dateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
    val dateRange: StateFlow<Pair<Long?, Long?>> = _dateRange.asStateFlow()

    val accounts: StateFlow<List<Account>> = transactionRepository.getAllAccountsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyUiState: StateFlow<HistoryUiState> = combine(
        transactionRepository.getAllTransactionsStream(),
        _searchQuery,
        _selectedAccountId,
        _dateRange
    ) { transactions, query, accountId, range ->
        var filteredTransactions = transactions

        if (query.isNotBlank()) {
            filteredTransactions = filteredTransactions.filter {
                it.category.contains(query, ignoreCase = true) ||
                        it.amount.toString().contains(query) ||
                        it.notes.contains(query, ignoreCase = true)
            }
        }

        if (accountId != null) {
            filteredTransactions = filteredTransactions.filter { it.accountId == accountId }
        }

        val (startDate, endDate) = range
        if (startDate != null) {
            filteredTransactions = filteredTransactions.filter { it.date >= startDate }
        }
        if (endDate != null) {
            filteredTransactions = filteredTransactions.filter { it.date <= endDate }
        }

        val groupedTransactions = filteredTransactions.groupBy {
            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it.date))
        }

        HistoryUiState(groupedTransactions = groupedTransactions)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectAccount(accountId: Int?) {
        _selectedAccountId.value = accountId
    }

    fun updateDateRange(start: Long?, end: Long?) {
        _dateRange.value = start to end
    }
}

data class HistoryUiState(
    val groupedTransactions: Map<String, List<Transaction>> = emptyMap()
)
