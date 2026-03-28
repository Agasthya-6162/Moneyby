package com.example.moneyby.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun `formatCurrency formats positive amounts correctly`() {
        val amount = 1234.56
        val formatted = formatCurrency(amount)
        // Since it uses the "en-IN" locale, it should insert commas correctly (like 1,234.56)
        // The exact symbol might differ depending on JDK version (can be "₹" or "Rs.").
        // We can just verify it contains the formatted numbers.
        assert(formatted.contains("1,234.56"))
    }

    @Test
    fun `formatCurrency formats zero correctly`() {
        val amount = 0.0
        val formatted = formatCurrency(amount)
        assert(formatted.contains("0.00"))
    }

    @Test
    fun `formatCurrency formats large amounts correctly`() {
        val amount = 10000000.0
        val formatted = formatCurrency(amount)
        // Indian number system format: 1,00,00,000.00 or generic 10,000,000.00 based on JDK
        assert(formatted.contains("1,00,00,000") || formatted.contains("10,000,000"))
    }

    @Test
    fun `formatCurrency formats negative amounts correctly`() {
        val amount = -500.25
        val formatted = formatCurrency(amount)
        // Check for minus sign or parenthesis based on format, but definitely the number
        assert(formatted.contains("500.25"))
        assert(formatted.contains("-") || formatted.contains("("))
    }
}
