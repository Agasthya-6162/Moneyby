package com.example.moneyby.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionParserTest {

    @Test
    fun parseIncomeTransaction_correctlyIdentifies() {
        val text = "Received Rs. 5000 from Boss for Project X"
        val result = TransactionParser.parse(text)

        assertNotNull(result)
        assertEquals(5000.0, result?.amount)
        assertEquals("Boss for Project X", result?.merchant)
        assertEquals("Income", result?.type)
    }

    @Test
    fun parseExpenseTransaction_correctlyIdentifies() {
        val text = "Spent Rs. 150.50 at Cafe Mocha on 12/Oct"
        val result = TransactionParser.parse(text)

        assertNotNull(result)
        assertEquals(150.5, result?.amount)
        assertEquals("Cafe Mocha", result?.merchant)
        assertEquals("Expense", result?.type)
    }

    @Test
    fun parseAccountDebited_correctlyIdentifies() {
        val text = "Rs. 2000.00 debited from A/c XX1234 on 5th June"
        val result = TransactionParser.parse(text)

        assertNotNull(result)
        assertEquals(2000.0, result?.amount)
        assertEquals("Expense", result?.type)
        assertEquals("1234", result?.accountSuffix)
    }

    @Test
    fun parseUPITransaction_correctlyIdentifies() {
        val text = "UPI-OUT/Rs.500.00/Grocery Store/some_ref"
        val result = TransactionParser.parse(text)

        assertNotNull(result)
        assertEquals(500.0, result?.amount)
        assertEquals("Grocery Store", result?.merchant)
        assertEquals("Expense", result?.type)
    }

    @Test
    fun parseInvalidText_returnsNull() {
        val text = "Hello this is a regular message from a friend"
        val result = TransactionParser.parse(text)

        assertNull(result)
    }
}
