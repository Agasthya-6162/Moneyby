package com.example.moneyby.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.moneyby.MoneybyApplication
import com.example.moneyby.ui.util.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.text.NumberFormat
import java.util.*
import com.example.moneyby.util.formatCurrency
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val application = applicationContext as? MoneybyApplication
                ?: return Result.retry()
            
            (application.container as? com.example.moneyby.AppDataContainer)?.ensureInitialized()
            val repository = application.container.transactionRepository
            val notificationHelper = NotificationHelper(applicationContext)

            val currentTime = System.currentTimeMillis()
            
            // Use timeout to prevent hanging
            val bills = withTimeoutOrNull(5000) {
                repository.getAllBillRemindersStream().first()
            } ?: return Result.retry()

            bills.filter { !it.isPaid }.forEach { bill ->
                try {
                    val daysRemaining = TimeUnit.MILLISECONDS.toDays(bill.dueDate - currentTime)
                    
                    if (daysRemaining <= bill.reminderDaysBefore && daysRemaining >= 0) {
                        val amountStr = formatCurrency(bill.amount)
                        val message = if (daysRemaining == 0L) {
                            "Your bill '${bill.name}' for $amountStr is due today!"
                        } else {
                            "Reminder: '${bill.name}' for $amountStr is due in $daysRemaining days."
                        }
                        
                        notificationHelper.showNotification(
                            title = "Bill Reminder",
                            message = message,
                            notificationId = bill.id
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Continue processing other bills
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
