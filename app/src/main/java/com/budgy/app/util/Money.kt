package com.budgy.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

object Money {
    val currencies = listOf(
        "₱" to "Philippine Peso (PHP)",
        "$" to "US Dollar (USD)",
        "€" to "Euro (EUR)",
        "£" to "British Pound (GBP)",
        "¥" to "Japanese Yen (JPY)",
        "₩" to "Korean Won (KRW)",
        "₹" to "Indian Rupee (INR)",
        "S$" to "Singapore Dollar (SGD)",
        "A$" to "Australian Dollar (AUD)",
    )

    /** 123456 -> "₱1,234.56" ; with sign: "+₱1,234.56" / "-₱1,234.56" */
    fun format(cents: Long, symbol: String, showPlus: Boolean = false): String {
        val sign = when {
            cents < 0 -> "-"
            showPlus && cents > 0 -> "+"
            else -> ""
        }
        val a = abs(cents)
        val whole = String.format(Locale.US, "%,d", a / 100)
        val frac = (a % 100).toString().padStart(2, '0')
        return "$sign$symbol$whole.$frac"
    }

    /** Compact for chart labels: ₱12.4k */
    fun compact(cents: Long, symbol: String): String {
        val v = abs(cents) / 100.0
        val s = when {
            v >= 1_000_000 -> String.format(Locale.US, "%.1fM", v / 1_000_000)
            v >= 10_000 -> String.format(Locale.US, "%.1fk", v / 1_000)
            else -> String.format(Locale.US, "%,.0f", v)
        }
        return (if (cents < 0) "-" else "") + symbol + s
    }

    /** "1,234.5" or "1234.50" -> 123450 cents. Null when invalid. Allows a leading minus when [allowNegative]. */
    fun parse(text: String, allowNegative: Boolean = false): Long? {
        val clean = text.replace(",", "").trim()
        if (clean.isEmpty() || clean == "-" || clean == ".") return null
        return try {
            val v = BigDecimal(clean).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
            if (!allowNegative && v < 0) null else v
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** cents -> plain editable text, e.g. 123450 -> "1234.50", 120000 -> "1200" */
    fun toInput(cents: Long): String {
        val a = abs(cents)
        val sign = if (cents < 0) "-" else ""
        return if (a % 100 == 0L) "$sign${a / 100}" else "$sign${a / 100}.${(a % 100).toString().padStart(2, '0')}"
    }

    /** Groups the integer part of a keypad string while typing: "1234.5" -> "1,234.5" */
    fun groupTyping(text: String): String {
        if (text.isEmpty()) return "0"
        val parts = text.split(".")
        val whole = parts[0].toLongOrNull()?.let { String.format(Locale.US, "%,d", it) } ?: parts[0]
        return if (parts.size > 1) "$whole.${parts[1]}" else whole
    }
}
