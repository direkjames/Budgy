@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.budgy.app.util.formatUnits
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.IconBadge
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.components.icon
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.ui.theme.Numeric
import com.budgy.app.util.Money
import com.budgy.app.util.NetWorthPoint
import com.budgy.app.util.netWorthHistory
import com.budgy.app.util.isForeign

@Composable
fun NetWorthScreen(vm: BudgyViewModel, go: BudgyNav) {
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val txns by vm.transactions.collectAsStateWithLifecycle()
    val debts by vm.debts.collectAsStateWithLifecycle()
    val balances by vm.homeBalances.collectAsStateWithLifecycle()
    val raw by vm.balances.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val home = settings?.currency ?: "₱"
    val holdings by vm.holdings.collectAsStateWithLifecycle()
    val investTxns by vm.investTxns.collectAsStateWithLifecycle()
    val snaps by vm.priceSnaps.collectAsStateWithLifecycle()
    val portfolio by vm.portfolio.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refreshPrices() }
    val history = remember(accounts, txns, debts, home, holdings, investTxns, snaps) {
        netWorthHistory(accounts, txns, debts, home, 12, holdings, investTxns, snaps)
    }
    val cur = Budgy.currency

    val assets = accounts.map { it to (balances[it.id] ?: it.startingBalance) }.filter { it.second > 0 }
    val owedAccounts = accounts.map { it to (balances[it.id] ?: it.startingBalance) }.filter { it.second < 0 }
    val openDebts = debts.filter { it.balance > 0 }
    val invested = portfolio.positions.filter { it.value > 0 }
    val totalAssets = assets.sumOf { it.second } + invested.sumOf { it.value }
    val totalLiabilities = -owedAccounts.sumOf { it.second } + openDebts.sumOf { it.balance }
    val net = totalAssets - totalLiabilities
    val first = history.firstOrNull { it.accounts != 0L || it.debts != 0L || it.investments != 0L } ?: history.first()
    val yearChange = net - first.net

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tr("Net worth")) },
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
                BudgyCard {
                    Text(tr("What you own minus what you owe"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(net, style = MaterialTheme.typography.headlineLarge, color = if (net < 0) Budgy.colors.expense else Color.Unspecified)
                    Text(
                        (if (yearChange >= 0) "▲ " else "▼ ") + Money.format(kotlin.math.abs(yearChange), cur) + " " + tr("since {0}", tr(monthName(first))),
                        style = MaterialTheme.typography.bodyMedium.merge(Numeric),
                        color = if (yearChange >= 0) Budgy.colors.income else Budgy.colors.expense,
                    )
                    Spacer(Modifier.height(16.dp))
                    LineChart(history.map { it.net }, Modifier.fillMaxWidth().height(150.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        listOf(0, 3, 6, 9, 11).forEach { i ->
                            Text(
                                history.getOrNull(i)?.let { monthName(it) } ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = if (i == 11) TextAlign.End else TextAlign.Start,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BudgyCard(Modifier.weight(1f), padding = 14.dp) {
                        Text(tr("Assets"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(totalAssets, style = MaterialTheme.typography.titleLarge, color = Budgy.colors.income)
                    }
                    BudgyCard(Modifier.weight(1f), padding = 14.dp) {
                        Text(tr("Liabilities"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(totalLiabilities, style = MaterialTheme.typography.titleLarge, color = Budgy.colors.expense)
                    }
                }
            }

            item {
                BudgyCard {
                    Text(tr("Breakdown"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    assets.sortedByDescending { it.second }.forEach { (a, v) ->
                        BreakdownRow({ IconBadge(a.type.icon(), Color(a.color), 32.dp) }, a.name, v, Budgy.colors.income,
                            if (a.isForeign(home)) Money.format(raw[a.id] ?: a.startingBalance, a.currency) else null)
                    }
                    invested.sortedByDescending { it.value }.forEach { p ->
                        BreakdownRow({ IconBadge(holdingIcon(p.holding), Color(p.holding.color), 32.dp) }, p.holding.name, p.value, Budgy.colors.income,
                            if (p.holding.symbol.isNotBlank()) formatUnits(p.units) + " " + p.holding.symbol else null)
                    }
                    owedAccounts.forEach { (a, v) ->
                        BreakdownRow({ IconBadge(a.type.icon(), Color(a.color), 32.dp) }, a.name, v, Budgy.colors.expense,
                            if (a.isForeign(home)) Money.format(raw[a.id] ?: a.startingBalance, a.currency) else null)
                    }
                    openDebts.forEach { d ->
                        BreakdownRow({ IconBadge(com.budgy.app.ui.components.CategoryIcons.get("bills"), Color(d.color), 32.dp) }, d.name + tr(" (debt)"), -d.balance, Budgy.colors.expense)
                    }
                    Spacer(Modifier.height(6.dp))
                    androidx.compose.foundation.layout.FlowRow {
                        TextButton(onClick = go::accounts) { Text(tr("Accounts")) }
                        TextButton(onClick = go::investments) { Text(tr("Investments")) }
                        TextButton(onClick = go::debts) { Text(tr("Debt planner")) }
                    }
                }
            }

            item {
                BudgyCard {
                    Text(tr("Month by month"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    val recent = history.takeLast(6).reversed()
                    recent.forEachIndexed { i, p ->
                        val prev = history.getOrNull(history.indexOf(p) - 1)
                        if (i > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(monthName(p) + " " + p.month.year, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            MoneyText(p.net, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(10.dp))
                            val diff = prev?.let { p.net - it.net } ?: 0L
                            Text(
                                if (prev == null) "" else (if (diff >= 0) "+" else "−") + Money.compact(kotlin.math.abs(diff), cur),
                                style = MaterialTheme.typography.labelMedium.merge(Numeric),
                                color = if (diff >= 0) Budgy.colors.income else Budgy.colors.expense,
                                modifier = Modifier.width(64.dp),
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tr("Rebuilt from your transactions. Debts from the planner count from the month you added them.") +
                            if (holdings.isNotEmpty()) " " + tr("Investments use the price saved at the time, or what you paid when there isn't one.") else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun monthName(p: NetWorthPoint) = p.month.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun BreakdownRow(icon: @Composable () -> Unit, name: String, value: Long, color: Color, original: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            if (original != null) Text(original, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MoneyText(value, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun LineChart(values: List<Long>, modifier: Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val gold = Budgy.colors.gold
    val zero = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val min = minOf(values.min(), 0L)
        val max = maxOf(values.max(), 0L)
        val span = (max - min).coerceAtLeast(1).toFloat()
        val padY = 8f
        fun y(v: Long) = padY + (size.height - 2 * padY) * (1f - (v - min) / span)
        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
        val pts = values.mapIndexed { i, v -> Offset(i * stepX, y(v)) }

        // zero line when the range crosses zero
        if (min < 0) drawLine(zero, Offset(0f, y(0)), Offset(size.width, y(0)), strokeWidth = 2f)

        val area = Path().apply {
            moveTo(pts.first().x, size.height)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(line.copy(alpha = 0.25f), line.copy(alpha = 0f))))
        val path = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, line, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(gold, radius = 9f, center = pts.last())
        drawCircle(Color.White, radius = 4f, center = pts.last())
    }
}
