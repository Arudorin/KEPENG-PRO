package com.example.core.money

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {
    private val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        monetaryDecimalSeparator = ','
    }
    private val rupiahFormat = DecimalFormat("#,##0", symbols)

    /**
     * Format raw Long rupiah amount to "Rp 1.250.000"
     */
    fun format(amount: Long, withPrefix: Boolean = true): String {
        val formatted = rupiahFormat.format(kotlin.math.abs(amount))
        return if (withPrefix) "Rp $formatted" else formatted
    }

    /**
     * Format with sign, e.g. "+Rp 1.250.000" or "-Rp 50.000"
     */
    fun formatWithSign(amount: Long, isExpense: Boolean): String {
        val sign = if (isExpense) "-" else "+"
        return "$sign${format(amount, withPrefix = true)}"
    }
}

object MoneyParser {
    /**
     * Parses user input strings like "25rb", "25k", "1,5jt", "1.5jt", "25000", "Rp 25.000" into Long
     */
    fun parse(input: String): Long {
        val clean = input.trim().lowercase(Locale.ROOT)
            .replace("rp", "")
            .replace(" ", "")
            .trim()

        if (clean.isEmpty()) return 0L

        // Check for 'jt' or 'juta'
        if (clean.endsWith("jt") || clean.endsWith("juta") || clean.endsWith("m")) {
            val numStr = clean.replace("juta", "").replace("jt", "").replace("m", "").replace(",", ".")
            val value = numStr.toDoubleOrNull() ?: 0.0
            return (value * 1_000_000).toLong()
        }

        // Check for 'rb', 'k', 'ribu'
        if (clean.endsWith("rb") || clean.endsWith("ribu") || clean.endsWith("k")) {
            val numStr = clean.replace("ribu", "").replace("rb", "").replace("k", "").replace(",", ".")
            val value = numStr.toDoubleOrNull() ?: 0.0
            return (value * 1_000).toLong()
        }

        // Regular digits with dots or commas
        val digitsOnly = clean.replace(".", "").replace(",", "")
        return digitsOnly.toLongOrNull() ?: 0L
    }
}
