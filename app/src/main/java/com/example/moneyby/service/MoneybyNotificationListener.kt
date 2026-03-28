package com.example.moneyby.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.moneyby.MoneybyApplication
import com.example.moneyby.util.TransactionParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MoneybyNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val app = application as? MoneybyApplication ?: return
        
        app.applicationScope.launch {
            try {
                // Use timeout to prevent blocking
                val isEnabled = withTimeoutOrNull(3000) {
                    app.securityManager.isAutoDetectionEnabled.first()
                } ?: false
                
                if (!isEnabled) return@launch
                val container = app.container as? com.example.moneyby.AppDataContainer
                val autoDetectionManager = container?.awaitAutoDetectionManager() ?: return@launch

                val extras = sbn.notification.extras
                val title = extras?.getString("android.title") ?: ""
                val text = extras?.getCharSequence("android.text")?.toString() ?: ""
                val bigText = extras?.getCharSequence("android.bigText")?.toString() ?: ""
                
                val fullContent = "$title $text $bigText"
                
                if (fullContent.isNotBlank()) {
                    val parsed = TransactionParser.parse(fullContent)
                    if (parsed != null) {
                        autoDetectionManager.processDetectedTransaction(parsed, fullContent)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
