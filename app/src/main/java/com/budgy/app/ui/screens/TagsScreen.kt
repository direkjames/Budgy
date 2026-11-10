@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgy.app.ui.screens

import com.budgy.app.i18n.tr
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.data.TxType
import com.budgy.app.data.Txn
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgetBar
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.EmptyState
import com.budgy.app.ui.components.IconBadge
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.components.SectionHeader
import com.budgy.app.ui.components.TxnRow
import com.budgy.app.ui.components.tagList
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.util.Dates

private data class TagSummary(val name: String, val spent: Long, val income: Long, val count: Int, val first: Long, val last: Long)

@Composable
fun TagsScreen(vm: BudgyViewModel, go: BudgyNav) {
    val txns by vm.transactions.collectAsStateWithLifecycle()
    val stats by vm.statTxns.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val acctMap = remember(accounts) { accounts.associateBy { it.id } }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    val summaries = remember(stats) {
        val map = linkedMapOf<String, MutableList<Txn>>()
        for (t in stats) for (tag in tagList(t.tags)) {
            val key = map.keys.firstOrNull { it.equals(tag, true) } ?: tag
            map.getOrPut(key) { mutableListOf() } += t
        }
        map.map { (name, list) ->
            TagSummary(
                name,
                list.filter { it.type == TxType.EXPENSE }.sumOf { it.amount },
                list.filter { it.type == TxType.INCOME }.sumOf { it.amount },
                list.size, list.minOf { it.date }, list.maxOf { it.date },
            )
        }.sortedByDescending { it.last }
    }

    BackHandler(enabled = selected != null) { selected = null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(selected?.let { "#$it" } ?: tr("Tags")) },
                navigationIcon = {
                    IconButton(onClick = { if (selected != null) selected = null else go.back() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val sel = selected
            if (sel == null) {
                if (summaries.isEmpty()) {
                    item {
                        BudgyCard {
                            EmptyState(
                                title = tr("No tags yet"),
                                body = tr("Add a tag like \"Baguio trip\" or \"Wedding\" when you log a transaction. Budgy totals everything with that tag here, across categories and months."),
                            )
                        }
                    }
                }
                val maxSpent = summaries.maxOfOrNull { it.spent }?.coerceAtLeast(1L) ?: 1L
                items(summaries, key = { it.name }) { t ->
                    BudgyCard(onClick = { selected = t.name }, padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.Sell, MaterialTheme.colorScheme.tertiary, 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("#${t.name}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    tr("{0} transaction{1} · ", t.count, if (t.count == 1) "" else "s") +
                                        if (t.first == t.last) Dates.short(t.first) else "${Dates.short(t.first)} – ${Dates.short(t.last)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            MoneyText(t.spent, color = Budgy.colors.expense)
                        }
                        Spacer(Modifier.height(10.dp))
                        BudgetBar(t.spent.toFloat() / maxSpent, MaterialTheme.colorScheme.tertiary, height = 6.dp)
                    }
                }
            } else {
                val tagged = txns.filter { t -> tagList(t.tags).any { it.equals(sel, true) } }
                val summary = summaries.firstOrNull { it.name.equals(sel, true) }
                item {
                    BudgyCard {
                        Text(tr("Total spent"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(summary?.spent ?: 0L, style = MaterialTheme.typography.headlineMedium, color = Budgy.colors.expense)
                        if ((summary?.income ?: 0L) > 0) {
                            Text(tr("Income tagged: "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MoneyText(summary?.income ?: 0L, style = MaterialTheme.typography.bodyMedium, color = Budgy.colors.income)
                        }
                    }
                }
                // Category breakdown for the tag
                val stat = stats.filter { t -> t.type == TxType.EXPENSE && tagList(t.tags).any { it.equals(sel, true) } }
                val byCat = stat.groupBy { it.categoryId }.mapValues { (_, v) -> v.sumOf { it.amount } }.entries.sortedByDescending { it.value }
                if (byCat.isNotEmpty()) {
                    item {
                        BudgyCard {
                            Text(tr("By category"), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            val total = byCat.sumOf { it.value }.coerceAtLeast(1)
                            byCat.forEach { (id, v) ->
                                val c = id?.let { catMap[it] }
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(c?.name ?: tr("Uncategorized"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text("${v * 100 / total}%  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    MoneyText(v, style = MaterialTheme.typography.bodyMedium)
                                }
                                BudgetBar(v.toFloat() / total, c?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline, height = 5.dp)
                            }
                        }
                    }
                }
                item { SectionHeader(tr("Transactions")) }
                items(tagged, key = { it.id }) { t ->
                    TxnRow(t, t.categoryId?.let { catMap[it] }, acctMap, onClick = { go.editEntry(t.id) }, showDate = true)
                }
            }
        }
    }
}
