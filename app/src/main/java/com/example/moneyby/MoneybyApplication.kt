package com.example.moneyby

import android.app.Application
import com.example.moneyby.data.BackupManager
import com.example.moneyby.data.SecurityManager
import com.example.moneyby.data.repository.OfflineTransactionRepository
import com.example.moneyby.domain.repository.TransactionRepository
import com.example.moneyby.util.AutoDetectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

class MoneybyApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var container: AppContainer
    lateinit var securityManager: SecurityManager

    override fun onCreate() {
        super.onCreate()
        securityManager = SecurityManager(this)
        
        // SQLCipher 4.6.1+ requires explicit native library loading
        System.loadLibrary("sqlcipher")
        
        container = AppDataContainer(this, securityManager)

        applicationScope.launch {
            try {
                securityManager.migrateLegacySecretsIfNeeded()
                // Proper flow collection without deprecated import
                securityManager.autoBackupInterval.collect { interval ->
                    scheduleAutoBackup(interval)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        applicationScope.launch {
            (container as AppDataContainer).ensureInitialized()
        }

        scheduleRecurringTransactions()
        scheduleBillReminders()
    }

    private fun scheduleBillReminders() {
        val workManager = androidx.work.WorkManager.getInstance(this)
        val reminderRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.moneyby.data.ReminderWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "BillReminders",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }

    private fun scheduleRecurringTransactions() {
        val workManager = androidx.work.WorkManager.getInstance(this)
        val recurringRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.moneyby.data.RecurringTransactionWorker>(
            12, java.util.concurrent.TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "RecurringTransactions",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            recurringRequest
        )
    }

    private fun scheduleAutoBackup(intervalMillis: Long) {
        val minInterval = 15 * 60 * 1000L
        val safeInterval = maxOf(intervalMillis, minInterval)
        val workManager = androidx.work.WorkManager.getInstance(this)
        val constraints = androidx.work.Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val autoBackupRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.moneyby.data.AutoBackupWorker>(
            safeInterval, java.util.concurrent.TimeUnit.MILLISECONDS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "AutoBackup",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE, // Update if interval changed
            autoBackupRequest
        )
    }
}


interface AppContainer {
    val transactionRepository: TransactionRepository
    val backupManager: BackupManager
    val autoDetectionManager: AutoDetectionManager
}




class AppDataContainer(
    private val context: android.content.Context,
    private val _securityManager: SecurityManager
) : AppContainer {

    private lateinit var _transactionRepository: TransactionRepository
    private lateinit var _autoDetectionManager: AutoDetectionManager
    private lateinit var _backupManager: BackupManager
    private val initMutex = kotlinx.coroutines.sync.Mutex()
    private var initialized = false

    suspend fun ensureInitialized() {
        if (initialized) return
        initMutex.withLock {
            if (initialized) return@withLock
            try {
                val passphrase = _securityManager.getOrCreatePassphrase().toByteArray()
                val database = com.example.moneyby.data.AppDatabase.getDatabase(context, passphrase)

                _transactionRepository = OfflineTransactionRepository(
                    database,
                    database.transactionDao(),
                    database.pendingTransactionDao(),
                    database.accountDao(),
                    database.budgetDao(),
                    database.savingGoalDao(),
                    database.categoryDao(),
                    database.recurringTransactionDao(),
                    database.billReminderDao()
                )

                _autoDetectionManager = AutoDetectionManager(_transactionRepository, _securityManager)
                _backupManager = BackupManager(context, _transactionRepository)
                initialized = true
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    override val transactionRepository: TransactionRepository
        get() {
            if (!initialized) {
                android.util.Log.e("AppDataContainer", "Accessing repository before initialization. This will fail.")
            }
            require(initialized) { "AppDataContainer not initialized. Call ensureInitialized() first." }
            return _transactionRepository
        }

    override val backupManager: BackupManager 
        get() {
            require(initialized) { "AppDataContainer not initialized. Call ensureInitialized() first." }
            return _backupManager
        }

    override val autoDetectionManager: AutoDetectionManager 
        get() {
            require(initialized) { "AppDataContainer not initialized. Call ensureInitialized() first." }
            return _autoDetectionManager
        }

    /**
     * Safely waits for initialization and returns the repository.
     * Prevents race condition crashes in background services.
     */
    suspend fun awaitRepository(): TransactionRepository {
        ensureInitialized()
        return transactionRepository
    }

    suspend fun awaitAutoDetectionManager(): AutoDetectionManager {
        ensureInitialized()
        return autoDetectionManager
    }
}
