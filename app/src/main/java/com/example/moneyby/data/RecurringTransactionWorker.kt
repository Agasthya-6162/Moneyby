package com.example.moneyby.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.moneyby.MoneybyApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*

class RecurringTransactionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? MoneybyApplication
                ?: return Result.retry()
            
            (app.container as? com.example.moneyby.AppDataContainer)?.ensureInitialized()
            val repository = app.container.transactionRepository
            
            // Use timeout to prevent hanging
            val recurringTransactions = withTimeoutOrNull(5000) {
                repository.getAllRecurringTransactionsStream().first()
            } ?: return Result.retry()
            
            val currentTime = System.currentTimeMillis()

            recurringTransactions.forEach { recurring ->
                try {
                    if (shouldProcess(recurring, currentTime)) {
                        val transaction = Transaction(
                            amount = recurring.amount,
                            category = recurring.category,
                            date = currentTime,
                            type = recurring.type,
                            accountId = recurring.accountId,
                            notes = "Recurring: ${recurring.notes}"
                        )
                        repository.insertTransaction(transaction)
                        repository.updateRecurringTransaction(
                            recurring.copy(lastProcessedDate = currentTime)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Continue processing other transactions
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun shouldProcess(recurring: RecurringTransaction, currentTime: Long): Boolean {
        if (recurring.lastProcessedDate == 0L) return true

        val lastCalendar = Calendar.getInstance().apply { timeInMillis = recurring.lastProcessedDate }
        val currentCalendar = Calendar.getInstance().apply { timeInMillis = currentTime }

        return when (recurring.frequency) {
            "Daily" -> {
                currentTime - recurring.lastProcessedDate >= 24 * 60 * 60 * 1000L
            }
            "Weekly" -> {
                currentTime - recurring.lastProcessedDate >= 7 * 24 * 60 * 60 * 1000L
            }
            "Monthly" -> {
                currentCalendar.get(Calendar.MONTH) != lastCalendar.get(Calendar.MONTH) ||
                        currentCalendar.get(Calendar.YEAR) != lastCalendar.get(Calendar.YEAR)
            }
            else -> false
        }
    }
}
