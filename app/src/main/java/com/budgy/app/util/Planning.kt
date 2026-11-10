package com.budgy.app.util

import com.budgy.app.i18n.tr
import com.budgy.app.data.Account
import com.budgy.app.data.Budget
import com.budgy.app.data.Debt
import com.budgy.app.data.EnvelopeMove
import com.budgy.app.data.TxType
import com.budgy.app.data.Txn
import com.budgy.app.data.Holding
import com.budgy.app.data.InvestTxn
import com.budgy.app.data.PriceSnap
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

// ---------------- Debt payoff ----------------

enum class PayoffStrategy(private val enLabel: String, private val enBlurb: String) {
    SNOWBALL("Snowball", "Smallest balance first. Quick wins keep you motivated."),
    AVALANCHE("Avalanche", "Highest interest first. Saves the most money.");

    val label: String get() = com.budgy.app.i18n.L10n.tr(enLabel)
    val blurb: String get() = com.budgy.app.i18n.L10n.tr(enBlurb)
}

data class PayoffResult(
    val feasible: Boolean,
    val months: Int,
    val totalInterest: Long,
    /** debtId -> month number (1-based) when it's paid off */
    val payoffMonth: Map<Long, Int>,
    val order: List<Long>,
)

/**
 * Month-by-month simulation: interest accrues, every debt gets its minimum, and everything left over
 * (your extra + minimums freed by debts already paid off) goes to the target debt chosen by [strategy].
 */
fun simulatePayoff(debts: List<Debt>, extraPerMonth: Long, strategy: PayoffStrategy): PayoffResult {
    val live = debts.filter { it.balance > 0 }
    if (live.isEmpty()) return PayoffResult(true, 0, 0, emptyMap(), emptyList())
    val bal = live.associate { it.id to it.balance.toDouble() }.toMutableMap()
    val budget = live.sumOf { it.minPayment } + extraPerMonth.coerceAtLeast(0)
    var interest = 0.0
    val paidOff = linkedMapOf<Long, Int>()
    var month = 0
    while (bal.values.any { it > 0.5 } && month < 600) {
        month++
        // 1. Interest
        for (d in live) {
            val b = bal.getValue(d.id)
            if (b <= 0.5) continue
            val i = b * (d.aprBps / 10000.0) / 12.0
            bal[d.id] = b + i
            interest += i
        }
        // 2. Minimums
        var left = budget.toDouble()
        for (d in live) {
            val b = bal.getValue(d.id)
            if (b <= 0.5) continue
            val pay = minOf(b, d.minPayment.toDouble(), left)
            bal[d.id] = b - pay
            left -= pay
        }
        // 3. Everything else to the target debt(s)
        val targets = live.filter { bal.getValue(it.id) > 0.5 }.let { open ->
            when (strategy) {
                PayoffStrategy.SNOWBALL -> open.sortedBy { bal.getValue(it.id) }
                PayoffStrategy.AVALANCHE -> open.sortedWith(compareByDescending<Debt> { it.aprBps }.thenBy { bal.getValue(it.id) })
            }
        }
        for (d in targets) {
            if (left <= 0.0) break
            val b = bal.getValue(d.id)
            val pay = minOf(b, left)
            bal[d.id] = b - pay
            left -= pay
        }
        for (d in live) if (bal.getValue(d.id) <= 0.5 && d.id !in paidOff) paidOff[d.id] = month
        // Stuck: payments don't even cover interest
        if (month == 1 && budget.toDouble() <= live.sumOf { it.balance * (it.aprBps / 10000.0) / 12.0 }) break
    }
    val feasible = bal.values.all { it <= 0.5 }
    return PayoffResult(feasible, month, interest.toLong(), paidOff, paidOff.keys.toList())
}

fun monthLabel(monthsFromNow: Int): String {
    val ym = YearMonth.now().plusMonths(monthsFromNow.toLong())
    return ym.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() } + " " + ym.year
}

// ---------------- Net worth ----------------

data class NetWorthPoint(val month: YearMonth, val accounts: Long, val debts: Long, val investments: Long = 0) {
    val net get() = accounts + investments - debts
}

/**
 * Account totals at the end of each of the last [months] months, rebuilt from your transaction history.
 * Planner debts count from the month you added them (Budgy doesn't know their earlier balances).
 */
