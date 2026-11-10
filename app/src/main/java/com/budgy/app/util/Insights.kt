package com.budgy.app.util

import com.budgy.app.i18n.tr
import com.budgy.app.data.Category
import com.budgy.app.data.Debt
import com.budgy.app.data.Goal
import com.budgy.app.data.TxType
import com.budgy.app.data.Txn
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

// ---------------- Spending alerts ----------------

/** "Food is 40% higher than your usual month (at this point in the month)." */
data class Insight(val categoryId: Long?, val name: String, val pct: Int, val current: Long, val usual: Long) {
    val higher get() = pct > 0
}

/**
 * Compares what you've spent in each category so far this month with the average of the same days
 * in the previous 3 months (only months you were already using Budgy).
 */
fun spendingInsights(txns: List<Txn>, categories: List<Category>, today: LocalDate = LocalDate.now()): List<Insight> {
    val expenses = txns.filter { it.type == TxType.EXPENSE }
    if (expenses.isEmpty()) return emptyList()
    val firstDay = expenses.minOf { it.date }
    val day = today.dayOfMonth
    val now = YearMonth.from(today)
    val curRange = now.atDay(1).toEpochDay()..today.toEpochDay()
    val prev = (1..3).map { now.minusMonths(it.toLong()) }
        // Only months you were already using Budgy by the 15th
        .filter { it.atDay(15).toEpochDay() >= firstDay }
    if (prev.isEmpty() || day < 5) return emptyList()
    val names = categories.associate { it.id to it.name }

    val current = expenses.filter { it.date in curRange }.groupBy { it.categoryId }.mapValues { (_, v) -> v.sumOf { it.amount } }
    val usual = mutableMapOf<Long?, Long>()
    for (m in prev) {
        val r = m.atDay(1).toEpochDay()..m.atDay(minOf(day, m.lengthOfMonth())).toEpochDay()
        expenses.filter { it.date in r }.groupBy { it.categoryId }.forEach { (k, v) -> usual[k] = (usual[k] ?: 0) + v.sumOf { it.amount } }
    }
    usual.keys.toList().forEach { k -> usual[k] = usual.getValue(k) / prev.size }

    val out = mutableListOf<Insight>()
    for (cat in (current.keys + usual.keys).toSet()) {
        val cur = current[cat] ?: 0L
        val avg = usual[cat] ?: 0L
        if (avg <= 0) continue
        val diff = cur - avg
        val pct = (diff * 100 / avg).toInt()
        val name = cat?.let { names[it] } ?: tr("Uncategorized")
        when {
            // Noticeably higher: at least 25% and ₱200 more than usual
            pct >= 25 && diff >= 20_000 -> out += Insight(cat, name, pct, cur, avg)
            // Noticeably lower, later in the month, on a category that matters
            pct <= -40 && day >= 10 && avg >= 100_000 -> out += Insight(cat, name, pct, cur, avg)
        }
    }
    return out.sortedByDescending { abs(it.pct) }.take(3)
}

fun insightText(i: Insight, cur: String): String =
    if (i.higher) tr("{0} is {1}% higher than your usual month so far ({2} vs {3}).", tr(i.name), i.pct, Money.format(i.current, cur), Money.format(i.usual, cur))
    else tr("{0} is {1}% lower than usual so far. Nice!", tr(i.name), -i.pct)

// ---------------- Year in review ----------------

data class YearReview(
    val year: Int,
    val income: Long,
    val spent: Long,
    val byMonth: List<Long>,
    val topCategories: List<Pair<Long?, Long>>,
    val biggest: Txn?,
    val noSpendDays: Int,
    val bestStreak: Int,
    val subscriptions: Long,
    val transactions: Int,
    val daysLogged: Int,
    val topTag: Pair<String, Long>?,
) {
    val saved get() = income - spent
    val savingsRate get() = if (income > 0) (saved * 100 / income).toInt() else 0
    val biggestMonth get() = byMonth.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }
    val cheapestMonth get() = byMonth.withIndex().filter { it.value > 0 }.minByOrNull { it.value }
}

