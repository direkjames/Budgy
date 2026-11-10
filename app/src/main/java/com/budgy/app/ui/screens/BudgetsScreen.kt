package com.budgy.app.ui.screens

import com.budgy.app.i18n.tr
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.data.Budget
import androidx.compose.material.icons.rounded.SwapHoriz
import com.budgy.app.util.envelopeState
import com.budgy.app.util.EnvelopeState
import com.budgy.app.util.PeriodSpec
import com.budgy.app.util.currentPeriod
import com.budgy.app.util.daysLeftIn
import com.budgy.app.util.isCalendarMonth
import com.budgy.app.util.periodLabel
import com.budgy.app.ui.spendingInRange
import com.budgy.app.data.EnvelopeMove
import com.budgy.app.data.Category
import com.budgy.app.data.CategoryKind
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgetBar
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.CategoryIcons
import com.budgy.app.ui.components.EmptyState
import com.budgy.app.ui.components.IconBadge
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.components.ScreenTitle
import com.budgy.app.ui.components.SectionHeader
import com.budgy.app.ui.components.SelectField
import com.budgy.app.ui.components.budgetColor
import com.budgy.app.ui.spendingByCategory
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.ui.theme.Numeric
import com.budgy.app.util.Money
import java.time.LocalDate
import java.time.YearMonth

private sealed interface BudgetDialog {
    data class New(val categoryId: Long? = null) : BudgetDialog
    data class Edit(val budget: Budget) : BudgetDialog
}

