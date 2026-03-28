package com.example.moneyby.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.moneyby.MoneybyApplication
import com.example.moneyby.util.TransactionParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            val app = context.applicationContext as? MoneybyApplication
            
            if (app == null) {
                pendingResult.finish()
                return
            }
            
            app.applicationScope.launch {
                try {
                    // Use timeout to prevent blocking
                    val isEnabled = withTimeoutOrNull(3000) {
                        app.securityManager.isAutoDetectionEnabled.first()
                    } ?: false
                    
                    if (!isEnabled) {
                        return@launch
                    }
                    
                    val container = app.container as? com.example.moneyby.AppDataContainer
                    val autoDetectionManager = container?.awaitAutoDetectionManager() ?: return@launch

                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    for (msg in messages) {
                        try {
                            val body = msg.messageBody ?: continue
                            
                            val parsed = TransactionParser.parse(body)
                            if (parsed != null) {
                                autoDetectionManager.processDetectedTransaction(parsed, body)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Continue processing other messages
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
