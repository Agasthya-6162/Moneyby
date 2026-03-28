package com.example.moneyby.util

import com.example.moneyby.data.PendingTransaction
import com.example.moneyby.domain.repository.TransactionRepository
import com.example.moneyby.data.SecurityManager
import kotlinx.coroutines.flow.first

import java.security.MessageDigest

class AutoDetectionManager(
    private val repository: TransactionRepository,
    private val securityManager: SecurityManager
) {
    // In-memory cache to prevent near-simultaneous duplicate processing (e.g. SMS + Notification)
    private val recentlyProcessedHashes = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val CACHE_LIMIT = 50

    suspend fun processDetectedTransaction(parsed: ParsedTransaction, rawText: String) {
        try {
            // 0. Check if Auto-Detection is enabled - use non-blocking approach
            val isEnabled = try {
                securityManager.isAutoDetectionEnabled.first()
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }
            
            if (!isEnabled) {
                return
            }

            // 1. Generate Stable Hash for Deduplication
            val timestamp = System.currentTimeMillis()
            val dateBucket = timestamp / (1000 * 60 * 60 * 2) // 2 hour bucket
            val hashInput = "${parsed.amount}_${parsed.merchant ?: "unknown"}_${parsed.type}_${parsed.accountSuffix ?: "none"}_$dateBucket"
            val hash = sha256(hashInput)

            // 2. Concurrency Guard (In-Memory)
            if (recentlyProcessedHashes.contains(hash)) {
                return
            }
            
            // Add to memory cache (with simple size management)
            if (recentlyProcessedHashes.size > CACHE_LIMIT) {
                recentlyProcessedHashes.clear()
            }
            recentlyProcessedHashes.add(hash)

            // 3. Database Check (Confirmed and Pending)
            try {
                if (repository.getTransactionByHash(hash) != null || repository.getPendingTransactionByHash(hash) != null) {
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }

            // 4. Predict Category
            val category = predictCategory(parsed.merchant ?: "", parsed.type)

            // 5. Create and Insert Pending Transaction
            val pending = PendingTransaction(
                amount = parsed.amount,
                category = category,
                date = timestamp,
                type = parsed.type,
                accountSuffix = parsed.accountSuffix,
                merchant = parsed.merchant,
                rawText = rawText,
                transactionHash = hash
            )

            try {
                repository.insertPendingTransaction(pending)
            } catch (e: Exception) {
                e.printStackTrace()
                // Intentionally ignore duplicate races
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // General error handling - don't crash on any parsing issue
        }
    }




    private fun predictCategory(merchant: String, type: String): String {
        val m = merchant.uppercase()
        
        if (type == "Income") {
            return when {
                m.contains("SALARY") || m.contains("MONTHLY") || m.contains("PAYROLL") || m.contains("WAGES") -> "Salary"
                m.contains("INTEREST") || m.contains("DIVIDEND") || m.contains("FD") || m.contains("RD") || m.contains("SAVINGS") -> "Investment"
                m.contains("REFUND") || m.contains("CASHBACK") || m.contains("REWARD") || m.contains("PAYTM") || m.contains("GPAY") -> "Others" // Using "Others" as per user preference
                m.contains("GIFT") || m.contains("BIRTHDAY") -> "Gift"
                else -> "Others"
            }
        }

        // Expense Categorization
        return when {
            // Food & Dining
            m.contains("ZOMATO") || m.contains("SWIGGY") || m.contains("RESTAURANT") || m.contains("FOOD") || 
            m.contains("CAFE") || m.contains("BAKERY") || m.contains("DOMINOS") || m.contains("PIZZA") || 
            m.contains("KFC") || m.contains("MCDONALDS") || m.contains("DINING") || m.contains("EAT") -> "Food"
            
            // Transport & Travel
            m.contains("UBER") || m.contains("OLA") || m.contains("PETROL") || m.contains("SHELL") || 
            m.contains("FUEL") || m.contains("HPCL") || m.contains("BPCL") || m.contains("INDIANOIL") || 
            m.contains("METRO") || m.contains("RAILWAY") || m.contains("IRCTC") || m.contains("AIRLINES") || 
            m.contains("FLIGHT") || m.contains("TRAVEL") || m.contains("CAB") -> "Transport"
            
            // Shopping
            m.contains("AMAZON") || m.contains("FLIPKART") || m.contains("MYNTRA") || m.contains("AJIO") || 
            m.contains("RELIANCE") || m.contains("MART") || m.contains("DMART") || m.contains("STORE") || 
            m.contains("MALL") || m.contains("FASHION") || m.contains("TEXTILE") || m.contains("NYKAA") -> "Shopping"
            
            // Health
            m.contains("HOSPITAL") || m.contains("PHARMACY") || m.contains("APOLLO") || m.contains("MEDICAL") || 
            m.contains("CLINIC") || m.contains("HEALTH") || m.contains("DOCTOR") || m.contains("LAB") -> "Health"
            
            // Entertainment
            m.contains("NETFLIX") || m.contains("PRIME") || m.contains("CINEMA") || m.contains("PVR") || 
            m.contains("BOOKMYSHOW") || m.contains("HOTSTAR") || m.contains("SPOTIFY") || m.contains("GAMING") ||
            m.contains("INOX") || m.contains("THEATRE") -> "Entertainment"
            
            // Bills & Utilities
            m.contains("RECHARGE") || m.contains("AIRTEL") || m.contains("JIO") || m.contains("BILL") || 
            m.contains("ELECTRICITY") || m.contains("WATER") || m.contains("GAS") || m.contains("MSEB") || 
            m.contains("INSURANCE") || m.contains("BROADBAND") || m.contains("WIFI") || m.contains("MOBILE") -> "Utilities"
            
            // Default to "Others" (or user mentioned "Deduction" always as Others)
            else -> "Others"
        }
    }


    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
