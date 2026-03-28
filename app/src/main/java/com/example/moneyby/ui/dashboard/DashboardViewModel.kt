package com.example.moneyby.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyby.data.*
import com.example.moneyby.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val securityManager: SecurityManager
) : ViewModel() {

    val isBiometricEnabled: StateFlow<Boolean> = securityManager.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBiometricAvailable: Boolean = securityManager.isBiometricAvailable()

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            securityManager.setBiometricEnabled(enabled)
        }
    }

    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.getAllTransactionsStream(),
        transactionRepository.getAllAccountsStream(),
        securityManager.resetDay,
        transactionRepository.getAllSavingGoalsStream(),
        transactionRepository.getAllPendingTransactionsStream()
    ) { transactions, accounts, resetDay, goals, pending ->
        val accountBalances = accounts.map { account ->
            val fromTransactions = transactions.filter { it.accountId == account.id }
            val toTransactions = transactions.filter { it.toAccountId == account.id }
            
            val income = fromTransactions.filter { it.type == "Income" }.sumOf { it.amount }
            val expense = fromTransactions.filter { it.type == "Expense" }.sumOf { it.amount }
            val transferOut = fromTransactions.filter { it.type == "Transfer" }.sumOf { it.amount }
            val transferIn = toTransactions.filter { it.type == "Transfer" }.sumOf { it.amount }
            
            AccountBalance(account, account.initialBalance + income + transferIn - expense - transferOut)
        }

        val netWorth = accountBalances.sumOf { it.balance }
        
        val dateRange = getCurrentMonthRange(resetDay)
        val startTime = dateRange.first
        val endTime = dateRange.second
        
        val monthlyTransactions = transactions.filter {
            it.date in startTime..endTime
        }
        
        val monthlyExpense = monthlyTransactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val monthlyIncome = monthlyTransactions.filter { it.type == "Income" }.sumOf { it.amount }
        
        val currentMonthYear = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date(startTime))
        
        // Handle budgets safely without blocking - assume empty if unavailable
        val budgetProgress = emptyList<BudgetProgress>()

        val categoryWiseSpending = monthlyTransactions
            .filter { it.type == "Expense" }
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }

        val topCategory = categoryWiseSpending.maxByOrNull { it.value }?.key
        val budgetsAtRisk = 0 // Will update via separate flow if needed

        val updatedGoals = goals.map { goal ->
            val goalTransactions = transactions.filter { it.goalId == goal.id }
            val deposits = goalTransactions.filter { it.type == "Income" }.sumOf { it.amount }
            val withdrawals = goalTransactions.filter { it.type == "Expense" }.sumOf { it.amount }
            val calculatedAmount = deposits - withdrawals
            goal.copy(
                currentAmount = calculatedAmount,
                isCompleted = calculatedAmount >= goal.targetAmount
            )
        }

        DashboardUiState(
            netWorth = netWorth,
            monthlyExpense = monthlyExpense,
            monthlyIncome = monthlyIncome,
            accountBalances = accountBalances,
            budgetProgress = budgetProgress,
            categoryWiseSpending = categoryWiseSpending,
            recentTransactions = transactions.sortedByDescending { it.date }.take(10),
            topSpendingCategory = topCategory,
            budgetsAtRiskCount = budgetsAtRisk,
            savingGoals = updatedGoals.filter { !it.isCompleted }.take(3),
            pendingTransactions = pending,
            isLoading = false
        )

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(isLoading = true)
    )

    // ── One-shot events ───────────────────────────────────────────────────────
    private val _event = MutableStateFlow<DashboardEvent?>(null)
    val event: StateFlow<DashboardEvent?> = _event.asStateFlow()
    fun consumeEvent() { _event.value = null }

    fun confirmTransaction(pending: PendingTransaction) {
        viewModelScope.launch {
            val accounts = transactionRepository.getAllAccountsStream().first()
            val account = if (pending.accountSuffix != null) {
                accounts.find { it.accountNumberSuffix == pending.accountSuffix }
            } else {
                accounts.firstOrNull()
            } ?: accounts.firstOrNull()

            if (account != null) {
                val transaction = Transaction(
                    amount = pending.amount,
                    category = pending.category,
                    date = pending.date,
                    type = pending.type,
                    accountId = account.id,
                    notes = pending.merchant ?: "Auto-detected",
                    transactionHash = pending.transactionHash,
                    isAutoDetected = true
                )
                val result = transactionRepository.insertTransaction(transaction)
                if (result is com.example.moneyby.util.Result.Error) {
                    _event.value = DashboardEvent.Error(result.message)
                    return@launch
                }
            }
            
            val deleteResult = transactionRepository.deletePendingTransaction(pending)
            if (deleteResult is com.example.moneyby.util.Result.Error) {
                _event.value = DashboardEvent.Error(deleteResult.message)
            }
        }
    }

    fun discardTransaction(pending: PendingTransaction) {
        viewModelScope.launch {
            val result = transactionRepository.deletePendingTransaction(pending)
            if (result is com.example.moneyby.util.Result.Error) {
                _event.value = DashboardEvent.Error(result.message)
            }
        }
    }



    private fun getCurrentMonthRange(resetDay: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        if (currentDay < resetDay) {
            startCalendar.add(Calendar.MONTH, -1)
        }
        startCalendar.set(Calendar.DAY_OF_MONTH, resetDay)
        
        val endCalendar = (startCalendar.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }
        
        return Pair(startCalendar.timeInMillis, endCalendar.timeInMillis)
    }
}

sealed class DashboardEvent {
    data class Error(val message: String) : DashboardEvent()
}

data class DashboardUiState(
    val netWorth: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val accountBalances: List<AccountBalance> = emptyList(),
    val budgetProgress: List<BudgetProgress> = emptyList(),
    val categoryWiseSpending: Map<String, Double> = emptyMap(),
    val recentTransactions: List<Transaction> = emptyList(),
    val topSpendingCategory: String? = null,
    val budgetsAtRiskCount: Int = 0,
    val savingGoals: List<SavingGoal> = emptyList(),
    val pendingTransactions: List<PendingTransaction> = emptyList(),
    val isLoading: Boolean = false
)



data class AccountBalance(
    val account: Account,
    val balance: Double
)

data class BudgetProgress(
    val budget: Budget,
    val spent: Double
)
