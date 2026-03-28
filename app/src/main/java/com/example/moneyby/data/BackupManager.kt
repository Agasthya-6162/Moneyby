package com.example.moneyby.data

import android.content.Context
import android.net.Uri
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.io.FileOutputStream
import com.example.moneyby.domain.repository.TransactionRepository
import com.example.moneyby.util.Result

class BackupManager(
    context: Context,
    private val repository: TransactionRepository
) {
    private val appContext = context.applicationContext
    /**
     * Creates and returns the public "Moneyby/Backups" directory in Documents.
     * Fallback to internal storage if external is not available.
     */
    fun getPublicBackupFolder(): File {
        val backupDir = File(appContext.getExternalFilesDir(null), "Backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        return backupDir
    }

    private val moshi = Moshi.Builder()
        .build()
    private val backupAdapter = moshi.adapter(BackupData::class.java)
    private val metaAdapter = moshi.adapter(BackupMetaData::class.java)

    suspend fun exportAsZip(uri: Uri, password: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        var tempDir: File? = null
        var zipFile: File? = null
        var zip: ZipFile? = null
        try {
            // Check for available space (Roughly 10MB minimum for a safety margin)
            if (appContext.cacheDir.usableSpace < 10 * 1024 * 1024) {
                return@withContext Result.Error(Exception("Not enough storage space available for export"))
            }

            val backupData = repository.getBackupData()
            val metaData = BackupMetaData(
                appVersion = appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "1.0"
            )

            val dataJson = backupAdapter.toJson(backupData)
            val metaJson = metaAdapter.toJson(metaData)

            tempDir = File(appContext.cacheDir, "backup_temp")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            val dataFile = File(tempDir, "data.json")
            dataFile.writeText(dataJson)

            val metaFile = File(tempDir, "meta.json")
            metaFile.writeText(metaJson)

            zipFile = File(appContext.cacheDir, "backup.zip")
            if (zipFile.exists()) zipFile.delete()

            zip = ZipFile(zipFile)
            val parameters = ZipParameters()
            if (!password.isNullOrBlank()) {
                parameters.isEncryptFiles = true
                parameters.encryptionMethod = EncryptionMethod.AES
                parameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                zip.setPassword(password.toCharArray())
            }

            zip.addFiles(listOf(dataFile, metaFile), parameters)
            zip.close()
            zip = null

            appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                zipFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            Result.Success(Unit)
        } catch (e: SecurityException) {
            Result.Error(e, "Permission nahi mili. Settings me storage access allow karein.")
        } catch (e: java.io.IOException) {
            Result.Error(e, "Storage me file save nahi ho paayi. Space check karein.")
        } catch (e: Exception) {
            Result.Error(e, "Backup fail ho gaya: ${e.message ?: "Unknown error"}")
        } finally {
            zip?.close()
            tempDir?.deleteRecursively()
            zipFile?.delete()
        }
    }

    suspend fun importFromZip(uri: Uri, password: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        var tempZipFile: File? = null
        var extractDir: File? = null
        var zip: ZipFile? = null
        try {
            tempZipFile = File(appContext.cacheDir, "import_temp.zip")
            appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempZipFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw Exception("Backup file open nahi ho paayi.")

            zip = ZipFile(tempZipFile)
            if (zip.isEncrypted) {
                if (password.isNullOrBlank()) {
                    throw Exception("Encrypt backup ke liye password zaroori hai.")
                }
                zip.setPassword(password.toCharArray())
            }

            extractDir = File(appContext.cacheDir, "extract_temp")
            if (extractDir.exists()) extractDir.deleteRecursively()
            extractDir.mkdirs()

            zip.extractAll(extractDir.absolutePath)
            zip.close()
            zip = null

            val dataFile = File(extractDir, "data.json")
            if (!dataFile.exists()) throw Exception("Invalid backup: data.json file nahi mili.")
            
            val dataJson = dataFile.readText()
            val backupData = backupAdapter.fromJson(dataJson) 
                ?: throw Exception("Backup data parse nahi ho paaya.")
            
            val restoreResult = repository.restoreData(backupData)
            if (restoreResult is Result.Error) {
                throw restoreResult.exception
            }

            Result.Success(Unit)
        } catch (e: net.lingala.zip4j.exception.ZipException) {
            Result.Error(e, "Backup file me dikkat hai ya password galat hai.")
        } catch (e: Exception) {
            Result.Error(e, "Restore fail ho gaya: ${e.message ?: "Unknown error"}")
        } finally {
            zip?.close()
            extractDir?.deleteRecursively()
            tempZipFile?.delete()
        }
    }
}
