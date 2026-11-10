@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgy.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.i18n.tr
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.CategoryIcons
import com.budgy.app.ui.components.IconBadge
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.ui.theme.Numeric
import com.budgy.app.util.Dates
import com.budgy.app.util.Money
import com.budgy.app.util.yearReview
import java.time.LocalDate

private val monthShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

@Composable
fun YearReviewScreen(vm: BudgyViewModel, go: BudgyNav) {
    val txns by vm.statTxns.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val thisYear = LocalDate.now().year
    var year by rememberSaveable { mutableIntStateOf(thisYear) }
    val firstYear = txns.minOfOrNull { LocalDate.ofEpochDay(it.date).year } ?: thisYear
    val r = remember(txns, year) { yearReview(txns, year) }
    val cur = Budgy.currency

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tr("Year in review")) },
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
                    IconButton(onClick = { year-- }, enabled = year > firstYear) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, tr("Previous year")) }
                    Text("$year", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    IconButton(onClick = { year++ }, enabled = year < thisYear) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, tr("Next year")) }
                }
            }

            if (r.transactions == 0) {
                item {
                    BudgyCard {
                        Text(tr("Nothing logged in {0} yet.", year), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                return@LazyColumn
            }

            // Hero
            item {
                val c = Budgy.colors
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(listOf(c.heroStart, c.heroEnd)))
                        .padding(22.dp)
                ) {
                    Text(if (year == thisYear) tr("Your {0} so far", year) else tr("Your {0}", year),
                        color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(tr("You saved"), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                    MoneyText(r.saved, style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        tr("{0}% of what you earned", r.savingsRate),
                        color = c.gold, style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Income"), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            MoneyText(r.income, color = Color.White)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(tr("Spent"), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            MoneyText(r.spent, color = Color.White)
                        }
                    }
                }
            }

            // Month bars
            item {
                BudgyCard {
                    Text(tr("Spending by month"), style = MaterialTheme.typography.titleMedium)
                    r.biggestMonth?.let {
                        Text(
                            tr("Biggest: {0} ({1})", tr(monthShort[it.index]), Money.format(it.value, cur)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    MonthBars(r.byMonth, r.biggestMonth?.index ?: -1)
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        monthShort.forEach {
                            Text(tr(it).take(1), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Top categories
            item {
                BudgyCard {
                    Text(tr("Where your money went"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    r.topCategories.forEachIndexed { i, (id, v) ->
                        val c = id?.let { catMap[it] }
                        if (i > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = Budgy.colors.gold, modifier = Modifier.width(22.dp))
                            IconBadge(CategoryIcons.get(c?.icon), c?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline, 34.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(c?.name ?: tr("Uncategorized"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${if (r.spent > 0) v * 100 / r.spent else 0}%  ", style = MaterialTheme.typography.labelMedium.merge(Numeric),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MoneyText(v, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Highlights grid
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Highlight(tr("No-spend days"), "${r.noSpendDays}", tr("Best streak: {0}", r.bestStreak), Modifier.weight(1f))
                    Highlight(tr("Transactions"), "${r.transactions}", tr("on {0} days", r.daysLogged), Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Highlight(tr("Subscriptions"), Money.compact(r.subscriptions, cur), tr("paid this year"), Modifier.weight(1f))
                    Highlight(
                        tr("Cheapest month"),
                        r.cheapestMonth?.let { tr(monthShort[it.index]) } ?: "—",
                        r.cheapestMonth?.let { Money.compact(it.value, cur) } ?: "",
                        Modifier.weight(1f),
                    )
                }
            }
            item {
                BudgyCard {
                    r.biggest?.let { b ->
                        Text(tr("Biggest expense"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(b.amount, style = MaterialTheme.typography.titleLarge)
                        Text(
                            listOfNotNull(b.note.ifBlank { null }, b.categoryId?.let { catMap[it]?.name }, Dates.short(b.date)).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    r.topTag?.let { (tag, v) ->
                        Spacer(Modifier.height(12.dp))
                        Text(tr("Top tag"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("#$tag · ${Money.format(v, cur)}", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun Highlight(label: String, value: String, sub: String, modifier: Modifier) {
    BudgyCard(modifier, padding = 14.dp) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall.merge(Numeric))
        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MonthBars(values: List<Long>, highlight: Int) {
    val bar = MaterialTheme.colorScheme.primary
    val gold = Budgy.colors.gold
    val track = MaterialTheme.colorScheme.surfaceVariant
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val slot = size.width / 12
        val w = slot * 0.6f
        values.forEachIndexed { i, v ->
            val x = i * slot + (slot - w) / 2
            drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(w / 3, w / 3))
            if (v > 0) {
                val h = (size.height * v / max).coerceAtLeast(w / 2)
                drawRoundRect(if (i == highlight) gold else bar, Offset(x, size.height - h), Size(w, h), CornerRadius(w / 3, w / 3))
            }
        }
    }
}
