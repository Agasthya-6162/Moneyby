package com.example.moneyby.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyby.data.Budget
import com.example.moneyby.domain.repository.TransactionRepository
import com.example.moneyby.data.SecurityManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BudgetViewModel(
    private val transactionRepository: TransactionRepository,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val currentMonthYear = securityManager.resetDay.map { resetDay ->
        getCurrentMonthYear(resetDay)
    }

    val budgetUiState: StateFlow<BudgetUiState> = combine(
        currentMonthYear,
        transactionRepository.getAllTransactionsStream(),
        securityManager.resetDay
    ) { monthYear, transactions, resetDay ->
        // Don't use .first() - assume budgets are empty if unavailable
        val budgets = emptyList<Budget>()
        
        // Calculate spending for each budget category in the current period
        val periodStart = getPeriodStart(resetDay)
        val periodTransactions = transactions.filter { it.date >= periodStart && it.type == "Expense" }
        
        val budgetsWithProgress = budgets.map { budget ->
            val spent = periodTransactions
                .filter { it.category == budget.category }
                .sumOf { it.amount }
            BudgetWithProgress(budget, spent)
        }
        
        BudgetUiState(budgets = budgetsWithProgress)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetUiState()
    )

    // ── One-shot events ───────────────────────────────────────────────────────
    private val _event = MutableStateFlow<BudgetEvent?>(null)
    val event: StateFlow<BudgetEvent?> = _event.asStateFlow()
    fun consumeEvent() { _event.value = null }

    fun saveBudget(category: String, limit: Double) {
        viewModelScope.launch {
            val resetDay = securityManager.resetDay.first()
            val monthYear = getCurrentMonthYear(resetDay)
            val existingBudget = budgetUiState.value.budgets.find { it.budget.category == category }
            
            val result = if (existingBudget != null) {
                transactionRepository.updateBudget(existingBudget.budget.copy(limitAmount = limit))
            } else {
                transactionRepository.insertBudget(
                    Budget(category = category, limitAmount = limit, monthYear = monthYear)
                )
            }
            
            if (result is com.example.moneyby.util.Result.Error) {
                _event.value = BudgetEvent.Error(result.message)
            }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            val result = transactionRepository.deleteBudget(budget)
            if (result is com.example.moneyby.util.Result.Error) {
                _event.value = BudgetEvent.Error(result.message)
            }
        }
    }

    private fun getPeriodStart(resetDay: Int): Long {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        if (currentDay < resetDay) {
            calendar.add(Calendar.MONTH, -1)
        }
        calendar.set(Calendar.DAY_OF_MONTH, resetDay)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getCurrentMonthYear(resetDay: Int): String {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        if (currentDay < resetDay) {
            calendar.add(Calendar.MONTH, -1)
        }
        
        val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        return sdf.format(calendar.time)
    }
}

data class BudgetWithProgress(
    val budget: Budget,
    val spent: Double
)

sealed class BudgetEvent {
    data class Error(val message: String) : BudgetEvent()
}

data class BudgetUiState(
    val budgets: List<BudgetWithProgress> = emptyList()
)
