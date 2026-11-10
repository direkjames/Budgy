package com.budgy.app.util

import com.budgy.app.i18n.tr
import com.budgy.app.data.BillingCycle
import com.budgy.app.data.Subscription
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object Dates {
    // Month and weekday names follow the in-app language (Filipino uses "Oktubre", "Lun", …).
    private fun locale(): Locale = when (com.budgy.app.i18n.L10n.lang) {
        "fil" -> Locale.forLanguageTag("fil")
        "ceb" -> Locale.forLanguageTag("ceb")
        else -> Locale.US
    }
    private val cache = java.util.concurrent.ConcurrentHashMap<String, DateTimeFormatter>()
    private fun fmt(pattern: String): DateTimeFormatter {
        val loc = locale()
        return cache.getOrPut(pattern + loc.toLanguageTag()) { DateTimeFormatter.ofPattern(pattern, loc) }
    }
    private val dayFmt get() = fmt("EEE, MMM d")
    private val dayYearFmt get() = fmt("EEE, MMM d, yyyy")
    private val shortFmt get() = fmt("MMM d")
    private val monthFmt get() = fmt("MMMM yyyy")

    fun today(): Long = LocalDate.now().toEpochDay()

    fun dayLabel(epochDay: Long): String {
        val t = today()
        return when (epochDay) {
            t -> tr("Today")
            t - 1 -> tr("Yesterday")
            t + 1 -> tr("Tomorrow")
            else -> {
                val d = LocalDate.ofEpochDay(epochDay)
                if (d.year == LocalDate.now().year) d.format(dayFmt) else d.format(dayYearFmt)
            }
        }
    }

    fun short(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(shortFmt)

    fun month(ym: YearMonth): String = ym.atDay(1).format(monthFmt)

    /** "Today", "Tomorrow", "In 5 days", "2 days overdue" */
    fun dueLabel(due: Long): String {
        val d = due - today()
        return when {
            d < -1 -> tr("{0} days overdue", -d)
            d == -1L -> tr("1 day overdue")
            d == 0L -> tr("Today")
            d == 1L -> tr("Tomorrow")
            else -> tr("In {0} days", d)
        }
    }

    // The Material date picker works in UTC milliseconds.
    fun toPickerMillis(epochDay: Long): Long = epochDay * 86_400_000L
    fun fromPickerMillis(millis: Long): Long = Math.floorDiv(millis, 86_400_000L)

    fun monthRange(ym: YearMonth): LongRange = ym.atDay(1).toEpochDay()..ym.atEndOfMonth().toEpochDay()
}

fun advanceDate(date: Long, cycle: BillingCycle, customDays: Int): Long {
    val d = LocalDate.ofEpochDay(date)
    return when (cycle) {
        BillingCycle.WEEKLY -> d.plusWeeks(1)
        BillingCycle.MONTHLY -> d.plusMonths(1)
        BillingCycle.YEARLY -> d.plusYears(1)
        BillingCycle.CUSTOM -> d.plusDays(customDays.coerceAtLeast(1).toLong())
    }.toEpochDay()
}

fun Subscription.advance(): Long = advanceDate(nextBillingDate, cycle, customDays)

/** Your part of the bill when a plan is shared (rounded down to the cent). */
fun Subscription.myAmount(): Long = amount / sharedWith.coerceAtLeast(1)

/** Monthly-equivalent cost of YOUR share, so a yearly plan counts as 1/12 per month. */
fun Subscription.monthlyCost(): Long = monthlyEquivalent(myAmount(), cycle, customDays)

fun monthlyEquivalent(amount: Long, cycle: BillingCycle, customDays: Int): Long = when (cycle) {
    BillingCycle.WEEKLY -> amount * 52 / 12
    BillingCycle.MONTHLY -> amount
    BillingCycle.YEARLY -> amount / 12
    BillingCycle.CUSTOM -> (amount * 365.25 / 12 / customDays.coerceAtLeast(1)).toLong()
}

fun cycleText(cycle: BillingCycle, customDays: Int): String = when (cycle) {
    BillingCycle.CUSTOM -> tr("Every {0} days", customDays)
    else -> cycle.label
}

fun Subscription.cycleLabel(): String = when (cycle) {
    BillingCycle.CUSTOM -> tr("Every {0} days", customDays)
    else -> cycle.label
}

fun Subscription.cycleSuffix(): String = when (cycle) {
    BillingCycle.WEEKLY -> "/wk"
    BillingCycle.MONTHLY -> "/mo"
    BillingCycle.YEARLY -> "/yr"
    BillingCycle.CUSTOM -> "/${customDays}d"
}

/** The date that matters next: trial end for trials, otherwise the next charge. */
fun Subscription.keyDate(): Long = if (isTrial && trialEndDate != null) trialEndDate else nextBillingDate
