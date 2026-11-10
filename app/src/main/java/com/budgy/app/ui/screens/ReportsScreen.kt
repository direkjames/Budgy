@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgy.app.ui.screens

import com.budgy.app.i18n.tr
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.data.TxType
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgetBar
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.CategoryIcons
import com.budgy.app.ui.components.IconBadge
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.monthTotals
import com.budgy.app.ui.spendingByCategory
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.ui.theme.Numeric
import com.budgy.app.util.Dates
import com.budgy.app.util.Money
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

@Composable
fun ReportsScreen(vm: BudgyViewModel, go: BudgyNav) {
    val txns by vm.statTxns.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val catMap = remember(categories) { categories.associateBy { it.id } }
    var offset by rememberSaveable { mutableStateOf(0L) }
    val ym = YearMonth.now().plusMonths(offset)
    val prev = ym.minusMonths(1)
    val cur = Budgy.currency

    val totals = remember(txns, offset) { monthTotals(txns, ym) }
    val prevTotals = remember(txns, offset) { monthTotals(txns, prev) }
    val byCat = remember(txns, offset) { spendingByCategory(txns, ym) }
    val prevByCat = remember(txns, offset) { spendingByCategory(txns, prev) }
    val range = Dates.monthRange(ym)
    val expenses = remember(txns, offset) { txns.filter { it.type == TxType.EXPENSE && it.date in range } }
    val daily = remember(expenses) {
        val arr = LongArray(ym.lengthOfMonth())
        expenses.forEach { arr[(it.date - range.first).toInt()] += it.amount }
        arr
    }
    val isCurrent = offset == 0L
    val daysCounted = if (isCurrent) LocalDate.now().dayOfMonth else ym.lengthOfMonth()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tr("Monthly report")) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { offset-- }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, tr("Previous month")) }
                    Text(Dates.month(ym), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    IconButton(onClick = { offset++ }, enabled = offset < 0) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, tr("Next month")) }
                }
            }

            item {
                androidx.compose.material3.OutlinedButton(onClick = go::year, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("See your year in review"))
                }
            }

            // Headline
            item {
                BudgyCard {
                    Text(tr("You spent"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(totals.expense, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        changeText(totals.expense, prevTotals.expense, prev, cur),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            prevTotals.expense == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
                            totals.expense > prevTotals.expense -> Budgy.colors.expense
                            else -> Budgy.colors.income
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Fact(tr("Income"), Modifier.weight(1f)) { MoneyText(totals.income, color = Budgy.colors.income) }
                        Fact(tr("Net"), Modifier.weight(1f)) { MoneyText(totals.net, showPlus = true) }
                        Fact(tr("Saved"), Modifier.weight(1f)) {
                            Text(
                                if (totals.income > 0) "${(totals.net * 100 / totals.income)}%" else "—",
                                style = MaterialTheme.typography.bodyLarge.merge(Numeric),
                            )
                        }
                    }
                }
            }

            // Daily bars
            item {
                BudgyCard {
                    Text(tr("Spending by day"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        tr("Average {0} a day", Money.format(if (daysCounted > 0) totals.expense / daysCounted else 0, cur)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    DailyBars(daily, todayIndex = if (isCurrent) LocalDate.now().dayOfMonth - 1 else -1, avg = if (daysCounted > 0) totals.expense / daysCounted else 0)
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        listOf(1, 8, 15, 22, 29).forEach {
                            Text(
                                if (it <= ym.lengthOfMonth()) "$it" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // Categories with month-over-month change
            item {
                BudgyCard {
                    Text(tr("Top categories"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    if (byCat.isEmpty()) {
                        Text(tr("No spending this month."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    byCat.entries.sortedByDescending { it.value }.take(8).forEachIndexed { i, (id, v) ->
                        val c = id?.let { catMap[it] }
                        val color = c?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline
                        val before = prevByCat[id] ?: 0L
                        if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(CategoryIcons.get(c?.icon), color, 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c?.name ?: tr("Uncategorized"), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                BudgetBar(if (totals.expense > 0) v.toFloat() / totals.expense else 0f, color, height = 6.dp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                MoneyText(v, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    when {
                                        before == 0L -> tr("new")
                                        else -> {
                                            val pct = (v - before) * 100 / before
                                            (if (pct >= 0) "▲ " else "▼ ") + "${abs(pct)}%"
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall.merge(Numeric),
                                    color = when {
                                        before == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
                                        v > before -> Budgy.colors.expense
                                        else -> Budgy.colors.income
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Quick facts
            item {
                val biggest = expenses.maxByOrNull { it.amount }
                val subsSpent = expenses.filter { it.subscriptionId != null }.sumOf { it.amount }
                val busiest = daily.withIndex().maxByOrNull { it.value }
                BudgyCard {
                    Text(tr("Quick facts"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    FactRow(tr("Biggest expense"),
                        biggest?.let { "${Money.format(it.amount, cur)} · ${it.note.ifBlank { it.categoryId?.let { id -> catMap[id]?.name } ?: "Uncategorized" }} · ${Dates.short(it.date)}" } ?: "—")
                    FactRow(tr("Busiest day"),
                        busiest?.takeIf { it.value > 0 }?.let { "${Dates.short(range.first + it.index)} · ${Money.format(it.value, cur)}" } ?: "—")
                    FactRow(tr("Subscriptions paid"), Money.format(subsSpent, cur))
                    FactRow(tr("Transactions"), "${txns.count { it.date in range }}")
                    FactRow(tr("Last month spent"), Money.format(prevTotals.expense, cur))
                }
            }
        }
    }
}

private fun changeText(now: Long, before: Long, prev: YearMonth, cur: String): String {
    val name = prev.month.name.lowercase().replaceFirstChar { it.uppercase() }
    if (before == 0L) return tr("Nothing spent in {0} to compare", name)
    val diff = now - before
    val pct = abs(diff) * 100 / before
    return if (diff >= 0) tr("{0} more than {1} (▲ {2}%)", Money.format(diff, cur), name, pct)
    else tr("{0} less than {1} (▼ {2}%)", Money.format(-diff, cur), name, pct)
}

@Composable
private fun Fact(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        value()
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.45f))
        Text(value, style = MaterialTheme.typography.bodyMedium.merge(Numeric), textAlign = TextAlign.End, modifier = Modifier.weight(0.55f))
    }
}

@Composable
private fun DailyBars(values: LongArray, todayIndex: Int, avg: Long) {
    val bar = MaterialTheme.colorScheme.primary
    val dim = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val gold = Budgy.colors.gold
    val track = MaterialTheme.colorScheme.surfaceVariant
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val n = values.size
        val slot = size.width / n
        val w = (slot * 0.62f).coerceAtLeast(2f)
        values.forEachIndexed { i, v ->
            val x = i * slot + (slot - w) / 2
            drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(w / 2, w / 2))
            if (v > 0) {
                val h = (size.height * v / max).coerceAtLeast(w)
                val color = when {
                    i == todayIndex -> gold
                    todayIndex in 0 until i -> dim
                    else -> bar
                }
                drawRoundRect(color, Offset(x, size.height - h), Size(w, h), CornerRadius(w / 2, w / 2))
            }
        }
        if (avg > 0) {
            val y = size.height - size.height * avg / max
            drawLine(
                gold, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
        }
    }
}
