@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.budgy.app.ui.screens

import com.budgy.app.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.data.DefaultData
import com.budgy.app.data.Goal
import com.budgy.app.data.GoalEntry
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgetBar
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.CategoryIcons
import com.budgy.app.ui.components.ColorPicker
import com.budgy.app.ui.components.ConfirmDialog
import com.budgy.app.ui.components.DateField
import com.budgy.app.ui.components.EmptyState
import com.budgy.app.ui.components.IconBadge
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.ui.theme.Numeric
import com.budgy.app.util.Dates
import com.budgy.app.util.Money
import kotlin.math.ceil

private val goalIcons = listOf("savings", "travel", "home", "car", "phone", "education", "gift", "health", "fitness", "pets", "kids", "invest")

/** How much to put aside each week to reach the goal by its deadline, or null when there's no deadline. */
fun weeklyNeeded(g: Goal, saved: Long): Long? {
    val deadline = g.deadline ?: return null
    val left = g.target - saved
    if (left <= 0) return 0
    val days = (deadline - Dates.today()).coerceAtLeast(1)
    val weeks = ceil(days / 7.0).toLong().coerceAtLeast(1)
    return (left + weeks - 1) / weeks
}

@Composable
fun GoalsScreen(vm: BudgyViewModel, go: BudgyNav) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val saved by vm.goalSaved.collectAsStateWithLifecycle()
    val entries by vm.goalEntries.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Goal?>(null) }
    var creating by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf<Pair<Goal, Boolean>?>(null) } // goal, isDeposit

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tr("Savings goals")) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) } },
                actions = { IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, tr("Add goal")) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (goals.isEmpty()) {
                item {
                    BudgyCard {
                        EmptyState(
                            title = tr("Save for something"),
                            body = tr("A new phone, a trip, an emergency fund. Set a target and a date, and Budgy tells you how much to put aside each week."),
                            action = tr("Create a goal"),
                            onAction = { creating = true },
                        )
                    }
                }
            } else {
                item {
                    Text(
                        tr("Goals track money you set aside. They don't move money between your accounts."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            items(goals, key = { it.id }) { g ->
                val have = saved[g.id] ?: 0L
                val frac = if (g.target > 0) have.toFloat() / g.target else 0f
                val cur = Budgy.currency
                val last = entries.firstOrNull { it.goalId == g.id }
                BudgyCard(onClick = { editing = g }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(CategoryIcons.get(g.icon), Color(g.color), 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(g.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                tr("{0} of {1}", Money.format(have, cur), Money.format(g.target, cur)),
                                style = MaterialTheme.typography.bodySmall.merge(Numeric),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "${(frac * 100).toInt().coerceAtMost(999)}%",
                            style = MaterialTheme.typography.titleMedium.merge(Numeric),
                            color = if (frac >= 1f) Budgy.colors.income else Color(g.color),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    BudgetBar(frac, if (frac >= 1f) Budgy.colors.income else Color(g.color), height = 10.dp)
                    Spacer(Modifier.height(10.dp))
                    val need = weeklyNeeded(g, have)
                    Text(
                        when {
                            frac >= 1f -> tr("Goal reached. Nice work.")
                            need != null && g.deadline != null && g.deadline < Dates.today() ->
                                tr("Deadline passed · {0} to go", Money.format(g.target - have, cur))
                            need != null && g.deadline != null ->
                                tr("Save {0} a week to reach it by {1}", Money.format(need, cur), Dates.short(g.deadline))
                            else -> tr("{0} to go", Money.format(g.target - have, cur))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (frac >= 1f) Budgy.colors.income else MaterialTheme.colorScheme.onSurface,
                    )
                    if (last != null) {
                        Text(
                            if (last.amount >= 0) tr("Last added {0} · {1}", Money.format(kotlin.math.abs(last.amount), cur), Dates.dayLabel(last.date)) else tr("Last withdrew {0} · {1}", Money.format(kotlin.math.abs(last.amount), cur), Dates.dayLabel(last.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = { moving = g to true }, modifier = Modifier.weight(1f)) { Text(tr("Add money")) }
                        OutlinedButton(onClick = { moving = g to false }, modifier = Modifier.weight(1f), enabled = have > 0) { Text(tr("Withdraw")) }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        GoalEditor(
            initial = editing,
            onSave = { vm.saveGoal(it) },
            onDelete = { vm.deleteGoal(it) },
            onDismiss = { creating = false; editing = null },
        )
    }
    moving?.let { (g, deposit) ->
        GoalMoneyDialog(
            goal = g, deposit = deposit, available = saved[g.id] ?: 0L,
            onSave = { cents, note ->
                vm.addGoalEntry(GoalEntry(goalId = g.id, amount = if (deposit) cents else -cents, date = Dates.today(), note = note), g.name)
            },
            onDismiss = { moving = null },
        )
    }
}

@Composable
private fun GoalMoneyDialog(goal: Goal, deposit: Boolean, available: Long, onSave: (Long, String) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (deposit) tr("Add to {0}", goal.name) else tr("Withdraw from {0}", goal.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(12); error = null },
                    label = { Text(tr("Amount")) },
                    prefix = { Text(Budgy.currency) },
                    singleLine = true,
                    isError = error != null,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(40) },
                    label = { Text(tr("Note (optional)")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = Money.parse(amount)
                error = when {
                    cents == null || cents <= 0 -> tr("Enter an amount")
                    !deposit && cents > available -> tr("You only have {0} saved here", Money.format(available, ""))
                    else -> null
                }
                if (error == null && cents != null) { onSave(cents, note.trim()); onDismiss() }
            }) { Text(if (deposit) tr("Add") else tr("Withdraw")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

@Composable
private fun GoalEditor(initial: Goal?, onSave: (Goal) -> Unit, onDelete: (Goal) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var target by remember { mutableStateOf(initial?.let { Money.toInput(it.target) } ?: "") }
    var hasDeadline by remember { mutableStateOf(initial?.deadline != null) }
    var deadline by remember { mutableStateOf(initial?.deadline ?: (Dates.today() + 90)) }
    var icon by remember { mutableStateOf(initial?.icon ?: "savings") }
    var color by remember { mutableStateOf(initial?.color ?: DefaultData.swatches[10]) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) tr("New goal") else tr("Edit goal")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30); error = null },
                    label = { Text(tr("Goal")) },
                    placeholder = { Text(tr("Emergency fund, Japan trip…")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter { c -> c.isDigit() || c == '.' }.take(12); error = null },
                    label = { Text(tr("Target amount")) },
                    prefix = { Text(Budgy.currency) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                ToggleRow(tr("Target date"), tr("Get a weekly savings amount"), hasDeadline) { hasDeadline = it }
                if (hasDeadline) DateField(tr("Reach it by"), deadline) { deadline = it }
                Text(tr("Icon"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    goalIcons.forEach { key ->
                        val sel = key == icon
                        IconButton(onClick = { icon = key }) {
                            IconBadge(CategoryIcons.get(key), if (sel) Color(color) else MaterialTheme.colorScheme.outline, 38.dp)
                        }
                    }
                }
                ColorPicker(DefaultData.swatches, color) { color = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = Money.parse(target)
                error = when {
                    name.isBlank() -> tr("Name your goal")
                    cents == null || cents <= 0 -> tr("Enter a target amount")
                    else -> null
                }
                if (error == null && cents != null) {
                    val base = initial ?: Goal(name = "", target = cents, color = color)
                    onSave(base.copy(name = name.trim(), target = cents, deadline = if (hasDeadline) deadline else null, icon = icon, color = color))
                    onDismiss()
                }
            }) { Text(tr("Save")) }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = { confirmDelete = true }) { Text(tr("Delete"), color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
            }
        },
    )

    if (confirmDelete && initial != null) {
        ConfirmDialog(
            title = tr("Delete {0}?", initial.name),
            text = tr("Its saved history goes too. Your accounts aren't affected."),
            onConfirm = { onDelete(initial); onDismiss() },
            onDismiss = { confirmDelete = false },
        )
    }
}
