@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgy.app.ui.screens

import com.budgy.app.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.data.Category
import com.budgy.app.data.Recurring
import com.budgy.app.data.Subscription
import com.budgy.app.data.TxType
import com.budgy.app.data.Txn
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.EmptyState
import com.budgy.app.ui.components.LetterAvatar
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.components.ScreenTitle
import com.budgy.app.ui.components.SelectField
import com.budgy.app.ui.components.TxnRow
import com.budgy.app.ui.monthTotals
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.ui.theme.Numeric
import com.budgy.app.util.Dates
import com.budgy.app.util.Money
import com.budgy.app.util.advanceDate
import com.budgy.app.util.myAmount
import java.time.YearMonth

private data class Scheduled(val day: Long, val title: String, val amount: Long, val color: Long, val subId: Long?)

@Composable
fun ActivityScreen(vm: BudgyViewModel, go: BudgyNav) {
    val txns by vm.transactions.collectAsStateWithLifecycle()
    val stats by vm.statTxns.collectAsStateWithLifecycle()
    val statById = remember(stats) { stats.associateBy { it.id } }
    val categories by vm.categories.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val subs by vm.subscriptions.collectAsStateWithLifecycle()
    val recurring by vm.recurring.collectAsStateWithLifecycle()
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val acctMap = remember(accounts) { accounts.associateBy { it.id } }

    var monthOffset by rememberSaveable { mutableStateOf(0L) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var calendar by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var accountFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedDay by rememberSaveable { mutableStateOf(Dates.today()) }

    val ym = YearMonth.now().plusMonths(monthOffset)
    val range = Dates.monthRange(ym)
    val searchMode = searching && (query.isNotBlank() || accountFilter != null || categoryFilter != null)

    val shown: List<Txn> = remember(txns, monthOffset, filter, searchMode, query, accountFilter, categoryFilter, catMap, acctMap) {
        if (searchMode) {
            val q = query.trim().lowercase()
            txns.filter { t ->
                (accountFilter == null || t.accountId == accountFilter || t.toAccountId == accountFilter) &&
                    (categoryFilter == null || t.categoryId == categoryFilter) &&
                    (q.isEmpty() || matches(t, q, catMap[t.categoryId]?.name, acctMap[t.accountId]?.name))
            }
        } else {
            txns.filter { it.date in range && (filter == null || it.type.name == filter) }
        }
    }
    val totals = remember(stats, monthOffset) { monthTotals(stats, ym) }
    val grouped = remember(shown) { shown.groupBy { it.date } }
    val scheduled = remember(subs, recurring, monthOffset) { scheduledIn(subs, recurring, catMap, range) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            ScreenTitle(tr("Activity")) {
                IconButton(onClick = go::reports) { Icon(Icons.Rounded.BarChart, tr("Monthly report")) }
                IconButton(onClick = {
                    searching = !searching
                    if (!searching) { query = ""; accountFilter = null; categoryFilter = null }
                }) { Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, if (searching) tr("Close search") else tr("Search")) }
            }
        }

        if (searching) {
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(40) },
                        placeholder = { Text(tr("Search notes, #tags, categories, amounts")) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val acctOptions: List<Pair<Long?, String>> = listOf<Pair<Long?, String>>(null to tr("Any account")) + accounts.map { it.id to it.name }
                        val catOptions: List<Pair<Long?, String>> = listOf<Pair<Long?, String>>(null to tr("Any category")) + categories.map { it.id to it.name }
                        SelectField(
                            label = tr("Account"),
                            value = acctOptions.first { it.first == accountFilter }.second,
                            options = acctOptions, optionLabel = { it.second }, onSelect = { accountFilter = it.first },
                            modifier = Modifier.weight(1f),
                        )
                        SelectField(
                            label = tr("Category"),
                            value = catOptions.firstOrNull { it.first == categoryFilter }?.second ?: tr("Any category"),
                            options = catOptions, optionLabel = { it.second }, onSelect = { categoryFilter = it.first },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (searchMode) {
                item {
                    val spent = shown.filter { it.type == TxType.EXPENSE }.sumOf { statById[it.id]?.amount ?: it.amount }
                    Text(
                        tr("{0} result{1} · {2} spent", shown.size, if (shown.size == 1) "" else "s", Money.format(spent, Budgy.currency)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp),
                    )
                }
            }
        }

        if (!searchMode) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthOffset-- }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, tr("Previous month")) }
                    Text(Dates.month(ym), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    IconButton(onClick = { monthOffset++ }, enabled = monthOffset < 0 || calendar) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, tr("Next month"))
                    }
                }
            }
            item {
                BudgyCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), padding = 16.dp) {
                    Row(Modifier.fillMaxWidth()) {
                        Stat(tr("Income"), Modifier.weight(1f)) { MoneyText(totals.income, color = Budgy.colors.income) }
                        Stat(tr("Spent"), Modifier.weight(1f)) { MoneyText(totals.expense, color = Budgy.colors.expense) }
                        Stat(tr("Net"), Modifier.weight(1f)) { MoneyText(totals.net, showPlus = true) }
                    }
                }
            }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    SegmentedButton(selected = !calendar, onClick = { calendar = false }, shape = SegmentedButtonDefaults.itemShape(0, 2), label = { Text(tr("List")) })
                    SegmentedButton(selected = calendar, onClick = { calendar = true }, shape = SegmentedButtonDefaults.itemShape(1, 2), label = { Text(tr("Calendar")) })
                }
            }
        }

        if (!searchMode && calendar) {
            item {
                MonthCalendar(
                    ym = ym, txns = stats, scheduled = scheduled, selected = selectedDay,
                    onSelect = { selectedDay = it },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            val dayTxns = txns.filter { it.date == selectedDay }
            val daySched = scheduled.filter { it.day == selectedDay }
            item {
                Text(
                    Dates.dayLabel(selectedDay),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            if (dayTxns.isEmpty() && daySched.isEmpty()) {
                item {
                    Text(
                        tr("Nothing on this day."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
            items(daySched) { s ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = s.subId != null) { s.subId?.let(go::editSubscription) }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LetterAvatar(s.title, androidx.compose.ui.graphics.Color(s.color), 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.title, style = MaterialTheme.typography.titleSmall)
                        Text(tr("Scheduled"), style = MaterialTheme.typography.bodySmall, color = Budgy.colors.warning)
                    }
                    MoneyText(s.amount)
                }
            }
            items(dayTxns, key = { it.id }) { t ->
                Column(Modifier.padding(horizontal = 12.dp)) {
                    TxnRow(t, t.categoryId?.let { catMap[it] }, acctMap, onClick = { go.editEntry(t.id) })
                }
            }
        } else {
            if (!searchMode) {
                item {
                    val options = listOf(null to tr("All"), "EXPENSE" to tr("Expenses"), "INCOME" to tr("Income"), "TRANSFER" to tr("Transfers"))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        items(options) { (key, label) ->
                            FilterChip(
                                selected = filter == key,
                                onClick = { filter = key },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }
            }

            if (shown.isEmpty()) {
                item {
                    EmptyState(
                        title = if (searchMode) tr("No matches") else tr("Nothing here yet"),
                        body = when {
                            searchMode -> tr("Try a different word, or clear a filter.")
                            filter == null -> tr("No transactions in {0}.", Dates.month(ym))
                            else -> tr("No matching transactions this month.")
                        },
                        action = if (searchMode) null else tr("Add transaction"),
                        onAction = { go.newEntry(TxType.EXPENSE.name) },
                    )
                }
            }

            grouped.forEach { (day, list) ->
                item(key = "h$day") {
                    val dayNet = list.sumOf {
                        when (it.type) { TxType.INCOME -> it.amount; TxType.EXPENSE -> -it.amount; else -> 0L }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            Dates.dayLabel(day),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        MoneyText(dayNet, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, showPlus = true)
                    }
                }
                items(list, key = { it.id }) { t ->
                    Column(Modifier.padding(horizontal = 12.dp)) {
                        TxnRow(t, t.categoryId?.let { catMap[it] }, acctMap, onClick = { go.editEntry(t.id) })
                    }
                }
            }
        }
    }
}

private fun matches(t: Txn, q: String, category: String?, account: String?): Boolean {
    if (t.note.lowercase().contains(q)) return true
    if (t.tags.lowercase().contains(q.removePrefix("#"))) return true
    if (category?.lowercase()?.contains(q) == true) return true
    if (account?.lowercase()?.contains(q) == true) return true
    val digits = q.replace(",", "")
    if (digits.isNotEmpty() && digits.all { it.isDigit() || it == '.' }) {
        return Money.toInput(t.amount).startsWith(digits)
    }
    return false
}

/** Every bill and recurring entry expected in [range], projected forward from their next dates. */
private fun scheduledIn(subs: List<Subscription>, recurring: List<Recurring>, cats: Map<Long, Category>, range: LongRange): List<Scheduled> {
    val out = mutableListOf<Scheduled>()
    val today = Dates.today()
    for (s in subs.filter { it.active }) {
        if (s.isTrial && s.trialEndDate != null && s.trialEndDate in range) {
            out += Scheduled(s.trialEndDate, tr("{0} trial ends", s.name), s.amount, s.color, s.id)
        }
        var d = s.nextBillingDate
        var guard = 0
        while (d <= range.last && guard < 60) {
            if (d in range && d >= today - 31) out += Scheduled(d, s.name, s.myAmount(), s.color, s.id)
            d = advanceDate(d, s.cycle, s.customDays); guard++
        }
    }
    for (r in recurring.filter { it.active }) {
        var d = r.nextDate
        var guard = 0
        while (d <= range.last && guard < 60) {
            if (d in range) {
                val color = r.categoryId?.let { cats[it]?.color } ?: 0xFF8A948F
                out += Scheduled(d, r.note.ifBlank { r.categoryId?.let { cats[it]?.name } ?: r.type.label }, r.amount, color, null)
            }
            d = advanceDate(d, r.cycle, r.customDays); guard++
        }
    }
    return out
}

@Composable
private fun MonthCalendar(
    ym: YearMonth,
    txns: List<Txn>,
    scheduled: List<Scheduled>,
    selected: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val range = Dates.monthRange(ym)
    val spentByDay = remember(txns, ym) {
        txns.filter { it.type == TxType.EXPENSE && it.date in range }.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.amount } }
    }
    val incomeDays = remember(txns, ym) { txns.filter { it.type == TxType.INCOME && it.date in range }.map { it.date }.toSet() }
    val schedDays = scheduled.map { it.day }.toSet()
    val today = Dates.today()
    val first = ym.atDay(1)
    val lead = first.dayOfWeek.value % 7 // Sunday = 0
    val cells = lead + ym.lengthOfMonth()
    val rows = (cells + 6) / 7
    val cur = Budgy.currency

    BudgyCard(modifier, padding = 10.dp) {
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(
                    it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val idx = r * 7 + c - lead
                    Box(Modifier.weight(1f).aspectRatio(0.82f).padding(2.dp)) {
                        if (idx in 0 until ym.lengthOfMonth()) {
                            val day = first.plusDays(idx.toLong()).toEpochDay()
                            val isSel = day == selected
                            val isToday = day == today
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                                    .then(if (isToday && !isSel) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier)
                                    .clickable { onSelect(day) }
                                    .padding(top = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                )
                                spentByDay[day]?.let {
                                    Text(
                                        Money.compact(it, cur),
                                        fontSize = 9.sp,
                                        style = Numeric,
                                        color = Budgy.colors.expense,
                                        maxLines = 1,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
                                    if (day in incomeDays) Dot(Budgy.colors.income)
                                    if (day in schedDays) Dot(Budgy.colors.gold)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)) {
            Legend(Budgy.colors.income, tr("Income"))
            Legend(Budgy.colors.gold, tr("Bill or recurring due"))
        }
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.size(5.dp).clip(CircleShape).background(color))
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Stat(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        value()
    }
}
