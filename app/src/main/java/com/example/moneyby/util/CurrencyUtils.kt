package com.example.moneyby.util

import java.text.NumberFormat
import java.util.Locale

private val rupeeLocale = Locale.forLanguageTag("en-IN")
private val currencyFormat = NumberFormat.getCurrencyInstance(rupeeLocale)

fun formatCurrency(amount: Double): String {
    return currencyFormat.format(amount)
}
