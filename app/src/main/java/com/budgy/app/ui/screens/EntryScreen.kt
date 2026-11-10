@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.budgy.app.ui.screens

import com.budgy.app.i18n.tr
import androidx.compose.foundation.background
import com.budgy.app.util.isForeign
import com.budgy.app.util.toHome
import com.budgy.app.util.symbol
import com.budgy.app.ui.components.joinTags
import com.budgy.app.ui.components.tagList
import com.budgy.app.ui.components.MoneyText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.rounded.Sell
import com.budgy.app.ui.components.BudgyCard
import androidx.compose.material3.Switch
import androidx.compose.material3.InputChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.rounded.Group
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.data.BillingCycle
import com.budgy.app.data.CategoryKind
import com.budgy.app.data.TxType
import com.budgy.app.data.Txn
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.AmountKeypad
import com.budgy.app.ui.components.CategoryIcons
import com.budgy.app.ui.components.ConfirmDialog
import com.budgy.app.ui.components.DatePickerDialogBudgy
import com.budgy.app.ui.components.SelectField
import com.budgy.app.ui.components.applyKey
import com.budgy.app.ui.components.icon
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.ui.theme.Numeric
import com.budgy.app.util.Dates
import com.budgy.app.util.Money

@Composable
fun EntryScreen(vm: BudgyViewModel, go: BudgyNav, id: Long, initialType: String) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val allAccounts by vm.accounts.collectAsStateWithLifecycle()
    val accounts by vm.activeAccounts.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val allTxns by vm.transactions.collectAsStateWithLifecycle()

    var existing by remember { mutableStateOf<Txn?>(null) }
    var loaded by rememberSaveable { mutableStateOf(id < 0) }
    var type by rememberSaveable { mutableStateOf(runCatching { TxType.valueOf(initialType) }.getOrDefault(TxType.EXPENSE)) }
    var amount by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var accountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var toAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var date by rememberSaveable { mutableStateOf(Dates.today()) }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var noteFocused by remember { mutableStateOf(false) }
    var repeat by rememberSaveable { mutableStateOf<BillingCycle?>(null) }
    var split by rememberSaveable { mutableStateOf(false) }
    var people by rememberSaveable { mutableStateOf(listOf<String>()) }
    var personInput by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var tagInput by rememberSaveable { mutableStateOf("") }
    var toAmountText by rememberSaveable { mutableStateOf("") }

    // Load an existing transaction for editing.
    LaunchedEffect(id) {
        if (id >= 0) {
            val t = vm.getTxn(id)
            existing = t
            if (t != null && !loaded) {
                type = t.type
                amount = Money.toInput(t.amount)
                categoryId = t.categoryId
                accountId = t.accountId
                toAccountId = t.toAccountId
                date = t.date
                note = t.note
                tags = t.tags
                toAmountText = t.toAmount?.let { Money.toInput(it) } ?: ""
            }
            loaded = true
        }
    }

    // Sensible defaults for a new entry: last-used account and category.
    val s = settings
    LaunchedEffect(accounts, categories, type, s, loaded) {
        if (!loaded || s == null) return@LaunchedEffect
        if (accountId == null || (id < 0 && accounts.none { it.id == accountId })) {
            accountId = accounts.firstOrNull { it.id == s.lastAccountId }?.id ?: accounts.firstOrNull()?.id
        }
        if (type == TxType.TRANSFER && id < 0 && (toAccountId == null || toAccountId == accountId)) {
            toAccountId = accounts.firstOrNull { it.id != accountId }?.id
        }
        val kind = if (type == TxType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
        val valid = categories.filter { it.kind == kind }
        if (type.hasCategory() && valid.none { it.id == categoryId }) {
            val last = if (type == TxType.INCOME) s.lastIncomeCategoryId else s.lastExpenseCategoryId
            categoryId = valid.firstOrNull { it.id == last }?.id ?: valid.firstOrNull()?.id
        }
    }

    val typeColor = when (type) {
        TxType.EXPENSE -> Budgy.colors.expense
        TxType.INCOME -> Budgy.colors.income
        else -> MaterialTheme.colorScheme.tertiary
    }
    val isNew = id < 0
    val home = s?.currency ?: Budgy.currency
    val fromAcct = allAccounts.firstOrNull { it.id == accountId }
    val toAcct = allAccounts.firstOrNull { it.id == toAccountId }
    val fromSym = fromAcct?.symbol(home) ?: home
    val toSym = toAcct?.symbol(home) ?: home
    val crossCurrency = type == TxType.TRANSFER && fromAcct != null && toAcct != null && fromSym != toSym
    /** Suggested amount the destination receives, using your saved rates. */
    fun convertedTo(cents: Long): Long {
        val inHome = fromAcct?.toHome(cents, home) ?: cents
        val rate = if (toAcct != null && toAcct.isForeign(home)) toAcct.fxRateMicros / 1_000_000.0 else 1.0
        return Math.round(inHome / rate)
    }
    val canSplit = isNew && type == TxType.EXPENSE
    val canRepeat = isNew && !type.isIou && !(canSplit && split)

    fun save() {
        val cents = Money.parse(amount)
        val from = accountId
        error = when {
            cents == null || cents <= 0 -> tr("Enter an amount")
            from == null -> tr("Add an account first (Settings → Accounts)")
            type == TxType.TRANSFER && (toAccountId == null || toAccountId == from) -> tr("Pick two different accounts")
            else -> null
        }
        if (canSplit && split && people.isEmpty()) error = tr("Add at least one person to split with")
        if (error != null || cents == null || from == null) return
        val base = existing ?: Txn(type = type, amount = cents, accountId = from, date = date)
        val txn = base.copy(
            type = type,
            amount = cents,
            accountId = from,
            toAccountId = if (type == TxType.TRANSFER) toAccountId else null,
            categoryId = if (type.hasCategory()) categoryId else null,
            note = note.trim(),
            date = date,
            tags = joinTags(tagList(tags) + tagList(tagInput)),
            toAmount = if (crossCurrency) (Money.parse(toAmountText) ?: convertedTo(cents)) else null,
        )
        if (canSplit && split) {
            val (mine, each) = splitEvenly(cents, people.size)
            vm.saveSplit(txn, mine, people.map { it to each })
        } else {
            vm.saveTxn(txn, if (canRepeat) repeat else null)
        }
        go.back()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(if (id >= 0) tr("Edit transaction") else tr("New transaction")) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.Rounded.Close, tr("Close")) } },
                actions = {
                    if (existing != null && !type.isInvest) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, tr("Delete")) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp)
        ) {
          if (type == TxType.ADJUST) {
            AdjustmentInfo(existing, fromAcct?.name ?: "", fromSym, onDelete = { confirmDelete = true })
          } else if (type.isInvest) {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            InvestmentInfo(existing, fromAcct?.name ?: "", fromSym) {
                val t = existing
                scope.launch {
                    val rec = t?.let { vm.investTxnForLedger(it.id) }
                    go.back()
                    if (rec != null) go.holding(rec.holdingId) else go.investments()
                }
            }
          } else {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val typeOptions = if (type.isIou) listOf(type) else TxType.entryTypes
                typeOptions.forEachIndexed { i, t ->
                    SegmentedButton(
                        selected = type == t,
                        onClick = { type = t; error = null },
                        shape = SegmentedButtonDefaults.itemShape(i, typeOptions.size),
                        label = { Text(t.label) },
                    )
                }
            }

            // Amount display
            Column(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        fromSym,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = typeColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 6.dp, end = 4.dp),
                    )
                    Text(
                        Money.groupTyping(amount),
                        style = MaterialTheme.typography.displayMedium.merge(Numeric),
                        fontWeight = FontWeight.Bold,
                        color = if (amount.isEmpty()) MaterialTheme.colorScheme.outline else typeColor,
                        maxLines = 1,
                    )
                }
                error?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Details (scrolls if the screen is short)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (type.hasCategory()) {
                    val kind = if (type == TxType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
                    Text(tr("Category"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.filter { it.kind == kind }.forEach { c ->
                            val sel = c.id == categoryId
                            val cc = Color(c.color)
                            val shape = RoundedCornerShape(12.dp)
                            Row(
                                Modifier
                                    .clip(shape)
                                    .background(if (sel) cc.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, if (sel) cc else MaterialTheme.colorScheme.outlineVariant, shape)
                                    .clickable { categoryId = c.id }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(CategoryIcons.get(c.icon), null, tint = cc, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    c.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (sel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SelectField(
                        label = if (type == TxType.TRANSFER) tr("From") else tr("Account"),
                        value = fromAcct?.name ?: tr("Choose"),
                        options = accounts,
                        optionLabel = { it.name },
                        onSelect = { accountId = it.id },
                        leading = fromAcct?.type?.icon(),
                        modifier = Modifier.weight(1f),
                    )
                    if (type == TxType.TRANSFER) {
                        SelectField(
                            label = tr("To"),
                            value = toAcct?.name ?: tr("Choose"),
                            options = accounts.filter { it.id != accountId },
                            optionLabel = { it.name },
                            onSelect = { toAccountId = it.id },
                            leading = toAcct?.type?.icon(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (crossCurrency) {
                    val suggested = convertedTo(Money.parse(amount) ?: 0L)
                    OutlinedTextField(
                        value = toAmountText,
                        onValueChange = { toAmountText = it.filter { c -> c.isDigit() || c == '.' }.take(12) },
                        label = { Text(tr("{0} receives", toAcct?.name ?: tr("To"))) },
                        placeholder = { Text(Money.toInput(suggested)) },
                        prefix = { Text(toSym) },
                        supportingText = { Text(tr("Leave empty to use your saved rate ({0})", Money.format(suggested, toSym))) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().onFocusChanged { noteFocused = it.isFocused },
                    )
                }

                // Date chips: Today / Yesterday / pick
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val t = Dates.today()
                    FilterChip(selected = date == t, onClick = { date = t }, label = { Text(tr("Today")) })
                    FilterChip(selected = date == t - 1, onClick = { date = t - 1 }, label = { Text(tr("Yesterday")) })
                    FilterChip(
                        selected = date != t && date != t - 1,
                        onClick = { pickDate = true },
                        label = { Text(if (date != t && date != t - 1) Dates.short(date) else tr("Pick date")) },
                    )
                }

                if (canRepeat) {
                    Text(tr("Repeat"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = repeat == null, onClick = { repeat = null }, label = { Text(tr("Never")) })
                        listOf(BillingCycle.WEEKLY, BillingCycle.MONTHLY, BillingCycle.YEARLY).forEach { c ->
                            FilterChip(selected = repeat == c, onClick = { repeat = c }, label = { Text(c.label) })
                        }
                    }
                    val r = repeat
                    if (r != null) {
                        Text(
                            when (r) {
                                BillingCycle.WEEKLY -> tr("Budgy will log this automatically every week. Manage it in Settings → Recurring.")
                                BillingCycle.YEARLY -> tr("Budgy will log this automatically every year. Manage it in Settings → Recurring.")
                                else -> tr("Budgy will log this automatically every month. Manage it in Settings → Recurring.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (canSplit) {
                    SplitSection(
                        split = split,
                        onSplit = { split = it; error = null },
                        people = people,
                        onPeople = { people = it; error = null },
                        input = personInput,
                        onInput = { personInput = it },
                        total = Money.parse(amount) ?: 0L,
                    )
                }

                TagsSection(
                    tags = tagList(tags),
                    onTags = { tags = joinTags(it) },
                    input = tagInput,
                    onInput = { tagInput = it },
                    suggestions = remember(allTxns) {
                        allTxns.asSequence().flatMap { tagList(it.tags).asSequence() }
                            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }.take(8).toList()
                    },
                    onFocus = { noteFocused = it },
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(80) },
                    placeholder = { Text(tr("Add a note (optional)")) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Notes, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { noteFocused = it.isFocused },
                )
                Spacer(Modifier.height(4.dp))
            }

            if (!noteFocused) {
                AmountKeypad(onKey = { amount = applyKey(amount, it); error = null }, modifier = Modifier.padding(top = 8.dp))
            }
            Button(
                onClick = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 12.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(if (id >= 0) tr("Save changes") else tr("Save {0}", type.label.lowercase()), style = MaterialTheme.typography.titleMedium)
            }
          }
        }
    }

    if (pickDate) {
        DatePickerDialogBudgy(date, onDismiss = { pickDate = false }) { date = it; pickDate = false }
    }
    if (confirmDelete) {
        existing?.let { t ->
            ConfirmDialog(
                title = tr("Delete transaction?"),
                text = tr("This can't be undone. Account balances will update."),
                onConfirm = { vm.deleteTxn(t); go.back() },
                onDismiss = { confirmDelete = false },
            )
        }
    }
}

private fun TxType.hasCategory() = this == TxType.EXPENSE || this == TxType.INCOME

/** Splits [total] across you + [others] people; any leftover cent stays with you. Returns (yours, each). */
fun splitEvenly(total: Long, others: Int): Pair<Long, Long> {
    val each = total / (others + 1)
    return (total - each * others) to each
}

@Composable
private fun SplitSection(
    split: Boolean,
    onSplit: (Boolean) -> Unit,
    people: List<String>,
    onPeople: (List<String>) -> Unit,
    input: String,
    onInput: (String) -> Unit,
    total: Long,
) {
    val cur = Budgy.currency
    BudgyCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Group, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Split with friends"), style = MaterialTheme.typography.titleSmall)
                Text(
                    tr("You paid the whole bill; only your share counts as spending"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = split, onCheckedChange = onSplit)
        }
        if (split) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { onInput(it.take(24)) },
                    placeholder = { Text(tr("Name")) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (input.isNotBlank()) { onPeople(people + input.trim()); onInput("") }
                    }),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = {
                    if (input.isNotBlank()) { onPeople(people + input.trim()); onInput("") }
                }) { Text(tr("Add")) }
            }
            if (people.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    people.forEachIndexed { i, p ->
                        InputChip(
                            selected = false,
                            onClick = { onPeople(people.filterIndexed { j, _ -> j != i }) },
                            label = { Text(p) },
                            trailingIcon = { Icon(Icons.Rounded.Close, tr("Remove"), Modifier.size(16.dp)) },
                        )
                    }
                }
                if (total > 0) {
                    val (mine, each) = splitEvenly(total, people.size)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        tr("Your share {0} · each friend owes {1}", Money.format(mine, cur), Money.format(each, cur)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustmentInfo(t: Txn?, accountName: String, symbol: String, onDelete: () -> Unit) {
    BudgyCard(Modifier.padding(top = 8.dp)) {
        Text(tr("Balance adjustment"), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        if (t != null) {
            MoneyText(t.amount, style = MaterialTheme.typography.headlineMedium, showPlus = true, symbol = symbol)
            Text(
                "$accountName · ${Dates.dayLabel(t.date)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            tr("This corrected the account to match your real balance. It doesn't count as income or spending. ") +
                tr("Delete it to undo the correction, or make a new adjustment from Settings → Accounts."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Delete adjustment"), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun InvestmentInfo(t: Txn?, accountName: String, symbol: String, onOpen: () -> Unit) {
    BudgyCard(Modifier.padding(top = 8.dp)) {
        Text(t?.type?.label ?: "", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        if (t != null) {
            MoneyText(if (t.type == TxType.INVEST) -t.amount else t.amount, style = MaterialTheme.typography.headlineMedium, showPlus = true, symbol = symbol)
            Text(
                "$accountName · ${Dates.dayLabel(t.date)}" + if (t.note.isNotBlank()) " · ${t.note}" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            tr("This was recorded in Investments. It moves money between your account and the investment, so it isn't spending or income. Edit or delete it there so your holdings stay correct."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text(tr("Open in Investments")) }
    }
}

@Composable
private fun TagsSection(
    tags: List<String>,
    onTags: (List<String>) -> Unit,
    input: String,
    onInput: (String) -> Unit,
    suggestions: List<String>,
    onFocus: (Boolean) -> Unit,
) {
    fun add(t: String) {
        val v = t.trim().removePrefix("#")
        if (v.isNotEmpty() && tags.none { it.equals(v, true) }) onTags(tags + v)
        onInput("")
    }
    Text(tr("Tags"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (tags.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tags.forEach { t ->
                InputChip(
                    selected = true,
                    onClick = { onTags(tags - t) },
                    label = { Text("#$t") },
                    trailingIcon = { Icon(Icons.Rounded.Close, tr("Remove tag"), Modifier.size(16.dp)) },
                )
            }
        }
    }
    OutlinedTextField(
        value = input,
        onValueChange = { v -> if (v.endsWith(",")) add(v.dropLast(1)) else onInput(v.take(30)) },
        placeholder = { Text(tr("Trip or event, e.g. Baguio trip")) },
        leadingIcon = { Icon(Icons.Rounded.Sell, null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { add(input) }),
        modifier = Modifier.fillMaxWidth().onFocusChanged { onFocus(it.isFocused) },
    )
    val left = suggestions.filter { s -> tags.none { it.equals(s, true) } }
    if (left.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            left.forEach { s -> FilterChip(selected = false, onClick = { add(s) }, label = { Text("#$s") }) }
        }
    }
}
