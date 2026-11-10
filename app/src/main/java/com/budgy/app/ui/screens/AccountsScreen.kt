@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.budgy.app.ui.screens

import com.budgy.app.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import com.budgy.app.util.parseRate
import com.budgy.app.util.rateText
import com.budgy.app.util.symbol
import com.budgy.app.util.isForeign
import com.budgy.app.ui.components.SelectField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.budgy.app.data.Account
import com.budgy.app.data.AccountType
import com.budgy.app.data.DefaultData
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.ColorPicker
import com.budgy.app.ui.components.ConfirmDialog
import com.budgy.app.ui.components.IconBadge
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.components.icon
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.util.Money

@Composable
fun AccountsScreen(vm: BudgyViewModel, go: BudgyNav) {
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    val homeBalances by vm.homeBalances.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Account?>(null) }
    var creating by remember { mutableStateOf(false) }
    var adjusting by remember { mutableStateOf<Account?>(null) }
    var showArchived by remember { mutableStateOf(false) }
    val home = Budgy.currency
    val active = accounts.filter { !it.archived }
    val archived = accounts.filter { it.archived }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tr("Accounts")) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) } },
                actions = { IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, tr("Add account")) } },
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
                    Text(tr("Total across accounts"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(homeBalances.values.sum(), style = MaterialTheme.typography.headlineMedium)
                    if (accounts.any { it.isForeign(home) }) {
                        Text(
                            tr("Other currencies are converted with the rates you set on each account."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(active, key = { it.id }) { a ->
                AccountRow(a, balances[a.id] ?: a.startingBalance, homeBalances[a.id], home, onClick = { editing = a }, onAdjust = { adjusting = a })
            }
            if (archived.isNotEmpty()) {
                item {
                    TextButton(onClick = { showArchived = !showArchived }) {
                        Text(if (showArchived) tr("Hide archived ({0})", archived.size) else tr("Show archived ({0})", archived.size))
                    }
                }
                if (showArchived) {
                    items(archived, key = { it.id }) { a ->
                        AccountRow(a, balances[a.id] ?: a.startingBalance, homeBalances[a.id], home, onClick = { editing = a }, onAdjust = null)
                    }
                }
            }
            item {
                Text(
                    tr("Tip: transfers between accounts (like cash-in to an e-wallet) don't count as spending. ") +
                        tr("Use the tune icon to fix a balance that doesn't match your real one."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (creating || editing != null) {
        AccountEditor(
            initial = editing,
            home = home,
            onSave = { vm.saveAccount(it) },
            onDelete = { vm.deleteAccount(it) },
            onArchive = { a, v -> vm.saveAccount(a.copy(archived = v)) },
            onDismiss = { creating = false; editing = null },
        )
    }
    adjusting?.let { a ->
        AdjustDialog(a, balances[a.id] ?: a.startingBalance, a.symbol(home),
            onSave = { actual, note -> vm.adjustBalance(a, balances[a.id] ?: a.startingBalance, actual, note) },
            onDismiss = { adjusting = null })
    }
}

@Composable
private fun AccountRow(a: Account, balance: Long, homeValue: Long?, home: String, onClick: () -> Unit, onAdjust: (() -> Unit)?) {
    BudgyCard(onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(a.type.icon(), if (a.archived) MaterialTheme.colorScheme.outline else Color(a.color))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    buildString {
                        append(a.type.label)
                        if (a.isForeign(home)) append(" · ${a.currency} · 1 = ${home}${rateText(a.fxRateMicros)}")
                        if (a.archived) append(tr(" · Archived"))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(balance, color = if (balance < 0) Budgy.colors.expense else Color.Unspecified, symbol = a.symbol(home))
                if (a.isForeign(home) && homeValue != null) {
                    Text("≈ " + Money.format(homeValue, home), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onAdjust != null) {
                IconButton(onClick = onAdjust) { Icon(Icons.Rounded.Tune, tr("Adjust balance"), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun AdjustDialog(a: Account, current: Long, symbol: String, onSave: (Long, String) -> Unit, onDismiss: () -> Unit) {
    var actual by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val parsed = Money.parse(actual, allowNegative = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Adjust {0}", a.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tr("Budgy shows {0}. What's the real balance right now?", Money.format(current, symbol)))
                OutlinedTextField(
                    value = actual,
                    onValueChange = { v -> actual = v.filterIndexed { i, c -> c.isDigit() || c == '.' || (c == '-' && i == 0) }.take(14); error = null },
                    label = { Text(tr("Actual balance")) },
                    prefix = { Text(symbol) },
                    singleLine = true,
                    isError = error != null,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (parsed != null && parsed != current) {
                    Text(
                        tr("Adds a balance adjustment of {0}. It won't count as income or spending.", Money.format(parsed - current, symbol, showPlus = true)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(60) },
                    label = { Text(tr("Note (optional)")) },
                    placeholder = { Text(tr("Forgot to log some cash spending")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    parsed == null -> tr("Enter the balance")
                    parsed == current -> tr("That's already the balance")
                    else -> null
                }
                if (error == null && parsed != null) { onSave(parsed, note.trim()); onDismiss() }
            }) { Text(tr("Adjust")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

@Composable
private fun AccountEditor(
    initial: Account?,
    home: String,
    onSave: (Account) -> Unit,
    onDelete: (Account) -> Unit,
    onArchive: (Account, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: AccountType.BANK) }
    var start by remember { mutableStateOf(initial?.let { Money.toInput(it.startingBalance) } ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: DefaultData.swatches[3]) }
    var currency by remember { mutableStateOf(initial?.currency?.takeIf { it != home } ?: "") }
    var rate by remember { mutableStateOf(initial?.takeIf { it.currency.isNotEmpty() && it.currency != home }?.let { rateText(it.fxRateMicros).replace(",", "") } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val foreign = currency.isNotEmpty()
    val sym = if (foreign) currency else home

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) tr("New account") else tr("Edit account")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30); error = null },
                    label = { Text(tr("Name")) },
                    placeholder = { Text(tr("BDO Savings, GCash, Wallet…")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label) },
                            leadingIcon = { Icon(t.icon(), null) },
                        )
                    }
                }
                val options: List<Pair<String, String>> = listOf("" to tr("Main currency ({0})", home)) +
                    Money.currencies.filter { it.first != home }.map { it.first to "${it.first}  ${it.second}" }
                SelectField(
                    label = tr("Currency"),
                    value = options.firstOrNull { it.first == currency }?.second ?: currency,
                    options = options,
                    optionLabel = { it.second },
                    onSelect = { currency = it.first; error = null },
                )
                if (foreign) {
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' }.take(12); error = null },
                        label = { Text(tr("1 {0} = how much in {1}?", currency, home)) },
                        prefix = { Text(home) },
                        supportingText = { Text(tr("Budgy is offline, so update this when the rate changes.")) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = start,
                    onValueChange = { v -> start = v.filterIndexed { i, c -> c.isDigit() || c == '.' || (c == '-' && i == 0) }; error = null },
                    label = { Text(if (type == AccountType.CARD) tr("Starting balance (use − for amount owed)") else tr("Starting balance")) },
                    prefix = { Text(sym) },
                    placeholder = { Text("0") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
                ColorPicker(DefaultData.swatches, color) { color = it }
                if (initial != null) {
                    OutlinedButton(onClick = { onArchive(initial, !initial.archived); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (initial.archived) tr("Unarchive") else tr("Archive (hide from pickers, keep history)"))
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = if (start.isBlank()) 0L else Money.parse(start, allowNegative = true)
                val micros = if (foreign) parseRate(rate) else 1_000_000L
                when {
                    name.isBlank() -> error = tr("Give the account a name")
                    cents == null -> error = tr("Starting balance isn't a valid amount")
                    micros == null -> error = tr("Enter the exchange rate")
                    else -> {
                        val base = initial ?: Account(name = "", type = type, color = color)
                        onSave(base.copy(name = name.trim(), type = type, startingBalance = cents, color = color, currency = currency, fxRateMicros = micros))
                        onDismiss()
                    }
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
            text = tr("Accounts with transactions can't be deleted, so your history stays accurate. Archive it instead to hide it."),
            onConfirm = { onDelete(initial); onDismiss() },
            onDismiss = { confirmDelete = false },
        )
    }
}
