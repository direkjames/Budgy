package com.budgy.app.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Budget periods (V4). Budgets can reset:
 *  - MONTHLY on a chosen day (1st by default, or your payday like the 15th), or
 *  - TWICE a month on the 15th and 30th (end of month in February) — "kinsenas".
 * A budget's limit applies to each period.
 */

enum class PeriodMode { MONTHLY, TWICE }

data class PeriodSpec(val mode: PeriodMode = PeriodMode.MONTHLY, val startDay: Int = 1)

private fun clampDay(ym: YearMonth, day: Int) = ym.atDay(day.coerceIn(1, ym.lengthOfMonth()))

/** Start dates of periods that begin in [ym], in order. */
private fun startsIn(ym: YearMonth, spec: PeriodSpec): List<LocalDate> = when (spec.mode) {
    PeriodMode.MONTHLY -> listOf(clampDay(ym, spec.startDay))
    PeriodMode.TWICE -> listOf(clampDay(ym, 15), clampDay(ym, 30))
}

/** The budget period (inclusive epoch-day range) that contains [day]. */
fun periodFor(day: Long, spec: PeriodSpec): LongRange {
    val d = LocalDate.ofEpochDay(day)
    val ym = YearMonth.from(d)
    val starts = (startsIn(ym.minusMonths(1), spec) + startsIn(ym, spec) + startsIn(ym.plusMonths(1), spec)).distinct().sorted()
    val i = starts.indexOfLast { !it.isAfter(d) }
    val start = starts[i]
    val end = starts[i + 1].minusDays(1)
    return start.toEpochDay()..end.toEpochDay()
}

fun currentPeriod(spec: PeriodSpec): LongRange = periodFor(Dates.today(), spec)

fun previousPeriod(p: LongRange, spec: PeriodSpec): LongRange = periodFor(p.first - 1, spec)

/** How many periods from the one containing [fromDay] up to and including the current one. */
fun periodsSince(fromDay: Long, spec: PeriodSpec): Int {
    var p = periodFor(fromDay, spec)
    val now = currentPeriod(spec)
    var n = 1
    while (p.first < now.first && n < 2000) {
        p = periodFor(p.last + 1, spec)
        n++
    }
    return n
}

fun daysLeftIn(p: LongRange): Int = (p.last - Dates.today() + 1).toInt().coerceAtLeast(1)

fun periodLabel(p: LongRange): String = Dates.short(p.first) + " – " + Dates.short(p.last)

fun isCalendarMonth(spec: PeriodSpec) = spec.mode == PeriodMode.MONTHLY && spec.startDay <= 1
