package com.haustoys.pos.core

object Money {
    /** 1234567 -> "1.234.567" (Indonesian grouping, no decimals). */
    fun plain(amount: Long): String {
        val digits = kotlin.math.abs(amount).toString()
        val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
        return if (amount < 0) "-$grouped" else grouped
    }

    /** 25000 -> "Rp 25.000" */
    fun rp(amount: Long): String = "Rp " + plain(amount)

    /** Parses user input such as "25.000", "Rp25000" or "25,000"; null when empty or invalid. */
    fun parse(text: String): Long? {
        val digits = text.filter { it.isDigit() }
        if (digits.isEmpty() || digits.length > 15) return null
        return digits.toLong()
    }
}