fun netWorthHistory(
    accounts: List<Account>, txns: List<Txn>, debts: List<Debt>, home: String, months: Int = 12,
    holdings: List<Holding> = emptyList(), investTxns: List<InvestTxn> = emptyList(), snaps: List<PriceSnap> = emptyList(),
): List<NetWorthPoint> {
    val now = YearMonth.now()
    val byId = accounts.associateBy { it.id }
    val sorted = txns.sortedBy { it.date }
    val out = mutableListOf<NetWorthPoint>()
    for (k in months - 1 downTo 0) {
        val ym = now.minusMonths(k.toLong())
        val end = if (k == 0) Long.MAX_VALUE else ym.atEndOfMonth().toEpochDay()
        // Per-account balances in their own currency, then converted with today's rates.
        val bal = accounts.associate { it.id to it.startingBalance }.toMutableMap()
        for (t in sorted) {
            if (t.date > end) break
            when (t.type) {
                TxType.INCOME, TxType.COLLECT, TxType.ADJUST, TxType.DIVEST -> bal[t.accountId]?.let { bal[t.accountId] = it + t.amount }
                TxType.EXPENSE, TxType.LEND, TxType.INVEST -> bal[t.accountId]?.let { bal[t.accountId] = it - t.amount }
                TxType.TRANSFER -> {
                    bal[t.accountId]?.let { bal[t.accountId] = it - t.amount }
                    t.toAccountId?.let { to -> bal[to]?.let { bal[to] = it + (t.toAmount ?: t.amount) } }
                }
            }
        }
        val total = bal.entries.sumOf { (id, v) -> byId[id]?.toHome(v, home) ?: v }
        val debtTotal = debts.filter {
            YearMonth.from(Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault())) <= ym
        }.sumOf { it.balance }
        val invested = if (holdings.isEmpty()) 0L else investmentsValueOn(
            if (k == 0) Long.MAX_VALUE else end, holdings, investTxns, snaps, isToday = k == 0,
        )
        out += NetWorthPoint(ym, total, debtTotal, invested)
    }
    return out
}

// ---------------- No-spend streaks ----------------

data class StreakInfo(
    val current: Int,
    val best: Int,
    val thisMonth: Int,
    val spentToday: Boolean,
    /** last 7 days, oldest first: true = no spending, false = spent, null = before you started */
    val week: List<Boolean?>,
)

/** A no-spend day is a day with no expenses. Counting starts from your first transaction. */
fun noSpendStreaks(txns: List<Txn>): StreakInfo {
    val today = Dates.today()
    if (txns.isEmpty()) return StreakInfo(0, 0, 0, false, List(7) { null })
    val first = txns.minOf { it.date }
    val spendDays = txns.asSequence().filter { it.type == TxType.EXPENSE }.map { it.date }.toHashSet()
    val spentToday = today in spendDays

    // Current: consecutive no-spend days ending today (or yesterday while today is still clean)
    var current = 0
    var d = today
    while (d >= first && d !in spendDays) { current++; d-- }

    var best = 0
    var run = 0
    for (day in first..today) {
        if (day in spendDays) run = 0 else { run++; if (run > best) best = run }
    }
    val monthStart = YearMonth.now().atDay(1).toEpochDay()
    val thisMonth = (maxOf(monthStart, first)..today).count { it !in spendDays }
    val week = (today - 6..today).map { day -> if (day < first) null else day !in spendDays }
    return StreakInfo(current, best, thisMonth, spentToday, week)
}

fun streakMessage(s: StreakInfo): String = when {
    s.current >= 30 -> tr("A whole month without spending. Legendary.")
    s.current >= 14 -> tr("Two weeks strong. Keep it going!")
    s.current >= 7 -> tr("A full week of no spending. Nice!")
    s.current >= 3 -> tr("3+ days. You're on a roll.")
    s.current >= 1 -> tr("No spending so far. Keep the streak alive.")
    s.spentToday -> tr("You spent today. A fresh streak starts tomorrow.")
    else -> tr("Have a day with no expenses to start a streak.")
}

// ---------------- Envelopes ----------------

data class EnvelopeState(
    /** money left in the envelope right now */
    val available: Long,
    /** spent from this envelope in the current month */
    val spentThisMonth: Long,
    /** carried in from earlier months and moves (can be negative if you overspent before) */
    val carriedIn: Long,
)

/**
 * Envelope mode: every month adds the budget's limit to its envelope. Unspent money stays (rolls over),
 * overspending carries forward as a negative, and you can move money between envelopes.
 */
fun envelopeState(b: Budget, txns: List<Txn>, moves: List<EnvelopeMove>, spec: PeriodSpec = PeriodSpec()): EnvelopeState {
    val now = currentPeriod(spec)
    val startDay = if (b.startDate > 0) b.startDate else now.first
    val firstPeriod = periodFor(startDay, spec)
    val funded = periodsSince(startDay, spec)
    val from = firstPeriod.first
    var spentAll = 0L
    var spentNow = 0L
    for (t in txns) {
        if (t.type != TxType.EXPENSE || t.categoryId != b.categoryId || t.date < from || t.date > now.last) continue
        spentAll += t.amount
        if (t.date in now) spentNow += t.amount
    }
    val movesIn = moves.filter { it.toCategoryId == b.categoryId && it.date >= from }.sumOf { it.amount }
    val movesOut = moves.filter { it.fromCategoryId == b.categoryId && it.date >= from }.sumOf { it.amount }
    val available = b.monthlyLimit * funded + movesIn - movesOut - spentAll
    return EnvelopeState(available, spentNow, available + spentNow - b.monthlyLimit)
}
