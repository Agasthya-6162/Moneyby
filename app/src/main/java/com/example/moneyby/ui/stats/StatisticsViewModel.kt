package com.example.moneyby.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyby.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import java.util.*

class StatisticsViewModel(private val transactionRepository: TransactionRepository) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

    val selectedMonth: StateFlow<Int> = _selectedMonth
    val selectedYear: StateFlow<Int> = _selectedYear

    fun goToPreviousMonth() {
        if (_selectedMonth.value == 0) {
            _selectedMonth.value = 11
            _selectedYear.value -= 1
        } else {
            _selectedMonth.value -= 1
        }
    }

    fun goToNextMonth() {
        val now = Calendar.getInstance()
        val isCurrentMonth = _selectedMonth.value == now.get(Calendar.MONTH) &&
                _selectedYear.value == now.get(Calendar.YEAR)
        if (!isCurrentMonth) {
            if (_selectedMonth.value == 11) {
                _selectedMonth.value = 0
                _selectedYear.value += 1
            } else {
                _selectedMonth.value += 1
            }
        }
    }

    private val _event = MutableStateFlow<StatisticsEvent?>(null)
    val event: StateFlow<StatisticsEvent?> = _event

    val statsUiState: StateFlow<StatisticsUiState> = combine(
        transactionRepository.getAllTransactionsStream(),
        _selectedMonth,
        _selectedYear
    ) { transactions, month, year ->
        val filtered = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
        }

        val totalIncome = filtered.filter { it.type == "Income" }.sumOf { it.amount }
        val totalExpense = filtered.filter { it.type == "Expense" }.sumOf { it.amount }

        val categoryBreakdown = filtered
            .filter { it.type == "Expense" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { t -> t.amount } }

        val trendData = (0..5).reversed().map { offset ->
            val targetCal = Calendar.getInstance().apply {
                set(Calendar.MONTH, month)
                set(Calendar.YEAR, year)
                add(Calendar.MONTH, -offset)
            }
            val m = targetCal.get(Calendar.MONTH)
            val y = targetCal.get(Calendar.YEAR)

            val monthTx = transactions.filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == y
            }

            MonthlyTrend(
                monthName = targetCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: "",
                income = monthTx.filter { it.type == "Income" }.sumOf { it.amount },
                expense = monthTx.filter { it.type == "Expense" }.sumOf { it.amount }
            )
        }

        StatisticsUiState(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            categoryBreakdown = categoryBreakdown,
            monthlyTrend = trendData
        )
    }.catch { e ->
        _event.value = StatisticsEvent.ShowError("Data load nahi ho paaya: ${e.message}")
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState()
    )

    fun consumeEvent() { _event.value = null }
}

sealed class StatisticsEvent {
    data class ShowError(val message: String) : StatisticsEvent()
}

data class StatisticsUiState(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val monthlyTrend: List<MonthlyTrend> = emptyList()
)

data class MonthlyTrend(
    val monthName: String,
    val income: Double,
    val expense: Double
)
