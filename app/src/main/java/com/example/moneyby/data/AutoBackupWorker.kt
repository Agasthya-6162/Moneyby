package com.example.moneyby.data

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? com.example.moneyby.MoneybyApplication
                ?: return Result.retry()
            
            (app.container as? com.example.moneyby.AppDataContainer)?.ensureInitialized()
            val backupManager = app.container.backupManager
            val securityManager = app.securityManager

            // Use timeout to prevent worker from hanging
            val isEnabled = withTimeoutOrNull(5000) {
                securityManager.isAutoBackupEnabled.first()
            } ?: return Result.retry()
            
            if (!isEnabled) return Result.success()

            val password = withTimeoutOrNull(5000) {
                securityManager.backupPassword.first()
            } ?: return Result.retry()
            
            if (password.isNullOrBlank()) return Result.success()
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFolder = backupManager.getPublicBackupFolder()
            val backupFile = File(backupFolder, "auto_backup_$timestamp.zip")
            val uri = Uri.fromFile(backupFile)

            val result = backupManager.exportAsZip(uri, password)
            
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
