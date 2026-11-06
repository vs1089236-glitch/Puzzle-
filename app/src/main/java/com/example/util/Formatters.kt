package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val indianSymbols = DecimalFormatSymbols(Locale("en", "IN")).apply {
        currencySymbol = "₹"
    }
    private val inrDecimalFormat = DecimalFormat("₹#,##,##0.00", indianSymbols)
    private val inrWholeFormat = DecimalFormat("₹#,##,##0", indianSymbols)
    private val dateTimeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    fun formatInr(amount: Double, showDecimals: Boolean = true): String {
        return if (showDecimals) {
            inrDecimalFormat.format(amount)
        } else {
            inrWholeFormat.format(amount)
        }
    }

    fun formatCompactInr(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            inrWholeFormat.format(amount)
        } else {
            inrDecimalFormat.format(amount)
        }
    }

    fun formatTimestamp(epochMillis: Long): String {
        return dateTimeFormat.format(Date(epochMillis))
    }

    fun generateUtr(): String {
        val randomPart = (100000000000L..999999999999L).random()
        return "UTR$randomPart"
    }
}
