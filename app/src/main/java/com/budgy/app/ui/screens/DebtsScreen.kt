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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.data.Account
import com.budgy.app.data.DefaultData
import com.budgy.app.data.Debt
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.ColorPicker
import com.budgy.app.ui.components.ConfirmDialog
import com.budgy.app.ui.components.EmptyState
import com.budgy.app.ui.components.LetterAvatar
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.components.SectionHeader
import com.budgy.app.ui.components.SelectField
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.ui.theme.Numeric
import com.budgy.app.util.Money
import com.budgy.app.util.PayoffStrategy
import com.budgy.app.util.monthLabel
import com.budgy.app.util.simulatePayoff
import java.util.Locale

private fun aprText(bps: Int) = String.format(Locale.US, "%.2f%%", bps / 100.0).replace(".00%", "%")

@Composable
fun DebtsScreen(vm: BudgyViewModel, go: BudgyNav) {
    val debts by vm.debts.collectAsStateWithLifecycle()
    val accounts by vm.activeAccounts.collectAsStateWithLifecycle()
    var extra by rememberSaveable { mutableStateOf("1000") }
    var strategy by rememberSaveable { mutableStateOf(PayoffStrategy.AVALANCHE) }
    var editing by remember { mutableStateOf<Debt?>(null) }
    var creating by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<Debt?>(null) }
    val cur = Budgy.currency

    val extraCents = Money.parse(extra) ?: 0L
    val open = debts.filter { it.balance > 0 }
    val snow = remember(debts, extraCents) { simulatePayoff(debts, extraCents, PayoffStrategy.SNOWBALL) }
    val aval = remember(debts, extraCents) { simulatePayoff(debts, extraCents, PayoffStrategy.AVALANCHE) }
    val chosen = if (strategy == PayoffStrategy.SNOWBALL) snow else aval

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tr("Debt payoff")) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) } },
                actions = { IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, tr("Add debt")) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (debts.isEmpty()) {
                item {
                    BudgyCard {
                        EmptyState(
                            title = tr("Plan your way out of debt"),
                            body = tr("Add your credit cards and loans with their interest rate and minimum payment. Budgy shows when you'll be debt-free with the snowball or avalanche method."),
                            action = tr("Add a debt"),
                            onAction = { creating = true },
                        )
                    }
                }
                return@LazyColumn
            }

            // Totals + extra payment
            item {
                BudgyCard {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Total debt"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MoneyText(open.sumOf { it.balance }, style = MaterialTheme.typography.headlineSmall, color = Budgy.colors.expense)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(tr("Minimums / month"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MoneyText(open.sumOf { it.minPayment }, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = extra,
                        onValueChange = { extra = it.filter { c -> c.isDigit() || c == '.' }.take(10) },
                        label = { Text(tr("Extra I can pay each month")) },
                        prefix = { Text(cur) },
                        supportingText = { Text(tr("On top of the minimums. Paid-off debts' minimums roll into the next one.")) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Strategy comparison
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StrategyCard(PayoffStrategy.SNOWBALL, snow, strategy == PayoffStrategy.SNOWBALL, Modifier.weight(1f)) { strategy = it }
                    StrategyCard(PayoffStrategy.AVALANCHE, aval, strategy == PayoffStrategy.AVALANCHE, Modifier.weight(1f)) { strategy = it }
                }
            }
            item {
                val saved = snow.totalInterest - aval.totalInterest
                Text(
                    when {
                        !snow.feasible || !aval.feasible -> tr("Your payments don't cover the interest. Raise the extra amount to see a payoff date.")
                        saved > 0 -> tr("Avalanche saves you {0} in interest", Money.format(saved, cur)) +
                            if (snow.months != aval.months) tr(" and {0} month(s).", kotlin.math.abs(snow.months - aval.months)) else "."
                        else -> tr("Both methods cost about the same here. Pick the one that keeps you motivated.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (!snow.feasible) Budgy.colors.expense else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            // Payoff order for the chosen strategy
            if (chosen.feasible && chosen.order.isNotEmpty()) {
                item {
                    BudgyCard {
                        Text(tr("{0} order", strategy.label), style = MaterialTheme.typography.titleMedium)
                        Text(strategy.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        chosen.order.forEachIndexed { i, id ->
                            val d = debts.firstOrNull { it.id == id } ?: return@forEachIndexed
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(26.dp).clip(CircleShape).background(Color(d.color)),
                                    contentAlignment = Alignment.Center,
                                ) { Text("${i + 1}", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                                Spacer(Modifier.width(12.dp))
                                Text(d.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    tr("Paid off {0}", monthLabel(chosen.payoffMonth[id] ?: 0)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader(tr("Your debts")) }
            items(debts, key = { it.id }) { d ->
                BudgyCard(onClick = { editing = d }, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LetterAvatar(d.name, if (d.balance > 0) Color(d.color) else MaterialTheme.colorScheme.outline, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (d.balance <= 0) tr("Paid off") else tr("{0} APR · min {1}/mo", aprText(d.aprBps), Money.format(d.minPayment, cur)),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (d.balance <= 0) Budgy.colors.income else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        MoneyText(d.balance, color = if (d.balance > 0) Budgy.colors.expense else Budgy.colors.income)
                    }
                    if (d.balance > 0) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { paying = d }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text(tr("Record a payment"))
                        }
                    }
                }
            }
            item {
                Text(
                    tr("Tip: if you also track a credit card as an account, its balance and this debt describe the same money. Net worth counts both, so add a card in one place only."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }

    if (creating || editing != null) {
        DebtEditor(editing, onSave = { vm.saveDebt(it) }, onDelete = { vm.deleteDebt(it) }, onDismiss = { creating = false; editing = null })
    }
    paying?.let { d ->
        DebtPaymentDialog(d, accounts, onPay = { amt, acct -> vm.recordDebtPayment(d, amt, acct) }, onDismiss = { paying = null })
    }
}

@Composable
private fun StrategyCard(
    s: PayoffStrategy,
    r: com.budgy.app.util.PayoffResult,
    selected: Boolean,
    modifier: Modifier,
    onSelect: (PayoffStrategy) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else Budgy.colors.cardBorder, shape)
            .clickable { onSelect(s) }
            .padding(14.dp),
    ) {
        Text(s.label, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        if (r.feasible) {
            Text(tr("Debt-free"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(monthLabel(r.months), style = MaterialTheme.typography.titleMedium.merge(Numeric))
            Text(
                tr("{0} month{1}", r.months, if (r.months == 1) "" else "s"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(tr("Interest"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyText(r.totalInterest, style = MaterialTheme.typography.bodyMedium, color = Budgy.colors.expense)
        } else {
            Text(tr("Never at this rate"), style = MaterialTheme.typography.bodyMedium, color = Budgy.colors.expense)
        }
    }
}

@Composable
private fun DebtPaymentDialog(d: Debt, accounts: List<Account>, onPay: (Long, Long?) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(Money.toInput(minOf(d.minPayment, d.balance))) }
    var log by remember { mutableStateOf(true) }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Pay {0}", d.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(12); error = null },
                    label = { Text(tr("Amount paid")) },
                    prefix = { Text(Budgy.currency) },
                    singleLine = true,
                    isError = error != null,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                ToggleRow(tr("Log as an expense"), tr("Adds it to Activity under \"Debt payments\""), log) { log = it }
                if (log) {
                    SelectField(
                        label = tr("Paid from"),
                        value = accounts.firstOrNull { it.id == accountId }?.name ?: tr("Choose"),
                        options = accounts, optionLabel = { it.name }, onSelect = { accountId = it.id },
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = Money.parse(amount)
                error = if (cents == null || cents <= 0) tr("Enter an amount") else null
                if (cents != null && cents > 0) { onPay(cents, if (log) accountId else null); onDismiss() }
            }) { Text(tr("Record")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

@Composable
private fun DebtEditor(initial: Debt?, onSave: (Debt) -> Unit, onDelete: (Debt) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var balance by remember { mutableStateOf(initial?.let { Money.toInput(it.balance) } ?: "") }
    var apr by remember { mutableStateOf(initial?.let { (it.aprBps / 100.0).toString().removeSuffix(".0") } ?: "") }
    var min by remember { mutableStateOf(initial?.let { Money.toInput(it.minPayment) } ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: DefaultData.swatches[8]) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) tr("New debt") else tr("Edit debt")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(30); error = null },
                    label = { Text(tr("Name")) }, placeholder = { Text(tr("BPI credit card, Car loan…")) },
                    singleLine = true, shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = balance, onValueChange = { balance = it.filter { c -> c.isDigit() || c == '.' }.take(12); error = null },
                    label = { Text(tr("Current balance")) }, prefix = { Text(Budgy.currency) },
                    singleLine = true, shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = apr, onValueChange = { apr = it.filter { c -> c.isDigit() || c == '.' }.take(6); error = null },
                    label = { Text(tr("Interest rate per year (APR)")) }, suffix = { Text("%") },
                    supportingText = { Text(tr("Cards often show a monthly rate. Multiply by 12 (3% a month = 36%).")) },
                    singleLine = true, shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = min, onValueChange = { min = it.filter { c -> c.isDigit() || c == '.' }.take(12); error = null },
                    label = { Text(tr("Minimum payment per month")) }, prefix = { Text(Budgy.currency) },
                    singleLine = true, shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                ColorPicker(DefaultData.swatches, color) { color = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val bal = Money.parse(balance)
                val minC = Money.parse(min)
                val aprBps = apr.toDoubleOrNull()?.let { (it * 100).toInt() }
                error = when {
                    name.isBlank() -> tr("Name it")
                    bal == null -> tr("Enter the balance")
                    aprBps == null || aprBps < 0 || aprBps > 50000 -> tr("Enter the yearly interest rate")
                    minC == null || minC <= 0 -> tr("Enter the minimum payment")
                    else -> null
                }
                if (error == null && bal != null && minC != null && aprBps != null) {
                    val base = initial ?: Debt(name = "", balance = bal, aprBps = aprBps, minPayment = minC, color = color)
                    onSave(base.copy(name = name.trim(), balance = bal, aprBps = aprBps, minPayment = minC, color = color))
                    onDismiss()
                }
            }) { Text(tr("Save")) }
        },
        dismissButton = {
            Row {
                if (initial != null) TextButton(onClick = { confirmDelete = true }) { Text(tr("Delete"), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
            }
        },
    )
    if (confirmDelete && initial != null) {
        ConfirmDialog(
            title = tr("Delete {0}?", initial.name),
            text = tr("Payments you logged stay in your transactions."),
            onConfirm = { onDelete(initial); onDismiss() },
            onDismiss = { confirmDelete = false },
        )
    }
}
