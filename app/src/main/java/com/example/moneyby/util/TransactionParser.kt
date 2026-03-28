package com.example.moneyby.util

import java.util.regex.Pattern


data class ParsedTransaction(
    val amount: Double,
    val merchant: String?,
    val accountSuffix: String?,
    val type: String // "Income" or "Expense"
)

object TransactionParser {
    data class BankPattern(
        val regex: Pattern,
        val amountGroup: Int,
        val suffixGroup: Int? = null,
        val merchantGroup: Int? = null,
        val type: String
    )

    private val PATTERNS = listOf(
        // CBoI: A/c 5XXXXX7638 debited by Rs. 1
        BankPattern(
            regex = Pattern.compile("(?i)a/c\\s*([a-z0-9x*.-]+)\\s*debited\\s*by\\s*(?:rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?)"),
            suffixGroup = 1,
            amountGroup = 2,
            type = "Expense"
        ),
        // CBoI: A/c XX7638 credited by Rs. 1.00
        BankPattern(
            regex = Pattern.compile("(?i)a/c\\s*([a-z0-9x*.-]+)\\s*credited\\s*by\\s*(?:rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?)"),
            suffixGroup = 1,
            amountGroup = 2,
            type = "Income"
        ),
        // Rs. 100.00 debited from A/c XXXX...
        BankPattern(
            regex = Pattern.compile("(?i)(?:rs\\.?|inr)\\s*(\\d+(?:\\.\\d+)?)\\s*debited\\s*from\\s*a/c\\s*([a-z0-9x*.]+)", Pattern.CASE_INSENSITIVE),
            amountGroup = 1,
            suffixGroup = 2,
            type = "Expense"
        ),
        // Rs. 100.00 credited to A/c XXXX...
        BankPattern(
            regex = Pattern.compile("(?i)(?:rs\\.?|inr)\\s*(\\d+(?:\\.\\d+)?)\\s*credited\\s*to\\s*a/c\\s*([a-z0-9x*.]+)", Pattern.CASE_INSENSITIVE),
            amountGroup = 1,
            suffixGroup = 2,
            type = "Income"
        ),
        // Spent Rs. 100.00 at MERCHANT...
        BankPattern(
            regex = Pattern.compile("(?i)spent\\s*(?:rs\\.?|inr)\\s*(\\d+(?:\\.\\d+)?)\\s*at\\s*([^.]+)"),
            amountGroup = 1,
            type = "Expense"
        ),
        // Received Rs. 100.00 from SENDER...
        BankPattern(
            regex = Pattern.compile("(?i)received\\s*(?:rs\\.?|inr)\\s*(\\d+(?:\\.\\d+)?)\\s*from\\s*([^.]+)"),
            amountGroup = 1,
            merchantGroup = 2,
            type = "Income"
        ),
        // Transaction of Rs. 100.00 on A/c XXXX... at MERCHANT
        BankPattern(
            regex = Pattern.compile("(?i)transaction\\s*of\\s*(?:rs\\.?|inr)\\s*(\\d+(?:\\.\\d+)?).*?a/c\\s*(\\d+).*?at\\s*([^.]+)"),
            amountGroup = 1,
            suffixGroup = 2,
            merchantGroup = 3,
            type = "Expense"
        ),
        // UPI-OUT/Rs.100.00/MERCHANT
        BankPattern(
            regex = Pattern.compile("(?i)upi(?:-out)?/(?:rs\\.?|inr)\\s*(\\d+(?:\\.\\d+)?)/([^/]+)"),
            amountGroup = 1,
            merchantGroup = 2,
            type = "Expense"
        )
    )

    fun parse(text: String): ParsedTransaction? {
        for (p in PATTERNS) {
            val matcher = p.regex.matcher(text)
            if (matcher.find()) {
                val amount = matcher.group(p.amountGroup)?.toDoubleOrNull() ?: continue
                val rawSuffix = p.suffixGroup?.let { matcher.group(it) }
                val suffix = rawSuffix?.filter { it.isDigit() }?.takeLast(4)
                
                val merchant = p.merchantGroup?.let { matcher.group(it) }?.trim() 
                    ?: extractMerchant(text, p.type)

                val result = ParsedTransaction(
                    amount = amount,
                    merchant = merchant,
                    accountSuffix = if (suffix?.length == 4) suffix else null,
                    type = p.type
                )
                return result
            }
        }
        return null
    }

    private fun extractMerchant(text: String, type: String): String? {
        // Cache compiled patterns to avoid recompilation
        val patterns = if (type == "Income") {
            listOf(
                INCOME_FROM_PATTERN,
                INCOME_CREDITED_PATTERN
            )
        } else {
            listOf(
                EXPENSE_AT_PATTERN,
                EXPENSE_TO_PATTERN,
                EXPENSE_PAID_PATTERN,
                EXPENSE_TRANSFER_PATTERN
            )
        }

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val found = matcher.group(1)?.trim()
                if (!found.isNullOrBlank() && !found.contains("A/C", ignoreCase = true)) {
                    return found
                }
            }
        }
        return null
    }

    // Cache compiled regex patterns
    private val INCOME_FROM_PATTERN = java.util.regex.Pattern.compile("(?i)from\\s+([^.]+?)(?:\\s+via|\\s+on|\\s+ref|$)")
    private val INCOME_CREDITED_PATTERN = java.util.regex.Pattern.compile("(?i)credited\\s+by\\s+([^.]+?)(?:\\s+via|\\s+on|$)")
    
    private val EXPENSE_AT_PATTERN = java.util.regex.Pattern.compile("(?i)at\\s+([^.]+?)(?:\\s+on|\\s+via|\\s+ref|$)")
    private val EXPENSE_TO_PATTERN = java.util.regex.Pattern.compile("(?i)to\\s+([^.]+?)(?:\\s+on|\\s+via|\\s+ref|$)")
    private val EXPENSE_PAID_PATTERN = java.util.regex.Pattern.compile("(?i)paid\\s+to\\s+([^.]+?)(?:\\s+on|\\s+via|\\s+ref|$)")
    private val EXPENSE_TRANSFER_PATTERN = java.util.regex.Pattern.compile("(?i)transfer\\s+to\\s+([^.]+?)(?:\\s+on|\\s+via|\\s+ref|$)")
}
