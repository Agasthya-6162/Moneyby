package com.example.moneyby.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import java.util.Base64
import at.favre.lib.crypto.bcrypt.BCrypt


private val Context.dataStore by preferencesDataStore(name = "security_prefs")

class SecurityManager(context: Context) {
    private val appContext = context.applicationContext

    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        appContext,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private val PIN_KEY = stringPreferencesKey("app_pin")
        private val DB_PASSPHRASE_KEY = stringPreferencesKey("db_passphrase")
        private val BIOMETRIC_ENABLED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("biometric_enabled")
        private val BACKUP_PASSWORD_KEY = stringPreferencesKey("backup_password")
        private val AUTO_BACKUP_ENABLED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("auto_backup_enabled")
        private val AUTO_BACKUP_INTERVAL_KEY = androidx.datastore.preferences.core.longPreferencesKey("auto_backup_interval")
        private val RESET_DAY_KEY = androidx.datastore.preferences.core.intPreferencesKey("reset_day")
        private val AUTO_DETECTION_ENABLED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("auto_detection_enabled")

        private const val ENC_DB_PASSPHRASE = "enc_db_passphrase"
        private const val ENC_BACKUP_PASSWORD = "enc_backup_password"
        private const val ENC_PIN = "enc_app_pin"
        private const val ENC_BIOMETRIC_ENABLED = "enc_biometric_enabled"
        private val REFRESH_TRIGGER = androidx.datastore.preferences.core.intPreferencesKey("security_refresh_trigger")
    }


    val appPin: Flow<String?> = appContext.dataStore.data.map { preferences ->
        encryptedPrefs.getString(ENC_PIN, null) ?: preferences[PIN_KEY]
    }

    val isBiometricEnabled: Flow<Boolean> = appContext.dataStore.data.map { preferences ->
        if (encryptedPrefs.contains(ENC_BIOMETRIC_ENABLED)) {
            encryptedPrefs.getBoolean(ENC_BIOMETRIC_ENABLED, false)
        } else {
            preferences[BIOMETRIC_ENABLED_KEY] ?: false
        }
    }

    val backupPassword: Flow<String?> = appContext.dataStore.data.map { preferences ->
        encryptedPrefs.getString(ENC_BACKUP_PASSWORD, null) ?: preferences[BACKUP_PASSWORD_KEY]
    }

    val isAutoBackupEnabled: Flow<Boolean> = appContext.dataStore.data.map { preferences ->
        preferences[AUTO_BACKUP_ENABLED_KEY] ?: false
    }

    val autoBackupInterval: Flow<Long> = appContext.dataStore.data.map { preferences ->
        preferences[AUTO_BACKUP_INTERVAL_KEY] ?: 24 * 60 * 60 * 1000L // Default 24 hours
    }

    val resetDay: Flow<Int> = appContext.dataStore.data.map { preferences ->
        preferences[RESET_DAY_KEY] ?: 1 // Default to 1st of month
    }

    val isAutoDetectionEnabled: Flow<Boolean> = appContext.dataStore.data.map { preferences ->
        preferences[AUTO_DETECTION_ENABLED_KEY] ?: false
    }



    suspend fun setPin(pin: String) {
        val hashedPin = BCrypt.withDefaults().hashToString(12, pin.toCharArray())
        encryptedPrefs.edit().putString(ENC_PIN, hashedPin).apply()
        
        // Trigger Flow update
        appContext.dataStore.edit { preferences ->
            preferences[PIN_KEY]?.let { preferences.remove(PIN_KEY) }
            val current = preferences[REFRESH_TRIGGER] ?: 0
            preferences[REFRESH_TRIGGER] = current + 1
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val storedValue = appPin.first() ?: return false
        
        // Check if the stored value looks like a BCrypt hash
        return if (storedValue.startsWith("$2a$") || storedValue.startsWith("$2y$") || storedValue.startsWith("$2b$")) {
            try {
                val result = BCrypt.verifyer().verify(pin.toCharArray(), storedValue)
                result.verified
            } catch (e: Exception) {
                false
            }
        } else {
            // Legacy plain text PIN
            if (storedValue == pin) {
                // Migrate to hashed PIN
                setPin(pin)
                true
            } else {
                false
            }
        }
    }



    suspend fun setBiometricEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(ENC_BIOMETRIC_ENABLED, enabled).apply()
        
        // Trigger Flow update
        appContext.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY]?.let { preferences.remove(BIOMETRIC_ENABLED_KEY) }
            val current = preferences[REFRESH_TRIGGER] ?: 0
            preferences[REFRESH_TRIGGER] = current + 1
        }
    }

    suspend fun setBackupPassword(password: String?) {
        if (password == null) {
            encryptedPrefs.edit().remove(ENC_BACKUP_PASSWORD).commit()
        } else {
            encryptedPrefs.edit().putString(ENC_BACKUP_PASSWORD, password).commit()
        }
        
        appContext.dataStore.edit { preferences ->
            preferences.remove(BACKUP_PASSWORD_KEY)
        }
    }

    suspend fun migrateLegacySecretsIfNeeded() {
        val preferences = appContext.dataStore.data.first()
        val legacyPassphrase = preferences[DB_PASSPHRASE_KEY]
        val legacyBackup = preferences[BACKUP_PASSWORD_KEY]
        val legacyPin = preferences[PIN_KEY]
        val legacyBiometric = preferences[BIOMETRIC_ENABLED_KEY]

        val edit = encryptedPrefs.edit()
        var needsUpdate = false

        if (legacyPassphrase != null && encryptedPrefs.getString(ENC_DB_PASSPHRASE, null) == null) {
            edit.putString(ENC_DB_PASSPHRASE, legacyPassphrase)
            needsUpdate = true
        }
        if (legacyBackup != null && encryptedPrefs.getString(ENC_BACKUP_PASSWORD, null) == null) {
            edit.putString(ENC_BACKUP_PASSWORD, legacyBackup)
            needsUpdate = true
        }
        if (legacyPin != null && encryptedPrefs.getString(ENC_PIN, null) == null) {
            edit.putString(ENC_PIN, legacyPin)
            needsUpdate = true
        }
        if (legacyBiometric != null && !encryptedPrefs.contains(ENC_BIOMETRIC_ENABLED)) {
            edit.putBoolean(ENC_BIOMETRIC_ENABLED, legacyBiometric)
            needsUpdate = true
        }

        if (needsUpdate) {
            edit.commit()
        }

        if (legacyPassphrase != null || legacyBackup != null || legacyPin != null || legacyBiometric != null) {
            appContext.dataStore.edit { prefs ->
                prefs.remove(DB_PASSPHRASE_KEY)
                prefs.remove(BACKUP_PASSWORD_KEY)
                prefs.remove(PIN_KEY)
                prefs.remove(BIOMETRIC_ENABLED_KEY)
                
                val current = prefs[REFRESH_TRIGGER] ?: 0
                prefs[REFRESH_TRIGGER] = current + 1
            }
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_ENABLED_KEY] = enabled
        }
    }

    suspend fun setAutoBackupInterval(intervalMillis: Long) {
        appContext.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_INTERVAL_KEY] = intervalMillis
        }
    }

    suspend fun setResetDay(day: Int) {
        appContext.dataStore.edit { preferences ->
            preferences[RESET_DAY_KEY] = day
        }
    }

    suspend fun setAutoDetectionEnabled(enabled: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[AUTO_DETECTION_ENABLED_KEY] = enabled
        }
    }



    fun isBiometricAvailable(): Boolean {
        val biometricManager = androidx.biometric.BiometricManager.from(appContext)
        return biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun getOrCreatePassphrase(): String {
        val encrypted = encryptedPrefs.getString(ENC_DB_PASSPHRASE, null)
        if (encrypted != null) return encrypted

        val preferences = appContext.dataStore.data.first()
        val legacy = preferences[DB_PASSPHRASE_KEY]
        if (legacy != null) {
            encryptedPrefs.edit().putString(ENC_DB_PASSPHRASE, legacy).apply()
            appContext.dataStore.edit { it.remove(DB_PASSPHRASE_KEY) }
            return legacy
        }

        val newPassphrase = generateRandomPassphrase()
        encryptedPrefs.edit().putString(ENC_DB_PASSPHRASE, newPassphrase).commit()
        return newPassphrase
    }

    private fun generateRandomPassphrase(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }
}
