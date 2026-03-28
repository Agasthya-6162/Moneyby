package com.example.moneyby.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moneyby.MoneybyApplication

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            com.example.moneyby.ui.transaction.TransactionEntryViewModel(
                moneybyApplication().container.transactionRepository
            )
        }
        initializer {
            com.example.moneyby.ui.dashboard.DashboardViewModel(
                moneybyApplication().container.transactionRepository,
                moneybyApplication().securityManager
            )
        }
        initializer {
            com.example.moneyby.ui.history.TransactionHistoryViewModel(
                moneybyApplication().container.transactionRepository
            )
        }
        initializer {
            com.example.moneyby.ui.budget.BudgetViewModel(
                moneybyApplication().container.transactionRepository,
                moneybyApplication().securityManager
            )
        }
        initializer {
            com.example.moneyby.ui.goal.SavingGoalViewModel(
                moneybyApplication().container.transactionRepository
            )
        }
        initializer {
            com.example.moneyby.ui.settings.SettingsViewModel(
                moneybyApplication().container.backupManager,
                moneybyApplication().securityManager
            )
        }
        initializer {
            com.example.moneyby.ui.settings.CategoryViewModel(
                moneybyApplication().container.transactionRepository
            )
        }
        initializer {
            com.example.moneyby.ui.stats.StatisticsViewModel(
                moneybyApplication().container.transactionRepository
            )
        }
        initializer {
            com.example.moneyby.ui.reminder.BillReminderViewModel(
                moneybyApplication().container.transactionRepository
            )
        }
        initializer {
            com.example.moneyby.ui.recurring.RecurringTransactionViewModel(
                moneybyApplication().container.transactionRepository
            )
        }
        initializer {
            com.example.moneyby.ui.settings.AccountViewModel(
                moneybyApplication().container.transactionRepository
            )
        }
    }
}

fun CreationExtras.moneybyApplication(): MoneybyApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MoneybyApplication)
