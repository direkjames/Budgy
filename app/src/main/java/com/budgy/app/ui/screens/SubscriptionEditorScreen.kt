@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.budgy.app.ui.screens

import com.budgy.app.i18n.tr
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.data.BillingCycle
import com.budgy.app.data.DefaultData
import com.budgy.app.data.Subscription
import com.budgy.app.notify.ReminderWorker
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.CategoryIcons
import com.budgy.app.ui.components.ColorPicker
import com.budgy.app.ui.components.ConfirmDialog
import com.budgy.app.ui.components.DateField
import com.budgy.app.ui.components.SelectField
import com.budgy.app.ui.components.icon
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.util.Dates
import com.budgy.app.util.Money
import com.budgy.app.util.monthlyCost

@Composable
fun SubscriptionEditorScreen(vm: BudgyViewModel, go: BudgyNav, id: Long) {
    val accounts by vm.activeAccounts.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val allPriceChanges by vm.priceChanges.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var existing by remember { mutableStateOf<Subscription?>(null) }
    var loaded by rememberSaveable { mutableStateOf(id < 0) }
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var cycle by rememberSaveable { mutableStateOf(BillingCycle.MONTHLY) }
    var customDays by rememberSaveable { mutableStateOf("30") }
    var nextDate by rememberSaveable { mutableStateOf(Dates.today() + 1) }
    var isTrial by rememberSaveable { mutableStateOf(false) }
    var trialEnd by rememberSaveable { mutableStateOf(Dates.today() + 7) }
    var reminderDays by rememberSaveable { mutableStateOf(1) }
    var accountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var color by rememberSaveable { mutableStateOf(DefaultData.swatches[4]) }
    var note by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(true) }
    var sharedWith by rememberSaveable { mutableStateOf(1) }
    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(id) {
        if (id >= 0) {
            val s = vm.getSubscription(id)
            existing = s
            if (s != null && !loaded) {
                name = s.name
                amount = Money.toInput(s.amount)
                cycle = s.cycle
                customDays = s.customDays.toString()
                nextDate = s.nextBillingDate
                isTrial = s.isTrial
                trialEnd = s.trialEndDate ?: (Dates.today() + 7)
                reminderDays = s.reminderDays
                accountId = s.accountId
                categoryId = s.categoryId
                color = s.color
                note = s.note
                active = s.active
                sharedWith = s.sharedWith
            }
            loaded = true
        }
    }
    // Defaults for new subscriptions
    LaunchedEffect(accounts, categories, loaded) {
        if (!loaded || id >= 0) return@LaunchedEffect
        if (accountId == null) accountId = accounts.firstOrNull()?.id
        if (categoryId == null) categoryId = categories.firstOrNull { it.icon == "subscriptions" }?.id
    }

    val cents = Money.parse(amount)
    val days = customDays.toIntOrNull()?.coerceIn(1, 3650) ?: 30

    fun build(): Subscription? {
        nameError = name.isBlank()
        amountError = cents == null || cents <= 0
        if (nameError || amountError || cents == null) return null
        val base = existing ?: Subscription(name = "", amount = 0, cycle = cycle, nextBillingDate = nextDate, color = color)
        return base.copy(
            name = name.trim(),
            amount = cents,
            cycle = cycle,
            customDays = days,
            nextBillingDate = if (isTrial) maxOf(nextDate, trialEnd) else nextDate,
            isTrial = isTrial,
            trialEndDate = if (isTrial) trialEnd else null,
            reminderDays = reminderDays,
            accountId = accountId,
            categoryId = categoryId,
            color = color,
            note = note.trim(),
            active = active,
            sharedWith = sharedWith,
        )
    }

    fun save() {
        val s = build() ?: return
        vm.saveSubscription(s)
        // Ask for notification permission the first time someone saves a subscription.
        if (settings?.reminders == true && !ReminderWorker.canNotify(context) && Build.VERSION.SDK_INT >= 33) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        ReminderWorker.checkNow(context)
        go.back()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(if (id >= 0) tr("Edit subscription") else tr("New subscription")) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) } },
                actions = {
                    if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, tr("Delete")) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Live cost preview
            if (cents != null && cents > 0) {
                val preview = Subscription(name = "", amount = cents, cycle = cycle, customDays = days, nextBillingDate = 0, color = 0, sharedWith = sharedWith)
                val cur = Budgy.currency
                BudgyCard(padding = 14.dp) {
                    Text(
                        (if (sharedWith > 1) tr("Your share ≈ ") else "≈ ") + tr("{0} a month · {1} a year", Money.format(preview.monthlyCost(), cur), Money.format(preview.monthlyCost() * 12, cur)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40); nameError = false },
                label = { Text(tr("Name")) },
                placeholder = { Text(tr("Netflix, Spotify, Gym…")) },
                isError = nameError,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(12); amountError = false },
                label = { Text(if (isTrial) tr("Price after trial") else tr("Price")) },
                prefix = { Text(Budgy.currency) },
                isError = amountError,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Label(tr("Billing cycle"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BillingCycle.entries.forEach { c ->
                    FilterChip(selected = cycle == c, onClick = { cycle = c }, label = { Text(c.label) })
                }
            }
            if (cycle == BillingCycle.CUSTOM) {
                OutlinedTextField(
                    value = customDays,
                    onValueChange = { customDays = it.filter(Char::isDigit).take(4) },
                    label = { Text(tr("Repeats every (days)")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Free trial
            BudgyCard(padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Free trial"), style = MaterialTheme.typography.titleSmall)
                        Text(
                            tr("Get a loud reminder before it turns into a paid plan"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = isTrial, onCheckedChange = { isTrial = it })
                }
                if (isTrial) {
                    Spacer(Modifier.height(12.dp))
                    DateField(tr("Trial ends on"), trialEnd) { trialEnd = it }
                }
            }

            DateField(if (isTrial) tr("First charge on") else tr("Next billing date"), if (isTrial) maxOf(nextDate, trialEnd) else nextDate) { nextDate = it }

            Label(tr("Remind me"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to tr("Same day"), 1 to tr("1 day before"), 2 to tr("2 days before"), 3 to tr("3 days before")).forEach { (d, l) ->
                    FilterChip(selected = reminderDays == d, onClick = { reminderDays = d }, label = { Text(l) })
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val acct = accounts.firstOrNull { it.id == accountId }
                SelectField(
                    label = tr("Paid from"),
                    value = acct?.name ?: tr("Choose"),
                    options = accounts,
                    optionLabel = { it.name },
                    onSelect = { accountId = it.id },
                    leading = acct?.type?.icon(),
                    modifier = Modifier.weight(1f),
                )
                val expenseCats = categories.filter { it.kind == com.budgy.app.data.CategoryKind.EXPENSE }
                val cat = expenseCats.firstOrNull { it.id == categoryId }
                SelectField(
                    label = tr("Category"),
                    value = cat?.name ?: tr("None"),
                    options = expenseCats,
                    optionLabel = { it.name },
                    onSelect = { categoryId = it.id },
                    leading = cat?.let { CategoryIcons.get(it.icon) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Shared plan
            BudgyCard(padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Shared plan"), style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (sharedWith > 1) tr("Split between {0} people. Totals and \"Mark as paid\" use your share.", sharedWith)
                            else tr("Family or group plan? Split it so only your share counts."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { if (sharedWith > 1) sharedWith-- }, enabled = sharedWith > 1) {
                        Icon(Icons.Rounded.Remove, tr("Fewer people"))
                    }
                    Text("$sharedWith", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { if (sharedWith < 20) sharedWith++ }) { Icon(Icons.Rounded.Add, tr("More people")) }
                }
            }

            Label(tr("Color"))
            ColorPicker(DefaultData.swatches, color) { color = it }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(120) },
                label = { Text(tr("Note (optional)")) },
                placeholder = { Text(tr("Shared with family, cancel in March…")) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            if (existing != null) {
                BudgyCard(padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Active"), style = MaterialTheme.typography.titleSmall)
                            Text(
                                tr("Turn off to pause it without deleting. Paused ones don't count in totals."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            }

            Button(
                onClick = { save() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text(if (existing != null) tr("Save changes") else tr("Add subscription"), style = MaterialTheme.typography.titleMedium) }

            existing?.let { e ->
                // Usage check: helps spot subscriptions worth cancelling
                BudgyCard(padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Last used"), style = MaterialTheme.typography.titleSmall)
                            Text(
                                e.lastUsed?.let { lu ->
                                    val ago = Dates.today() - lu
                                    when {
                                        ago <= 0 -> tr("Today")
                                        ago >= 30 -> tr("{0} days ago. Still worth paying for?", ago)
                                        else -> tr("{0} day{1} ago", ago, if (ago == 1L) "" else "s")
                                    }
                                } ?: tr("Not tracked yet. Tap when you use it to spot ones to cancel."),
                                style = MaterialTheme.typography.bodySmall,
                                color = if ((e.lastUsed?.let { Dates.today() - it } ?: 0L) >= 30L) Budgy.colors.warning
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalButton(onClick = { existing = e.copy(lastUsed = Dates.today()); vm.markUsedToday(e) }) { Text(tr("Used today")) }
                    }
                }
                val history = allPriceChanges.filter { it.subscriptionId == e.id }
                if (history.isNotEmpty()) {
                    BudgyCard(padding = 14.dp) {
                        Text(tr("Price history"), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        history.take(6).forEach { p ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Text(Dates.short(p.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text(
                                    "${Money.format(p.oldAmount, Budgy.currency)} → ${Money.format(p.newAmount, Budgy.currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (p.newAmount > p.oldAmount) Budgy.colors.expense else Budgy.colors.income,
                                )
                            }
                        }
                    }
                }
                if (e.active) {
                    OutlinedButton(
                        onClick = {
                            // markPaid saves the edited subscription too, then logs the payment.
                            build()?.let { s ->
                                vm.markPaid(s)
                                go.back()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text(tr("Mark as paid now")) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmDelete) {
        existing?.let { s ->
            ConfirmDialog(
                title = tr("Delete {0}?", s.name),
                text = tr("Past payments you logged stay in your transactions."),
                onConfirm = { vm.deleteSubscription(s); go.back() },
                onDismiss = { confirmDelete = false },
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 0.dp),
    )
}