@Composable
fun BudgetsScreen(vm: BudgyViewModel, go: BudgyNav) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val budgets by vm.budgets.collectAsStateWithLifecycle()
    val txns by vm.statTxns.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val moves by vm.envelopeMoves.collectAsStateWithLifecycle()
    val envelopeMode = settings?.envelopeMode == true
    var moving by remember { mutableStateOf(false) }

    val spec = settings?.period ?: PeriodSpec()
    val period = currentPeriod(spec)
    val calendarMonth = isCalendarMonth(spec)
    val spent = remember(txns, period) { spendingInRange(txns, period) }
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val daysLeft = daysLeftIn(period)
    val budgeted = budgets.mapNotNull { b -> catMap[b.categoryId]?.let { it to b } }
        .sortedByDescending { (c, b) -> (spent[c.id] ?: 0L).toFloat() / b.monthlyLimit.coerceAtLeast(1) }
    val budgetedIds = budgets.map { it.categoryId }.toSet()
    val unbudgeted = spent.filterKeys { it != null && it !in budgetedIds }
        .mapNotNull { (id, v) -> catMap[id]?.let { it to v } }
        .sortedByDescending { it.second }

    var dialog by remember { mutableStateOf<BudgetDialog?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenTitle(
                tr("Budgets"),
                tr("{0} · {1} day{2} left", periodLabel(period), daysLeft, if (daysLeft == 1) "" else "s"),
                horizontalPadding = 4.dp,
            ) {
                FilledTonalButton(onClick = { dialog = BudgetDialog.New() }) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr("Add"))
                }
            }
        }

        if (budgeted.isEmpty()) {
            item {
                BudgyCard {
                    EmptyState(
                        title = tr("No budgets yet"),
                        body = tr("Set a spending limit for a category, like {0}5,000 for Food. Budgy shows what's left and how much you can spend per day.", Budgy.currency),
                        action = tr("Create your first budget"),
                        onAction = { dialog = BudgetDialog.New() },
                    )
                }
            }
        } else if (envelopeMode) {
            val states = budgeted.associate { (_, b) -> b.id to envelopeState(b, txns, moves, spec) }
            item {
                val cur = Budgy.currency
                BudgyCard {
                    Text(tr("In your envelopes"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(states.values.sumOf { it.available }, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        tr("Unspent money rolls over to the next period. Overspending comes out of the next one."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { moving = true }, enabled = budgeted.size >= 2) {
                            Icon(Icons.Rounded.SwapHoriz, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tr("Move money"))
                        }
                    }
                    if (budgeted.size < 2) {
                        Text(tr("Add a second envelope to move money between them."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(budgeted, key = { "e" + it.second.id }) { (c, b) ->
                EnvelopeRow(c, b, states.getValue(b.id), daysLeft) { dialog = BudgetDialog.Edit(b) }
            }
        } else {
            item {
                val total = budgeted.sumOf { it.second.monthlyLimit }
                val used = budgeted.sumOf { spent[it.first.id] ?: 0L }
                val frac = if (total > 0) used.toFloat() / total else 0f
                val left = total - used
                val cur = Budgy.currency
                BudgyCard {
                    Text(if (calendarMonth) tr("Total this month") else tr("Total this period"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.Bottom) {
                        MoneyText(used, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "  " + tr("of {0}", Money.format(total, cur)),
                            style = MaterialTheme.typography.bodyMedium.merge(Numeric),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    BudgetBar(frac, budgetColor(frac), height = 12.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (left >= 0) tr("You can spend {0} a day until {1}.", Money.format(left / daysLeft, cur), com.budgy.app.util.Dates.short(period.last))
                        else tr("You're {0} over your total budget.", Money.format(-left, cur)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (left >= 0) MaterialTheme.colorScheme.onSurface else Budgy.colors.expense,
                    )
                }
            }
            items(budgeted, key = { it.second.id }) { (c, b) ->
                BudgetRow(c, b, spent[c.id] ?: 0L, daysLeft) { dialog = BudgetDialog.Edit(b) }
            }
        }

        if (unbudgeted.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader(tr("Spending without a budget"))
            }
            items(unbudgeted, key = { "u" + it.first.id }) { (c, v) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { dialog = BudgetDialog.New(c.id) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(CategoryIcons.get(c.icon), Color(c.color), 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.titleSmall)
                        Text(tr("Tap to set a budget"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    MoneyText(v, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (moving) {
        val states = budgeted.associate { (c, b) -> c.id to envelopeState(b, txns, moves, spec).available }
        MoveMoneyDialog(
            envelopes = budgeted.map { it.first },
            available = states,
            onMove = { from, to, amt ->
                vm.moveEnvelope(EnvelopeMove(fromCategoryId = from, toCategoryId = to, amount = amt, date = com.budgy.app.util.Dates.today()))
            },
            onDismiss = { moving = false },
        )
    }

    when (val d = dialog) {
        null -> Unit
        is BudgetDialog.New -> BudgetEditor(
            title = tr("New budget"),
            choices = categories.filter { it.kind == CategoryKind.EXPENSE && it.id !in budgetedIds },
            initialCategoryId = d.categoryId,
            initialAmount = "",
            onSave = { catId, cents -> vm.saveBudget(Budget(categoryId = catId, monthlyLimit = cents, startDate = period.first)) },
            onDelete = null,
            onDismiss = { dialog = null },
        )
        is BudgetDialog.Edit -> BudgetEditor(
            title = tr("Edit budget"),
            choices = listOfNotNull(catMap[d.budget.categoryId]),
            initialCategoryId = d.budget.categoryId,
            initialAmount = Money.toInput(d.budget.monthlyLimit),
            onSave = { _, cents -> vm.saveBudget(d.budget.copy(monthlyLimit = cents)) },
            onDelete = { vm.deleteBudget(d.budget) },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun BudgetRow(c: Category, b: Budget, spent: Long, daysLeft: Int, onClick: () -> Unit) {
    val frac = spent.toFloat() / b.monthlyLimit.coerceAtLeast(1)
    val left = b.monthlyLimit - spent
    val cur = Budgy.currency
    BudgyCard(onClick = onClick, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(CategoryIcons.get(c.icon), Color(c.color), 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    tr("{0} of {1}", Money.format(spent, cur), Money.format(b.monthlyLimit, cur)),
                    style = MaterialTheme.typography.bodySmall.merge(Numeric),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${(frac * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge.merge(Numeric),
                color = budgetColor(frac),
            )
        }
        Spacer(Modifier.height(12.dp))
        BudgetBar(frac, budgetColor(frac))
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                left < 0 -> tr("Over by {0}", Money.format(-left, cur))
                left == 0L -> tr("Budget used up")
                else -> tr("{0} left · {1}/day for {2} days", Money.format(left, cur), Money.format(left / daysLeft, cur), daysLeft)
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (left < 0) Budgy.colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BudgetEditor(
    title: String,
    choices: List<Category>,
    initialCategoryId: Long?,
    initialAmount: String,
    onSave: (Long, Long) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var catId by remember { mutableStateOf(initialCategoryId ?: choices.firstOrNull()?.id) }
    var amount by remember { mutableStateOf(initialAmount) }
    var error by remember { mutableStateOf<String?>(null) }
    val cur = Budgy.currency

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (choices.isEmpty()) {
                    Text(tr("Every expense category already has a budget. Add a new category in Settings → Categories."))
                } else {
                    val sel = choices.firstOrNull { it.id == catId }
                    SelectField(
                        label = tr("Category"),
                        value = sel?.name ?: tr("Choose"),
                        options = choices,
                        optionLabel = { it.name },
                        onSelect = { catId = it.id },
                        leading = sel?.let { CategoryIcons.get(it.icon) },
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }; error = null },
                        label = { Text(tr("Limit per budget period")) },
                        prefix = { Text(cur) },
                        singleLine = true,
                        isError = error != null,
                        supportingText = errorText(error),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (choices.isNotEmpty()) {
                TextButton(onClick = {
                    val cents = Money.parse(amount)
                    val c = catId
                    when {
                        cents == null || cents <= 0 -> error = tr("Enter an amount above zero")
                        c == null -> error = tr("Pick a category")
                        else -> { onSave(c, cents); onDismiss() }
                    }
                }) { Text(tr("Save")) }
            } else {
                TextButton(onClick = onDismiss) { Text(tr("OK")) }
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = { onDelete(); onDismiss() }) {
                        Text(tr("Delete"), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
            }
        },
    )
}

private fun errorText(e: String?): (@Composable () -> Unit)? =
    if (e == null) null else { { Text(e) } }

@Composable
private fun EnvelopeRow(c: Category, b: Budget, st: EnvelopeState, daysLeft: Int, onClick: () -> Unit) {
    val cur = Budgy.currency
    val startOfMonth = st.available + st.spentThisMonth
    val frac = if (startOfMonth > 0) st.spentThisMonth.toFloat() / startOfMonth else if (st.spentThisMonth > 0) 1f else 0f
    BudgyCard(onClick = onClick, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(CategoryIcons.get(c.icon), Color(c.color), 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    tr("+{0} each period", Money.format(b.monthlyLimit, cur)),
                    style = MaterialTheme.typography.bodySmall.merge(Numeric),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(st.available, color = if (st.available < 0) Budgy.colors.expense else Budgy.colors.income)
                Text(tr("available"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        BudgetBar(frac, budgetColor(frac))
        Spacer(Modifier.height(8.dp))
        Text(
            buildString {
                append(tr("Spent {0} this period", Money.format(st.spentThisMonth, cur)))
                if (st.carriedIn > 0) append(tr(" · {0} rolled over", Money.format(st.carriedIn, cur)))
                if (st.carriedIn < 0) append(tr(" · {0} overspent before", Money.format(-st.carriedIn, cur)))
                if (st.available > 0) append(tr(" · {0}/day", Money.format(st.available / daysLeft, cur)))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MoveMoneyDialog(
    envelopes: List<Category>,
    available: Map<Long, Long>,
    onMove: (Long, Long, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var from by remember { mutableStateOf(envelopes.maxByOrNull { available[it.id] ?: 0L }?.id) }
    var to by remember { mutableStateOf(envelopes.firstOrNull { it.id != from }?.id) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val cur = Budgy.currency
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Move money")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val f = envelopes.firstOrNull { it.id == from }
                SelectField(
                    label = tr("From"),
                    value = f?.let { "${it.name} · ${Money.format(available[it.id] ?: 0L, cur)}" } ?: tr("Choose"),
                    options = envelopes, optionLabel = { "${it.name} · ${Money.format(available[it.id] ?: 0L, cur)}" },
                    onSelect = { from = it.id; if (to == it.id) to = envelopes.firstOrNull { e -> e.id != it.id }?.id },
                )
                SelectField(
                    label = tr("To"),
                    value = envelopes.firstOrNull { it.id == to }?.name ?: tr("Choose"),
                    options = envelopes.filter { it.id != from }, optionLabel = { it.name }, onSelect = { to = it.id },
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(12); error = null },
                    label = { Text(tr("Amount")) },
                    prefix = { Text(cur) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = errorText(error),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = Money.parse(amount)
                val f = from
                val t = to
                error = when {
                    f == null || t == null || f == t -> tr("Pick two different envelopes")
                    cents == null || cents <= 0 -> tr("Enter an amount")
                    cents > (available[f] ?: 0L) -> tr("Only {0} available in that envelope", Money.format(available[f] ?: 0L, cur))
                    else -> null
                }
                if (error == null && f != null && t != null && cents != null) { onMove(f, t, cents); onDismiss() }
            }) { Text(tr("Move")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}
