package com.budgy.app.ui.screens

import com.budgy.app.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgetBar
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.BudgyLogo
import com.budgy.app.ui.components.DonutChart
import com.budgy.app.ui.components.IconBadge
import com.budgy.app.ui.components.LetterAvatar
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.components.SectionHeader
import com.budgy.app.ui.components.TxnRow
import com.budgy.app.ui.components.budgetColor
import com.budgy.app.ui.components.icon
import com.budgy.app.ui.monthTotals
import com.budgy.app.ui.spendingByCategory
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.ui.theme.Numeric
import com.budgy.app.util.Dates
import com.budgy.app.util.Money
import com.budgy.app.util.keyDate
import com.budgy.app.util.myAmount
import com.budgy.app.util.insightText
import androidx.compose.material.icons.rounded.Insights
import com.budgy.app.util.symbol
import com.budgy.app.util.noSpendStreaks
import com.budgy.app.util.streakMessage
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import com.budgy.app.data.IouDirection
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.OutlinedButton
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@Composable
fun HomeScreen(vm: BudgyViewModel, go: BudgyNav) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val txns by vm.transactions.collectAsStateWithLifecycle()
    val stats by vm.statTxns.collectAsStateWithLifecycle()
    val homeBalances by vm.homeBalances.collectAsStateWithLifecycle()
    val activeAccounts by vm.activeAccounts.collectAsStateWithLifecycle()
    val budgets by vm.budgets.collectAsStateWithLifecycle()
    val subs by vm.subscriptions.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    val dueRecurring by vm.dueRecurring.collectAsStateWithLifecycle()
    val goals by vm.goals.collectAsStateWithLifecycle()
    val goalSaved by vm.goalSaved.collectAsStateWithLifecycle()
    val ious by vm.ious.collectAsStateWithLifecycle()
    val debts by vm.debts.collectAsStateWithLifecycle()
    val streak = remember(txns) { noSpendStreaks(txns) }
    val insights by vm.insights.collectAsStateWithLifecycle()
    val badges by vm.badges.collectAsStateWithLifecycle()

    val ym = remember { YearMonth.now() }
    val totals = remember(stats) { monthTotals(stats, ym) }
    val byCategory = remember(stats) { spendingByCategory(stats, ym) }
    val spec = settings?.period ?: com.budgy.app.util.PeriodSpec()
    val period = com.budgy.app.util.currentPeriod(spec)
    val byCategoryPeriod = remember(stats, period) { com.budgy.app.ui.spendingInRange(stats, period) }
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val acctMap = remember(accounts) { accounts.associateBy { it.id } }
    val today = Dates.today()
    val upcoming = remember(subs) {
        subs.filter { it.active && it.keyDate() - today <= 7 }.sortedBy { it.keyDate() }
    }
    val dark = settings?.darkMode == true
    val portfolio by vm.portfolio.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refreshPrices() }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                BudgyLogo(38.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(greeting(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(tr("Budgy"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { vm.setDarkMode(!dark) }) {
                    Icon(if (dark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, tr("Toggle dark mode"))
                }
                IconButton(onClick = go::reports) { Icon(Icons.Rounded.BarChart, tr("Monthly report")) }
                IconButton(onClick = go::settings) { Icon(Icons.Rounded.Settings, tr("Settings")) }
            }
        }

        // First-run nudge toward the in-app guide (hidden once opened or dismissed)
        if (settings?.helpSeen == false) {
            item {
                BudgyCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.AutoMirrored.Rounded.MenuBook, MaterialTheme.colorScheme.primary, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("New to Budgy?"), style = MaterialTheme.typography.titleSmall)
                            Text(
                                tr("See how everything works in a few minutes."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { vm.setHelpSeen() }) { Text(tr("Not now")) }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(onClick = go::help) { Text(tr("Open guide")) }
                    }
                }
            }
        }

        // Balance hero
        item {
            val c = Budgy.colors
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(c.heroStart, c.heroEnd)))
                    .clickable(onClick = go::networth)
                    .padding(22.dp)
            ) {
                Text(tr("Total balance"), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                MoneyText(
                    homeBalances.values.sum(),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroStat(tr("Income"), totals.income, Icons.Rounded.ArrowDownward, Modifier.weight(1f))
                    HeroStat(tr("Spent"), totals.expense, Icons.Rounded.ArrowUpward, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    tr("{0} · tap for net worth", Dates.month(ym)),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // Accounts strip
        if (activeAccounts.isNotEmpty()) {
            item {
                SectionHeader(tr("Accounts"), tr("Manage"), go::accounts)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(activeAccounts, key = { it.id }) { a ->
                        BudgyCard(Modifier.width(150.dp), onClick = go::accounts, padding = 14.dp) {
                            IconBadge(a.type.icon(), Color(a.color), 34.dp)
                            Spacer(Modifier.height(10.dp))
                            Text(a.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            MoneyText(balances[a.id] ?: a.startingBalance, style = MaterialTheme.typography.bodyMedium, symbol = a.symbol(Budgy.currency))
                        }
                    }
                }
            }
        }

        // Investments (V5)
        if (portfolio.positions.isNotEmpty()) {
            item {
                BudgyCard(onClick = go::investments, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.AutoMirrored.Rounded.ShowChart, MaterialTheme.colorScheme.tertiary, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("Investments"), style = MaterialTheme.typography.titleSmall)
                            Text(
                                tr("{0} holding{1}", portfolio.positions.size, if (portfolio.positions.size == 1) "" else "s"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            MoneyText(portfolio.value, style = MaterialTheme.typography.titleSmall)
                            val g = portfolio.gain
                            Text(
                                Money.format(g, Budgy.currency, showPlus = true) + (portfolio.gainPct?.let { " · " + com.budgy.app.util.formatPct(it) } ?: ""),
                                style = MaterialTheme.typography.labelMedium.merge(Numeric),
                                color = when { g > 0 -> Budgy.colors.income; g < 0 -> Budgy.colors.expense; else -> MaterialTheme.colorScheme.onSurfaceVariant },
                            )
                        }
                    }
                }
            }
        }

        // Recurring entries waiting for a tap
        if (dueRecurring.isNotEmpty()) {
            item {
                BudgyCard {
                    SectionHeader(tr("To confirm"), tr("Manage"), go::recurring)
                    dueRecurring.forEachIndexed { i, r ->
                        if (i > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        val cat = r.categoryId?.let { catMap[it] }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.note.ifBlank { cat?.name ?: r.type.label }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(Dates.dayLabel(r.nextDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            MoneyText(r.amount, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { vm.confirmRecurring(r) }, modifier = Modifier.weight(1f)) { Text(tr("Log it")) }
                            OutlinedButton(onClick = { vm.skipRecurring(r) }, modifier = Modifier.weight(1f)) { Text(tr("Skip this time")) }
                        }
                    }
                }
            }
        }

        // Goals + IOUs shortcuts
        item {
            val cur = Budgy.currency
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val active = goals.filter { !it.archived }
                val target = active.sumOf { it.target }
                val have = active.sumOf { goalSaved[it.id] ?: 0L }
                BudgyCard(Modifier.weight(1f), onClick = go::goals, padding = 14.dp) {
                    IconBadge(Icons.Rounded.Savings, Budgy.colors.gold, 34.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(tr("Goals"), style = MaterialTheme.typography.labelLarge)
                    if (active.isEmpty()) {
                        Text(tr("Start saving"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(
                            tr("{0}% of {1}", if (target > 0) have * 100 / target else 0, Money.compact(target, cur)),
                            style = MaterialTheme.typography.bodySmall.merge(Numeric),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        BudgetBar(if (target > 0) have.toFloat() / target else 0f, Budgy.colors.gold, height = 6.dp)
                    }
                }
                val open = ious.filter { !it.settled }
                val owed = open.filter { it.direction == IouDirection.THEY_OWE }.sumOf { it.amount }
                val owe = open.filter { it.direction == IouDirection.I_OWE }.sumOf { it.amount }
                BudgyCard(Modifier.weight(1f), onClick = go::ious, padding = 14.dp) {
                    IconBadge(Icons.Rounded.Group, MaterialTheme.colorScheme.tertiary, 34.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(tr("IOUs"), style = MaterialTheme.typography.labelLarge)
                    when {
                        open.isEmpty() -> Text(tr("All settled"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> {
                            if (owed > 0) Text(tr("Owed {0}", Money.compact(owed, cur)), style = MaterialTheme.typography.bodySmall, color = Budgy.colors.income)
                            if (owe > 0) Text(tr("You owe {0}", Money.compact(owe, cur)), style = MaterialTheme.typography.bodySmall, color = Budgy.colors.expense)
                        }
                    }
                }
            }
        }

        // Debts + Net worth shortcuts
        item {
            val cur = Budgy.currency
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val owed = debts.sumOf { it.balance.coerceAtLeast(0) }
                BudgyCard(Modifier.weight(1f), onClick = go::debts, padding = 14.dp) {
                    IconBadge(Icons.Rounded.CreditCard, Budgy.colors.expense, 34.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(tr("Debts"), style = MaterialTheme.typography.labelLarge)
                    Text(
                        if (owed > 0) tr("{0} to pay off", Money.compact(owed, cur)) else tr("Plan a payoff"),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (owed > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    )
                }
                BudgyCard(Modifier.weight(1f), onClick = go::networth, padding = 14.dp) {
                    IconBadge(Icons.AutoMirrored.Rounded.TrendingUp, Budgy.colors.income, 34.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(tr("Net worth"), style = MaterialTheme.typography.labelLarge)
                    Text(
                        Money.compact(homeBalances.values.sum() - owed, cur),
                        style = MaterialTheme.typography.bodySmall.merge(Numeric),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // No-spend streak
        if (txns.isNotEmpty()) {
            item {
                BudgyCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            Icons.Rounded.LocalFireDepartment,
                            if (streak.current > 0) Budgy.colors.warning else MaterialTheme.colorScheme.outline,
                            40.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (streak.current == 1) tr("1 no-spend day") else tr("{0} no-spend days", streak.current),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(streakMessage(streak), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val labels = (6 downTo 0).map { java.time.LocalDate.now().minusDays(it.toLong()).dayOfWeek.name.take(1) }
                        streak.week.forEachIndexed { i, ok ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (ok) {
                                                true -> Budgy.colors.income
                                                false -> Budgy.colors.expense.copy(alpha = 0.2f)
                                                null -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (ok == true) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(labels[i], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        tr("Best streak {0} day{1} · {2} no-spend day{3} this month", streak.best, if (streak.best == 1) "" else "s", streak.thisMonth, if (streak.thisMonth == 1) "" else "s"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (badges.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        SectionHeader(tr("Badges {0}/{1}", badges.count { it.unlocked }, badges.size), tr("See all"), go::badges)
                    }
                }
            }
        }

        // Spending alerts
        if (insights.isNotEmpty()) {
            item {
                BudgyCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Insights, Budgy.colors.warning, 34.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(tr("Heads up"), style = MaterialTheme.typography.titleMedium)
                    }
                    insights.forEach { i ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            insightText(i, Budgy.currency),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (i.higher) MaterialTheme.colorScheme.onSurface else Budgy.colors.income,
                        )
                    }
                }
            }
        }

        // Budget overview
        item {
            val totalLimit = budgets.sumOf { it.monthlyLimit }
            BudgyCard(onClick = { go.tab("budgets") }) {
                if (budgets.isEmpty()) {
                    Text(tr("Set a budget"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr("Give your categories a limit and Budgy will tell you how much you can spend each day."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = { go.tab("budgets") }) { Text(tr("Create a budget")) }
                } else {
                    val spent = budgets.sumOf { byCategoryPeriod[it.categoryId] ?: 0L }
                    val frac = if (totalLimit > 0) spent.toFloat() / totalLimit else 0f
                    val daysLeft = com.budgy.app.util.daysLeftIn(period)
                    val left = totalLimit - spent
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (com.budgy.app.util.isCalendarMonth(spec)) tr("Monthly budget") else tr("Budget · {0}", com.budgy.app.util.periodLabel(period)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("${(frac * 100).toInt()}%", style = MaterialTheme.typography.labelLarge.merge(Numeric), color = budgetColor(frac))
                    }
                    Spacer(Modifier.height(10.dp))
                    BudgetBar(frac, budgetColor(frac), height = 10.dp)
                    Spacer(Modifier.height(10.dp))
                    val cur = Budgy.currency
                    Text(
                        if (left >= 0) tr("{0} left · {1}/day for {2} day{3}", Money.format(left, cur), Money.format(left / daysLeft, cur), daysLeft, if (daysLeft == 1) "" else "s")
                        else tr("Over budget by {0}", Money.format(-left, cur)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (left >= 0) MaterialTheme.colorScheme.onSurfaceVariant else Budgy.colors.expense,
                    )
                }
            }
        }

        // Spending breakdown
        item {
            BudgyCard {
                Text(tr("Where it went"), style = MaterialTheme.typography.titleMedium)
                Text(Dates.month(ym), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                if (byCategory.isEmpty()) {
                    Text(
                        tr("No spending yet this month. Tap Add to log your first expense."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val sorted = byCategory.entries.sortedByDescending { it.value }
                    val top = sorted.take(5)
                    val rest = sorted.drop(5).sumOf { it.value }
                    val grey = MaterialTheme.colorScheme.outline
                    val slices = top.map { (id, v) -> (catMap[id]?.let { Color(it.color) } ?: grey) to v } +
                        (if (rest > 0) listOf(grey to rest) else emptyList())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(slices, Modifier.size(132.dp), thickness = 16.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(tr("Spent"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    Money.compact(totals.expense, Budgy.currency),
                                    style = MaterialTheme.typography.titleMedium.merge(Numeric),
                                )
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            top.forEach { (id, v) ->
                                LegendRow(catMap[id]?.name ?: tr("Uncategorized"), catMap[id]?.let { Color(it.color) } ?: grey, v, totals.expense)
                            }
                            if (rest > 0) LegendRow(tr("Others"), grey, rest, totals.expense)
                        }
                    }
                }
            }
        }

        // Upcoming bills
        item {
            BudgyCard {
                SectionHeader(tr("Next 7 days"), tr("All subscriptions")) { go.tab("subs") }
                if (upcoming.isEmpty()) {
                    Text(
                        tr("No bills or trial endings coming up. Nice."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    upcoming.forEachIndexed { i, s ->
                        if (i > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { go.editSubscription(s.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LetterAvatar(s.name, Color(s.color), 38.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val label = (if (s.isTrial) tr("Trial ends ") else "") + Dates.dueLabel(s.keyDate()).let { if (s.isTrial) it.lowercase() else it }
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (s.keyDate() < today) Budgy.colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            MoneyText(s.myAmount(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Recent
        item {
            BudgyCard {
                SectionHeader(tr("Recent"), if (txns.isNotEmpty()) tr("See all") else null) { go.tab("activity") }
                if (txns.isEmpty()) {
                    Text(
                        tr("Your transactions will show up here."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(onClick = { go.newEntry() }) {
                        Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Add a transaction"))
                    }
                } else {
                    txns.take(5).forEach { t ->
                        TxnRow(t, t.categoryId?.let { catMap[it] }, acctMap, onClick = { go.editEntry(t.id) }, showDate = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, cents: Long, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Budgy.colors.gold, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            MoneyText(cents, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LegendRow(name: String, color: Color, value: Long, total: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(
            "${if (total > 0) (value * 100 / total) else 0}%",
            style = MaterialTheme.typography.labelMedium.merge(Numeric),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun greeting(): String {
    val h = LocalTime.now().hour
    return when {
        h < 12 -> tr("Good morning")
        h < 18 -> tr("Good afternoon")
        else -> tr("Good evening")
    }
}
