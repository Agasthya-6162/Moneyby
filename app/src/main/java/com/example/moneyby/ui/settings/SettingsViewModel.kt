package com.example.moneyby.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneyby.BuildConfig
import com.example.moneyby.data.BackupManager
import com.example.moneyby.data.SecurityManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val backupManager: BackupManager,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _event = MutableStateFlow<SettingsEvent?>(null)
    val event: StateFlow<SettingsEvent?> = _event

    val uiState: StateFlow<SettingsUiState> = combine(
        _isLoading,
        securityManager.isAutoBackupEnabled,
        securityManager.autoBackupInterval
    ) { loading, autoBackup, interval ->
        SettingsUiState(
            isLoading    = loading,
            isAutoBackupEnabled = autoBackup,
            autoBackupInterval  = interval,
            appVersion   = BuildConfig.VERSION_NAME
        )
    }.combine(securityManager.resetDay) { state, rDay ->
        state.copy(resetDay = rDay)
    }.combine(securityManager.isAutoDetectionEnabled) { state, autoDetect ->
        state.copy(isAutoDetectionEnabled = autoDetect)
    }.stateIn(
        scope   = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(appVersion = BuildConfig.VERSION_NAME)
    )

    // ── Backup / Restore ──────────────────────────────────────────────────────
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = backupManager.exportAsZip(uri, null)
                _isLoading.value = false
                if (result.isSuccess) {
                    _event.value = SettingsEvent.ShowMessage("Backup successfully saved!")
                } else {
                    _event.value = SettingsEvent.ShowMessage(result.getMessageOrNull() ?: "Backup failed")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                _event.value = SettingsEvent.ShowMessage("Backup failed: ${e.message}")
            }
        }
    }

    fun exportToPublicFolder() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val backupFolder = backupManager.getPublicBackupFolder()
                val backupFile   = java.io.File(backupFolder, "manual_backup_$timestamp.zip")
                val uri = Uri.fromFile(backupFile)
                val result = backupManager.exportAsZip(uri, null)
                _isLoading.value = false
                if (result.isSuccess) {
                    _event.value = SettingsEvent.ShowMessage("Backup saved to: ${backupFile.name}")
                } else {
                    _event.value = SettingsEvent.ShowMessage(result.getMessageOrNull() ?: "Backup failed")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                _event.value = SettingsEvent.ShowMessage("Backup failed: ${e.message}")
            }
        }
    }

    fun importBackup(uri: Uri, password: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = backupManager.importFromZip(uri, password)
                _isLoading.value = false
                if (result.isSuccess) {
                    _event.value = SettingsEvent.ShowMessage("Data restored successfully!")
                } else {
                    _event.value = SettingsEvent.ShowMessage(result.getMessageOrNull() ?: "Restore failed")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                _event.value = SettingsEvent.ShowMessage("Restore failed: ${e.message}")
            }
        }
    }

    // ── Other settings ────────────────────────────────────────────────────────
    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch { securityManager.setAutoBackupEnabled(enabled) }
    }

    fun setAutoBackupInterval(intervalMillis: Long) {
        viewModelScope.launch { securityManager.setAutoBackupInterval(intervalMillis) }
    }

    fun setResetDay(day: Int) {
        viewModelScope.launch { securityManager.setResetDay(day) }
    }

    fun setAutoDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch { securityManager.setAutoDetectionEnabled(enabled) }
    }

    fun consumeEvent() { _event.value = null }
}

// ── UI state ──────────────────────────────────────────────────────────────────
data class SettingsUiState(
    val isLoading: Boolean = false,
    val isAutoBackupEnabled: Boolean = false,
    val autoBackupInterval: Long = 24 * 60 * 60 * 1000L,
    val resetDay: Int = 1,
    val isAutoDetectionEnabled: Boolean = false,
    val appVersion: String = BuildConfig.VERSION_NAME
)

sealed class SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent()
}
