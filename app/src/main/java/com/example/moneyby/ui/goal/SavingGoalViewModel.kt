package com.example.moneyby.ui.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyby.data.SavingGoal
import com.example.moneyby.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SavingGoalViewModel(private val transactionRepository: TransactionRepository) : ViewModel() {

    val savingGoalsUiState: StateFlow<SavingGoalsUiState> = combine(
        transactionRepository.getAllSavingGoalsStream(),
        transactionRepository.getAllTransactionsStream()
    ) { goals, transactions ->
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
        SavingGoalsUiState(goals = updatedGoals)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavingGoalsUiState()
    )

    val accountsState: StateFlow<List<com.example.moneyby.data.Account>> = transactionRepository.getAllAccountsStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ── One-shot events ───────────────────────────────────────────────────────
    private val _event = MutableStateFlow<SavingGoalEvent?>(null)
    val event: StateFlow<SavingGoalEvent?> = _event.asStateFlow()
    fun consumeEvent() { _event.value = null }

    fun saveGoal(name: String, targetAmount: Double, currentAmount: Double, targetDate: Long, goalId: Int = 0) {
        viewModelScope.launch {
            val goal = SavingGoal(
                id = goalId,
                name = name,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                targetDate = targetDate,
                isCompleted = currentAmount >= targetAmount
            )
            val result = if (goalId == 0) {
                transactionRepository.insertSavingGoal(goal)
            } else {
                transactionRepository.updateSavingGoal(goal)
            }
            if (result is com.example.moneyby.util.Result.Error) {
                _event.value = SavingGoalEvent.Error(result.message)
            }
        }
    }

    fun deleteGoal(goal: SavingGoal) {
        viewModelScope.launch {
            val result = transactionRepository.deleteSavingGoal(goal)
            if (result is com.example.moneyby.util.Result.Error) {
                _event.value = SavingGoalEvent.Error(result.message)
            }
        }
    }

    fun addGoalTransaction(goal: SavingGoal, amount: Double, type: String, accountId: Int) {
        viewModelScope.launch {
            val transaction = com.example.moneyby.data.Transaction(
                amount = amount,
                category = "Saving Goal",
                date = System.currentTimeMillis(),
                type = type,
                accountId = accountId,
                goalId = goal.id,
                notes = "${if (type == "Income") "Deposit to" else "Withdrawal from"} ${goal.name}"
            )
            val result = transactionRepository.insertTransaction(transaction)
            if (result is com.example.moneyby.util.Result.Error) {
                _event.value = SavingGoalEvent.Error(result.message)
            }
        }
    }

    fun getGoalTransactions(goalId: Int): Flow<List<com.example.moneyby.data.Transaction>> =
        transactionRepository.getTransactionsByGoalIdStream(goalId)
}

/**
 * Events for the Saving Goal screen.
 */
sealed class SavingGoalEvent {
    data class Error(val message: String) : SavingGoalEvent()
}

data class SavingGoalsUiState(
    val goals: List<SavingGoal> = emptyList()
)
