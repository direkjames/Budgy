package com.budgy.app.util

import com.budgy.app.data.AssetKind
import com.budgy.app.data.Holding
import com.budgy.app.data.InvestTxn
import com.budgy.app.data.InvestType
import com.budgy.app.data.PriceSnap
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/*
 * Investments (V5). Cost basis uses the average-cost method: selling part of a holding
 * removes the same share of what you paid, and the rest of the sale is your realized gain.
 * Earnings (crypto rewards, MP2 dividends) add units that cost you nothing.
 */

private const val DUST = 1e-9

data class Position(
    val holding: Holding,
    val units: Double,
    /** What you paid for the units you still hold, in cents. */
    val cost: Long,
    /** Profit or loss from sales so far, in cents. */
    val realized: Long,
    /** Everything earned (rewards, dividends) in cents at the time — MP2 only; crypto rewards are counted in units. */
    val earned: Long,
    /** Price of one unit used for [value], in your main currency. */
    val price: Double,
    /** False when a crypto holding has no price yet (value falls back to what you paid). */
    val hasPrice: Boolean,
    val value: Long,
) {
    val gain get() = value - cost
    val gainPct: Double? get() = if (cost > 0) gain * 100.0 / cost else null
    val avgCost: Double get() = if (units > DUST) cost / 100.0 / units else 0.0
}

/** A fund whose price you type in another currency (e.g. an S&P 500 ETF in US dollars). */
fun Holding.hasForeignPrice(home: String): Boolean = kind == AssetKind.FUND && priceCurrency.isNotEmpty() && priceCurrency != home

/** Main-currency value of 1 unit of the fund's price currency. */
fun Holding.fxRate(): Double = fxRateMicros / 1_000_000.0

fun Holding.unitPrice(): Double = if (kind == AssetKind.MP2) 1.0 else price

/** Works out units, cost and value from the holding's history (optionally only up to [upTo], an epoch day). */
fun position(h: Holding, txns: List<InvestTxn>, upTo: Long = Long.MAX_VALUE, priceOverride: Double? = null): Position {
    var units = 0.0
    var cost = 0L
    var realized = 0L
    var earned = 0L
    for (t in txns.filter { it.holdingId == h.id && it.date <= upTo }.sortedWith(compareBy({ it.date }, { it.id }))) {
        when (t.type) {
            InvestType.BUY -> { units += t.units; cost += t.amount }
            InvestType.EARN -> { units += t.units; earned += t.amount }
            InvestType.SELL -> {
                if (units > DUST) {
                    val share = (t.units / units).coerceIn(0.0, 1.0)
                    val removed = (cost * share).roundToLong()
                    cost -= removed
                    realized += t.amount - removed
                } else {
                    realized += t.amount
                }
                units -= t.units
                if (units < DUST) { units = 0.0; cost = 0L }
            }
        }
    }
    val p = priceOverride ?: h.unitPrice()
    val hasPrice = p > 0
    val value = if (hasPrice) (units * p * 100).roundToLong() else cost
    return Position(h, units, cost, realized, earned, p, hasPrice, value)
}

data class Portfolio(val positions: List<Position>) {
    val value get() = positions.sumOf { it.value }
    val cost get() = positions.sumOf { it.cost }
    val gain get() = value - cost
    val gainPct: Double? get() = if (cost > 0) gain * 100.0 / cost else null
    val realized get() = positions.sumOf { it.realized }
}

fun portfolio(holdings: List<Holding>, txns: List<InvestTxn>): Portfolio =
    Portfolio(holdings.map { position(it, txns) })

/**
 * Total value of all investments at the end of [day], for net worth history.
 * Uses the last saved price on or before that day; without one, what you paid.
 */
fun investmentsValueOn(day: Long, holdings: List<Holding>, txns: List<InvestTxn>, snaps: List<PriceSnap>, isToday: Boolean): Long =
    holdings.sumOf { h ->
        if (isToday || h.kind == AssetKind.MP2) {
            position(h, txns, day).value
        } else {
            val snap = snaps.lastOrNull { it.holdingId == h.id && it.date <= day }?.price
            val pos = position(h, txns, day, priceOverride = snap ?: 0.0)
            pos.value // falls back to cost when there's no saved price
        }
    }

/** MP2 runs for 5 years. */
fun mp2Maturity(start: Long): Long = LocalDate.ofEpochDay(start).plusYears(5).toEpochDay()

/**
 * Rough MP2 value at maturity: today's balance growing at the expected rate, compounded yearly.
 * Ignores future savings, so it's a floor if you keep adding.
 */
fun mp2Projection(p: Position): Long? {
    val h = p.holding
    val maturity = h.maturityDate ?: return null
    if (h.rateBps <= 0) return null
    val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.ofEpochDay(maturity))
    if (days <= 0) return p.value
    val years = days / 365.25
    return (p.value * (1 + h.rateBps / 10_000.0).pow(years)).roundToLong()
}

// ---------- Formatting and parsing ----------

/** 0.00123400 -> "0.001234", 12 -> "12", 1234567.5 -> "1,234,567.5" */
fun formatUnits(units: Double): String {
    if (units == 0.0) return "0"
    val a = abs(units)
    val decimals = when {
        a >= 1000 -> 2
        a >= 1 -> 4
        else -> 8
    }
    val s = String.format(Locale.US, "%,.${decimals}f", units)
    return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
}

/** Price of one unit, e.g. "₱3,512,034.12" or "₱0.000123". */
fun formatPrice(price: Double, symbol: String): String {
    val a = abs(price)
    val s = when {
        a >= 1 -> String.format(Locale.US, "%,.2f", price)
        a >= 0.01 -> String.format(Locale.US, "%.4f", price)
        a == 0.0 -> "0.00"
        else -> BigDecimal(price).round(java.math.MathContext(4)).stripTrailingZeros().toPlainString()
    }
    return symbol + s
}

fun formatPct(pct: Double): String = (if (pct >= 0) "+" else "−") + String.format(Locale.US, "%.2f%%", abs(pct))

/** "0.0015" or "1,000" -> Double. Null when invalid or not positive. */
fun parseUnits(text: String): Double? =
    text.replace(",", "").trim().toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() && it < 1e15 }

/** Price typed by you: "3512000.50" -> 3512000.5 */
fun parsePrice(text: String): Double? =
    text.replace(",", "").trim().toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() && it < 1e13 }

/** "7.12" -> 712 basis points */
fun parseRateBps(text: String): Int? =
    text.replace("%", "").trim().toDoubleOrNull()?.takeIf { it in 0.0..100.0 }?.let { (it * 100).roundToLong().toInt() }

fun bpsText(bps: Int): String = String.format(Locale.US, "%.2f", bps / 100.0).trimEnd('0').trimEnd('.')

/** Plain editable text for a units value: 0.0012 -> "0.0012" (no grouping). */
fun unitsInput(units: Double): String = BigDecimal(units).round(java.math.MathContext(12)).stripTrailingZeros().toPlainString()
