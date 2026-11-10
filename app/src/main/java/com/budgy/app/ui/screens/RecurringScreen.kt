@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.budgy.app.ui.screens

import com.budgy.app.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.data.Account
import com.budgy.app.data.BillingCycle
import com.budgy.app.data.Category
import com.budgy.app.data.CategoryKind
import com.budgy.app.data.Recurring
import com.budgy.app.data.TxType
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.CategoryIcons
import com.budgy.app.ui.components.ConfirmDialog
import com.budgy.app.ui.components.DateField
import com.budgy.app.ui.components.EmptyState
import com.budgy.app.ui.components.IconBadge
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.components.SelectField
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.util.Dates
import com.budgy.app.util.Money
import com.budgy.app.util.cycleText

@Composable
fun RecurringScreen(vm: BudgyViewModel, go: BudgyNav) {
    val list by vm.recurring.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val accounts by vm.activeAccounts.collectAsStateWithLifecycle()
    val catMap = remember(categories) { categories.associateBy { it.id } }
    var editing by remember { mutableStateOf<Recurring?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tr("Recurring")) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) } },
                actions = { IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, tr("Add recurring")) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (list.isEmpty()) {
                item {
                    BudgyCard {
                        EmptyState(
                            title = tr("Nothing repeats yet"),
                            body = tr("Rent, salary, allowance, savings transfers — set them once and Budgy logs them for you. You can also pick \"Repeat\" when adding a transaction."),
                            action = tr("Add recurring"),
                            onAction = { creating = true },
                        )
                    }
                }
            }
            items(list, key = { it.id }) { r ->
                val cat = r.categoryId?.let { catMap[it] }
                BudgyCard(onClick = { editing = r }, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (r.type == TxType.TRANSFER) {
                            IconBadge(Icons.Rounded.SwapHoriz, MaterialTheme.colorScheme.tertiary)
                        } else {
                            IconBadge(CategoryIcons.get(cat?.icon), cat?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                r.note.ifBlank { cat?.name ?: r.type.label },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                when {
                                    !r.active -> tr("Paused · {0}", cycleText(r.cycle, r.customDays))
                                    else -> tr("{0} · next {1} · ", cycleText(r.cycle, r.customDays), Dates.dayLabel(r.nextDate)) +
                                        if (r.autoLog) tr("auto-logs") else tr("asks first")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        when (r.type) {
                            TxType.INCOME -> MoneyText(r.amount, color = Budgy.colors.income, showPlus = true)
                            TxType.EXPENSE -> MoneyText(-r.amount, color = Budgy.colors.expense)
                            else -> MoneyText(r.amount)
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        RecurringEditor(
            initial = editing,
            accounts = accounts,
            categories = categories,
            onSave = { vm.saveRecurring(it) },
            onDelete = { vm.deleteRecurring(it) },
            onDismiss = { creating = false; editing = null },
        )
    }
}

@Composable
private fun RecurringEditor(
    initial: Recurring?,
    accounts: List<Account>,
    categories: List<Category>,
    onSave: (Recurring) -> Unit,
    onDelete: (Recurring) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by remember { mutableStateOf(initial?.type ?: TxType.EXPENSE) }
    var amount by remember { mutableStateOf(initial?.let { Money.toInput(it.amount) } ?: "") }
    var accountId by remember { mutableStateOf(initial?.accountId ?: accounts.firstOrNull()?.id) }
    var toAccountId by remember { mutableStateOf(initial?.toAccountId ?: accounts.getOrNull(1)?.id) }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var cycle by remember { mutableStateOf(initial?.cycle ?: BillingCycle.MONTHLY) }
    var customDays by remember { mutableStateOf((initial?.customDays ?: 30).toString()) }
    var nextDate by remember { mutableStateOf(initial?.nextDate ?: Dates.today()) }
    var autoLog by remember { mutableStateOf(initial?.autoLog ?: true) }
    var active by remember { mutableStateOf(initial?.active ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val kind = if (type == TxType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
    val cats = categories.filter { it.kind == kind }
    LaunchedEffect(type, categories) {
        if (type != TxType.TRANSFER && cats.none { it.id == categoryId }) categoryId = cats.firstOrNull()?.id
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) tr("New recurring") else tr("Edit recurring")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TxType.entryTypes.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(12); error = null },
                    label = { Text(tr("Amount")) },
                    prefix = { Text(Budgy.currency) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(40) },
                    label = { Text(tr("Name")) },
                    placeholder = { Text(tr("Rent, Salary, Allowance…")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                val from = accounts.firstOrNull { it.id == accountId }
                SelectField(
                    label = if (type == TxType.TRANSFER) tr("From") else tr("Account"),
                    value = from?.name ?: tr("Choose"),
                    options = accounts, optionLabel = { it.name }, onSelect = { accountId = it.id },
                )
                if (type == TxType.TRANSFER) {
                    SelectField(
                        label = tr("To"),
                        value = accounts.firstOrNull { it.id == toAccountId }?.name ?: tr("Choose"),
                        options = accounts.filter { it.id != accountId }, optionLabel = { it.name },
                        onSelect = { toAccountId = it.id },
                    )
                } else {
                    val cat = cats.firstOrNull { it.id == categoryId }
                    SelectField(
                        label = tr("Category"), value = cat?.name ?: tr("Choose"),
                        options = cats, optionLabel = { it.name }, onSelect = { categoryId = it.id },
                        leading = cat?.let { CategoryIcons.get(it.icon) },
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BillingCycle.entries.forEach { c ->
                        FilterChip(selected = cycle == c, onClick = { cycle = c }, label = { Text(c.label) })
                    }
                }
                if (cycle == BillingCycle.CUSTOM) {
                    OutlinedTextField(
                        value = customDays,
                        onValueChange = { customDays = it.filter(Char::isDigit).take(4) },
                        label = { Text(tr("Every (days)")) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                DateField(tr("Next date"), nextDate) { nextDate = it }
                ToggleRow(tr("Log automatically"), if (autoLog) tr("Added on the date without asking") else tr("Shows on Home for you to confirm or skip"), autoLog) { autoLog = it }
                if (initial != null) ToggleRow(tr("Active"), tr("Turn off to pause"), active) { active = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = Money.parse(amount)
                val acct = accountId
                error = when {
                    cents == null || cents <= 0 -> tr("Enter an amount")
                    acct == null -> tr("Add an account first")
                    type == TxType.TRANSFER && (toAccountId == null || toAccountId == acct) -> tr("Pick two different accounts")
                    else -> null
                }
                if (error == null && cents != null && acct != null) {
                    val base = initial ?: Recurring(type = type, amount = cents, accountId = acct, cycle = cycle, nextDate = nextDate)
                    onSave(
                        base.copy(
                            type = type, amount = cents, accountId = acct,
                            toAccountId = if (type == TxType.TRANSFER) toAccountId else null,
                            categoryId = if (type == TxType.TRANSFER) null else categoryId,
                            note = note.trim(), cycle = cycle,
                            customDays = customDays.toIntOrNull()?.coerceIn(1, 3650) ?: 30,
                            nextDate = nextDate, autoLog = autoLog, active = active,
                        )
                    )
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
            title = tr("Stop repeating?"),
            text = tr("Entries it already logged stay in your transactions."),
            confirm = tr("Stop"),
            onConfirm = { onDelete(initial); onDismiss() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
