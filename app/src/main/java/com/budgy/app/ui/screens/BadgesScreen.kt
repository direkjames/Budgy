@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgy.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrackChanges
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.i18n.tr
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgetBar
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.SectionHeader
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.util.Badge
import com.budgy.app.util.BadgeGroup

fun badgeIcon(key: String): ImageVector = when (key) {
    "fire" -> Icons.Rounded.LocalFireDepartment
    "savings" -> Icons.Rounded.Savings
    "target" -> Icons.Rounded.TrackChanges
    "card" -> Icons.Rounded.CreditCard
    "trophy" -> Icons.Rounded.EmojiEvents
    "flag" -> Icons.Rounded.Flag
    "calendar" -> Icons.Rounded.CalendarMonth
    else -> Icons.AutoMirrored.Rounded.ReceiptLong
}

@Composable
fun BadgesScreen(vm: BudgyViewModel, go: BudgyNav) {
    val badges by vm.badges.collectAsStateWithLifecycle()
    val unlocked = badges.count { it.unlocked }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tr("Badges")) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                BudgyCard {
                    Text(tr("{0} of {1} unlocked", unlocked, badges.size), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BudgetBar(if (badges.isEmpty()) 0f else unlocked.toFloat() / badges.size, Budgy.colors.gold, height = 10.dp)
                }
            }
            BadgeGroup.entries.forEach { g ->
                val list = badges.filter { it.group == g }
                if (list.isEmpty()) return@forEach
                item { SectionHeader(tr(g.label)) }
                list.chunked(2).forEach { row ->
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { b -> BadgeCard(b, Modifier.weight(1f)) }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(b: Badge, modifier: Modifier) {
    val gold = Budgy.colors.gold
    BudgyCard(modifier, padding = 14.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (b.unlocked) gold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (b.unlocked) badgeIcon(b.icon) else Icons.Rounded.Lock, null,
                    tint = if (b.unlocked) gold else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(tr(b.title), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(
                tr(b.description), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            if (!b.unlocked) {
                Spacer(Modifier.height(8.dp))
                BudgetBar(b.progress, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), height = 5.dp)
            }
        }
    }
}
