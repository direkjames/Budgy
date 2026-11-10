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
import androidx.compose.material3.FilterChip
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
import com.budgy.app.data.Account
import com.budgy.app.data.IouDirection
import com.budgy.app.data.Iou
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.ConfirmDialog
import com.budgy.app.ui.components.EmptyState
import com.budgy.app.ui.components.LetterAvatar
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.components.SectionHeader
import com.budgy.app.ui.components.SelectField
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.util.Dates
import com.budgy.app.util.Money

private val avatarColors = listOf(0xFF3E8EDE, 0xFF8E6CEF, 0xFFD9578F, 0xFF2BA3A0, 0xFFF28C38, 0xFF12A15F)
private fun personColor(name: String) = Color(avatarColors[(name.lowercase().hashCode() and 0x7fffffff) % avatarColors.size])

@Composable
fun IousScreen(vm: BudgyViewModel, go: BudgyNav) {
    val ious by vm.ious.collectAsStateWithLifecycle()
    val accounts by vm.activeAccounts.collectAsStateWithLifecycle()
    val open = ious.filter { !it.settled }
    val owedToMe = open.filter { it.direction == IouDirection.THEY_OWE }
    val iOwe = open.filter { it.direction == IouDirection.I_OWE }
    val settled = ious.filter { it.settled }.take(30)
    var creating by remember { mutableStateOf(false) }
    var settling by remember { mutableStateOf<Iou?>(null) }
    var deleting by remember { mutableStateOf<Iou?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tr("IOUs")) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) } },
                actions = { IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, tr("Add IOU")) } },
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
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Owed to you"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MoneyText(owedToMe.sumOf { it.amount }, style = MaterialTheme.typography.headlineSmall, color = Budgy.colors.income)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(tr("You owe"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MoneyText(iOwe.sumOf { it.amount }, style = MaterialTheme.typography.headlineSmall, color = Budgy.colors.expense)
                        }
                    }
                }
            }
            if (ious.isEmpty()) {
                item {
                    BudgyCard {
                        EmptyState(
                            title = tr("No IOUs"),
                            body = tr("Split a bill when adding an expense, or tap + to note money you lent or borrowed. Settle up when it's paid back."),
                            action = tr("Add an IOU"),
                            onAction = { creating = true },
                        )
                    }
                }
            }
            if (owedToMe.isNotEmpty()) {
                item { SectionHeader(tr("Owed to you")) }
                items(owedToMe, key = { it.id }) { i -> IouRow(i, onSettle = { settling = i }, onDelete = { deleting = i }) }
            }
            if (iOwe.isNotEmpty()) {
                item { SectionHeader(tr("You owe")) }
                items(iOwe, key = { it.id }) { i -> IouRow(i, onSettle = { settling = i }, onDelete = { deleting = i }) }
            }
            if (settled.isNotEmpty()) {
                item { SectionHeader(tr("Settled")) }
                items(settled, key = { it.id }) { i -> IouRow(i, onSettle = null, onDelete = { deleting = i }) }
            }
        }
    }

    if (creating) {
        IouEditor(accounts, onSave = { iou, acct -> vm.addIou(iou, acct) }, onDismiss = { creating = false })
    }
    settling?.let { i ->
        SettleDialog(i, accounts, onSettle = { acct -> vm.settleIou(i, acct) }, onDismiss = { settling = null })
    }
    deleting?.let { i ->
        ConfirmDialog(
            title = tr("Delete this IOU?"),
            text = tr("Money movements already logged in your transactions stay there."),
            onConfirm = { vm.deleteIou(i) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun IouRow(i: Iou, onSettle: (() -> Unit)?, onDelete: () -> Unit) {
    BudgyCard(padding = 14.dp, onClick = onDelete.takeIf { onSettle == null }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LetterAvatar(i.person, if (i.settled) MaterialTheme.colorScheme.outline else personColor(i.person), 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(i.person, style = MaterialTheme.typography.titleSmall)
                Text(
                    buildString {
                        append(if (i.settled) tr("Settled {0}", i.settledDate?.let { Dates.short(it) } ?: "") else Dates.dayLabel(i.date))
                        if (i.note.isNotBlank()) append(" · ").append(i.note)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            MoneyText(
                i.amount,
                color = when {
                    i.settled -> MaterialTheme.colorScheme.onSurfaceVariant
                    i.direction == IouDirection.THEY_OWE -> Budgy.colors.income
                    else -> Budgy.colors.expense
                },
            )
        }
        if (onSettle != null) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSettle, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Text(if (i.direction == IouDirection.THEY_OWE) tr("Got paid back") else tr("I paid it back"))
                }
                TextButton(onClick = onDelete) { Text(tr("Delete"), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun SettleDialog(i: Iou, accounts: List<Account>, onSettle: (Long?) -> Unit, onDismiss: () -> Unit) {
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var noMoney by remember { mutableStateOf(false) }
    val theyOwe = i.direction == IouDirection.THEY_OWE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Settle with {0}", i.person)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (theyOwe) tr("{0} pays you {1}.", i.person, Money.format(i.amount, Budgy.currency))
                    else tr("You pay {0} {1}.", i.person, Money.format(i.amount, Budgy.currency)),
                )
                if (!noMoney) {
                    SelectField(
                        label = if (theyOwe) tr("Money goes into") else tr("Paid from"),
                        value = accounts.firstOrNull { it.id == accountId }?.name ?: tr("Choose"),
                        options = accounts, optionLabel = { it.name }, onSelect = { accountId = it.id },
                    )
                }
                ToggleRow(
                    tr("Don't log money"),
                    tr("Just mark it settled (e.g. they paid in kind)"),
                    noMoney,
                ) { noMoney = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSettle(if (noMoney) null else accountId); onDismiss() }) { Text(tr("Settle")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

@Composable
private fun IouEditor(accounts: List<Account>, onSave: (Iou, Long?) -> Unit, onDismiss: () -> Unit) {
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(IouDirection.THEY_OWE) }
    var note by remember { mutableStateOf("") }
    var moneyMoved by remember { mutableStateOf(true) }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("New IOU")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = direction == IouDirection.THEY_OWE, onClick = { direction = IouDirection.THEY_OWE }, label = { Text(tr("They owe me")) })
                    FilterChip(selected = direction == IouDirection.I_OWE, onClick = { direction = IouDirection.I_OWE }, label = { Text(tr("I owe them")) })
                }
                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it.take(24); error = null },
                    label = { Text(tr("Person")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
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
                    label = { Text(tr("What for? (optional)")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                ToggleRow(
                    if (direction == IouDirection.THEY_OWE) tr("I handed over money now") else tr("I received money now"),
                    tr("Updates the account balance; not counted as spending or income"),
                    moneyMoved,
                ) { moneyMoved = it }
                if (moneyMoved) {
                    SelectField(
                        label = if (direction == IouDirection.THEY_OWE) tr("From account") else tr("Into account"),
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
                error = when {
                    person.isBlank() -> tr("Who is it with?")
                    cents == null || cents <= 0 -> tr("Enter an amount")
                    moneyMoved && accountId == null -> tr("Pick an account")
                    else -> null
                }
                if (error == null && cents != null) {
                    onSave(
                        Iou(person = person.trim(), amount = cents, direction = direction, note = note.trim(), date = Dates.today()),
                        if (moneyMoved) accountId else null,
                    )
                    onDismiss()
                }
            }) { Text(tr("Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}