fun yearReview(txns: List<Txn>, year: Int): YearReview {
    val start = LocalDate.of(year, 1, 1).toEpochDay()
    val end = minOf(LocalDate.of(year, 12, 31).toEpochDay(), Dates.today())
    val inYear = txns.filter { it.date in start..end }
    val exp = inYear.filter { it.type == TxType.EXPENSE }
    val byMonth = LongArray(12)
    exp.forEach { byMonth[LocalDate.ofEpochDay(it.date).monthValue - 1] += it.amount }
    val spendDays = exp.map { it.date }.toHashSet()
    val firstUse = txns.minOfOrNull { it.date } ?: start
    val countFrom = maxOf(start, firstUse)
    var best = 0
    var run = 0
    var noSpend = 0
    if (countFrom <= end) for (d in countFrom..end) {
        if (d in spendDays) run = 0 else { run++; noSpend++; if (run > best) best = run }
    }
    val tagTotals = mutableMapOf<String, Long>()
    exp.forEach { t -> t.tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { tagTotals[it] = (tagTotals[it] ?: 0) + t.amount } }
    return YearReview(
        year = year,
        income = inYear.filter { it.type == TxType.INCOME }.sumOf { it.amount },
        spent = exp.sumOf { it.amount },
        byMonth = byMonth.toList(),
        topCategories = exp.groupBy { it.categoryId }.mapValues { (_, v) -> v.sumOf { it.amount } }.entries
            .sortedByDescending { it.value }.take(5).map { it.key to it.value },
        biggest = exp.maxByOrNull { it.amount },
        noSpendDays = noSpend,
        bestStreak = best,
        subscriptions = exp.filter { it.subscriptionId != null }.sumOf { it.amount },
        transactions = inYear.size,
        daysLogged = inYear.map { it.date }.toSet().size,
        topTag = tagTotals.maxByOrNull { it.value }?.toPair(),
    )
}

// ---------------- Badges ----------------

enum class BadgeGroup(val label: String) { STREAKS("No-spend streaks"), SAVING("Saving"), DEBT("Debt"), HABITS("Habits") }

data class Badge(
    val id: String,
    val group: BadgeGroup,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    /** 0..1 progress toward unlocking */
    val progress: Float,
    val icon: String,
)

fun computeBadges(
    txns: List<Txn>,
    goals: List<Goal>,
    goalSaved: Map<Long, Long>,
    debts: List<Debt>,
    budgetsUnderLastPeriod: Boolean?,
): List<Badge> {
    val best = noSpendStreaks(txns).best
    val reached = goals.count { (goalSaved[it.id] ?: 0) >= it.target && it.target > 0 }
    val paidOff = debts.count { it.balance <= 0 }
    val days = txns.map { it.date }.toSet().size
    fun p(v: Int, of: Int) = (v.toFloat() / of).coerceIn(0f, 1f)
    val list = mutableListOf<Badge>()
    listOf(
        Triple(3, "Warm-up", "3 no-spend days in a row"),
        Triple(7, "Week of zero", "7 no-spend days in a row"),
        Triple(14, "Fortnight saver", "14 no-spend days in a row"),
        Triple(30, "Iron wallet", "30 no-spend days in a row"),
    ).forEach { (n, t, d) -> list += Badge("streak$n", BadgeGroup.STREAKS, t, d, best >= n, p(best, n), "fire") }
    list += Badge("goal1", BadgeGroup.SAVING, "Goal getter", "Reach a savings goal", reached >= 1, p(reached, 1), "savings")
    list += Badge("goal3", BadgeGroup.SAVING, "Dream chaser", "Reach 3 savings goals", reached >= 3, p(reached, 3), "savings")
    list += Badge("budget", BadgeGroup.SAVING, "On budget", "Finish a budget period with every budget under its limit",
        budgetsUnderLastPeriod == true, if (budgetsUnderLastPeriod == true) 1f else 0f, "target")
    list += Badge("debt1", BadgeGroup.DEBT, "Debt slayer", "Pay off a debt in the planner", paidOff >= 1, p(paidOff, 1), "card")
    list += Badge("debtfree", BadgeGroup.DEBT, "Debt-free", "Pay off every debt in the planner",
        debts.isNotEmpty() && paidOff == debts.size, if (debts.isEmpty()) 0f else p(paidOff, debts.size), "trophy")
    list += Badge("first", BadgeGroup.HABITS, "First step", "Log your first transaction", txns.isNotEmpty(), if (txns.isEmpty()) 0f else 1f, "flag")
    list += Badge("days30", BadgeGroup.HABITS, "Consistent", "Log transactions on 30 different days", days >= 30, p(days, 30), "calendar")
    list += Badge("tx100", BadgeGroup.HABITS, "Century", "Log 100 transactions", txns.size >= 100, p(txns.size, 100), "receipt")
    list += Badge("tx500", BadgeGroup.HABITS, "Bookkeeper", "Log 500 transactions", txns.size >= 500, p(txns.size, 500), "receipt")
    return list
}
