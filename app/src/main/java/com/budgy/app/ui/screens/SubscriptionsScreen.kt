package com.budgy.app.ui.screens

import com.budgy.app.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.data.Subscription
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.EmptyState
import com.budgy.app.ui.components.LetterAvatar
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.components.ScreenTitle
import com.budgy.app.ui.components.SectionHeader
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.ui.theme.Numeric
import com.budgy.app.util.Dates
import com.budgy.app.util.Money
import com.budgy.app.util.cycleLabel
import com.budgy.app.util.cycleSuffix
import com.budgy.app.util.keyDate
import com.budgy.app.util.monthlyCost
import com.budgy.app.util.myAmount

@Composable
fun SubscriptionsScreen(vm: BudgyViewModel, go: BudgyNav) {
    val subs by vm.subscriptions.collectAsStateWithLifecycle()
    val active = subs.filter { it.active }
    val trials = active.filter { it.isTrial }.sortedBy { it.keyDate() }
    val paid = active.filter { !it.isTrial }.sortedBy { it.nextBillingDate }
    val paused = subs.filter { !it.active }
    // Trials cost nothing yet, so only paid subscriptions count toward totals.
    val monthly = paid.sumOf { it.monthlyCost() }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenTitle(tr("Subscriptions"), tr("{0} active", active.size), horizontalPadding = 4.dp) {
                FilledTonalButton(onClick = go::newSubscription) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr("Add"))
                }
            }
        }

        if (subs.isEmpty()) {
            item {
                BudgyCard {
                    EmptyState(
                        title = tr("Track your subscriptions"),
                        body = tr("Add Netflix, Spotify, iCloud, your gym — anything that renews. Budgy totals them up and reminds you before you're charged, including free trials."),
                        action = tr("Add a subscription"),
                        onAction = go::newSubscription,
                    )
                }
            }
            return@LazyColumn
        }

        // Totals
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(20.dp)
            ) {
                Text(tr("You spend"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                Row(verticalAlignment = Alignment.Bottom) {
                    MoneyText(monthly, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        tr(" / month"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    tr("That's {0} a year on {1} subscription{2}.", Money.format(monthly * 12, Budgy.currency), paid.size, if (paid.size == 1) "" else "s"),
                    style = MaterialTheme.typography.bodyMedium.merge(Numeric),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        if (trials.isNotEmpty()) {
            item { SectionHeader(tr("Free trials")) }
            items(trials, key = { it.id }) { s -> SubRow(s, vm, go) }
        }
        if (paid.isNotEmpty()) {
            item { SectionHeader(tr("Upcoming")) }
            items(paid, key = { it.id }) { s -> SubRow(s, vm, go) }
        }
        if (paused.isNotEmpty()) {
            item { SectionHeader(tr("Paused")) }
            items(paused, key = { it.id }) { s -> SubRow(s, vm, go) }
        }
    }
}

@Composable
private fun SubRow(s: Subscription, vm: BudgyViewModel, go: BudgyNav) {
    val today = Dates.today()
    val days = s.keyDate() - today
    val dueColor = when {
        !s.active -> MaterialTheme.colorScheme.onSurfaceVariant
        days < 0 -> Budgy.colors.expense
        days <= 3 -> Budgy.colors.warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    BudgyCard(onClick = { go.editSubscription(s.id) }, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LetterAvatar(s.name, if (s.active) Color(s.color) else MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (s.isTrial && s.active) {
                        Icon(Icons.Rounded.HourglassTop, null, tint = dueColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        when {
                            !s.active -> tr("Paused · {0}", s.cycleLabel())
                            s.isTrial -> tr("Trial ends ") + Dates.dueLabel(s.keyDate()).lowercase()
                            else -> "${s.cycleLabel()} · " + Dates.dueLabel(s.nextBillingDate)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = dueColor,
                    )
                }
                val unused = s.lastUsed?.let { today - it }
                if (s.active && unused != null && unused >= 30) {
                    Text(
                        tr("Not used in {0} days · cancel?", unused),
                        style = MaterialTheme.typography.bodySmall,
                        color = Budgy.colors.warning,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(s.myAmount(), style = MaterialTheme.typography.bodyLarge)
                Text(
                    s.cycleSuffix() + if (s.sharedWith > 1) tr(" · your share") else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Quick "paid" action when a charge is close or overdue.
        if (s.active && !s.isTrial && days <= 3) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { vm.markPaid(s) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) { Text(tr("Mark as paid · logs {0}", Money.format(s.myAmount(), Budgy.currency))) }
        }
    }
}
