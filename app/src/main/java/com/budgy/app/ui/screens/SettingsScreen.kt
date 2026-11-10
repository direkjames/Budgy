@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgy.app.ui.screens

import android.Manifest
import android.os.Build
import com.budgy.app.util.PeriodSpec
import com.budgy.app.util.PeriodMode
import com.budgy.app.notify.WeeklySummaryWorker
import com.budgy.app.i18n.tr
import com.budgy.app.i18n.L10n
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Inventory2
import com.budgy.app.ui.findActivity
import com.budgy.app.ui.AppLock
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Autorenew
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.notify.ReminderWorker
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.BudgyLogo
import com.budgy.app.ui.components.IconBadge
import com.budgy.app.util.Money

@Composable
fun SettingsScreen(vm: BudgyViewModel, go: BudgyNav) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    val context = LocalContext.current
    var pickCurrency by remember { mutableStateOf(false) }
    var pickPeriod by remember { mutableStateOf(false) }
    var pickLanguage by remember { mutableStateOf(false) }
    var showInternetInfo by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    val today = java.time.LocalDate.now().toString()

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.exportBackup(it) }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        restoreUri = uri
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { vm.exportCsv(it) }
    }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    val csvImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importUri = uri
    }
    val accent = MaterialTheme.colorScheme.primary

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            vm.setReminders(true)
            ReminderWorker.checkNow(context)
        } else {
            vm.toast(tr("Notifications are blocked. Turn them on in Android Settings → Apps → Budgy."))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tr("Settings")) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Group(tr("Help")) {
                SettingRow(Icons.AutoMirrored.Rounded.MenuBook, tr("How to use Budgy"), tr("Learn every feature in a few minutes"), accent, onClick = go::help) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
            }

            Group(tr("Appearance")) {
                SettingRow(Icons.Rounded.DarkMode, tr("Dark mode"), tr("Light is the default"), accent) {
                    Switch(checked = s.darkMode, onCheckedChange = { vm.setDarkMode(it) })
                }
                Divider()
                SettingRow(Icons.Rounded.Translate, tr("Language"), L10n.languages.firstOrNull { it.first == s.language }?.second ?: "English", accent,
                    onClick = { pickLanguage = true }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
            }

            Group(tr("Money")) {
                val name = Money.currencies.firstOrNull { it.first == s.currency }?.second ?: s.currency
                SettingRow(Icons.Rounded.Paid, tr("Currency"), "${s.currency}  ·  $name", accent, onClick = { pickCurrency = true }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
                Divider()
                SettingRow(Icons.Rounded.AccountBalanceWallet, tr("Accounts"), tr("Cash, bank, e-wallets, cards"), accent, onClick = go::accounts) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
                Divider()
                SettingRow(Icons.Rounded.Category, tr("Categories"), tr("Add, rename, recolor"), accent, onClick = go::categories) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
                Divider()
                SettingRow(Icons.Rounded.Event, tr("Budget period"), periodSummary(s.period), accent, onClick = { pickPeriod = true }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
                Divider()
                SettingRow(Icons.Rounded.Inventory2, tr("Envelope budgeting"), tr("Unspent budget rolls over; move money between envelopes"), accent) {
                    Switch(checked = s.envelopeMode, onCheckedChange = { vm.setEnvelopeMode(it) })
                }
                Divider()
                SettingRow(Icons.Rounded.Sell, tr("Tags"), tr("Totals for trips and events"), accent, onClick = go::tags) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
            }

            Group(tr("Planning")) {
                SettingRow(Icons.Rounded.Autorenew, tr("Recurring"), tr("Rent, salary, allowance on repeat"), accent, onClick = go::recurring) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
                Divider()
                SettingRow(Icons.Rounded.Savings, tr("Savings goals"), tr("Targets with a weekly plan"), accent, onClick = go::goals) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
                Divider()
                SettingRow(Icons.Rounded.Group, tr("IOUs"), tr("Split bills and who owes who"), accent, onClick = go::ious) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
                Divider()
                SettingRow(Icons.Rounded.BarChart, tr("Monthly report"), tr("Compare months, top categories"), accent, onClick = go::reports) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
                Divider()
                SettingRow(Icons.Rounded.CreditCard, tr("Debt payoff planner"), tr("Snowball vs. avalanche"), accent, onClick = go::debts) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
                Divider()
                SettingRow(Icons.AutoMirrored.Rounded.ShowChart, tr("Investments"), tr("Crypto, funds and Pag-IBIG MP2"), accent, onClick = go::investments) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
                Divider()
                SettingRow(Icons.AutoMirrored.Rounded.TrendingUp, tr("Net worth"), tr("What you own minus what you owe, over time"), accent, onClick = go::networth) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
                Divider()
                SettingRow(Icons.Rounded.AutoAwesome, tr("Year in review"), tr("Your year in numbers"), accent, onClick = go::year) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
                Divider()
                SettingRow(Icons.Rounded.EmojiEvents, tr("Badges"), tr("Streaks, goals and milestones"), accent, onClick = go::badges) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
            }

            Group(tr("Reminders")) {
                SettingRow(Icons.Rounded.NotificationsActive, tr("Bill & trial reminders"), tr("Daily check around 9 AM"), accent) {
                    Switch(checked = s.reminders, onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= 33 && !ReminderWorker.canNotify(context)) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            vm.setReminders(on)
                        }
                    })
                }
                Divider()
                SettingRow(
                    Icons.AutoMirrored.Rounded.Send, tr("Send a test reminder"), tr("Shows what's due in the next 7 days right now"), accent,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33 && !ReminderWorker.canNotify(context)) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            ReminderWorker.sendTest(context)
                            vm.toast(tr("Test reminder sent"))
                        }
                    },
                ) {}
                Divider()
                SettingRow(Icons.Rounded.Warning, tr("Budget alerts"), tr("When a budget reaches 80% and 100%"), accent) {
                    Switch(checked = s.budgetAlerts, onCheckedChange = { vm.setBudgetAlerts(it) })
                }
                Divider()
                SettingRow(Icons.Rounded.Insights, tr("Spending alerts"), tr("When a category runs much higher than usual"), accent) {
                    Switch(checked = s.smartAlerts, onCheckedChange = { vm.setSmartAlerts(it) })
                }
                Divider()
                SettingRow(Icons.Rounded.Summarize, tr("Weekly summary"), tr("Sunday evening recap"), accent) {
                    Switch(checked = s.weeklySummary, onCheckedChange = { vm.setWeeklySummary(it) })
                }
                Divider()
                SettingRow(
                    Icons.AutoMirrored.Rounded.Send, tr("Send this week's summary now"), tr("See what the Sunday recap looks like"), accent,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33 && !ReminderWorker.canNotify(context)) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            WeeklySummaryWorker.sendNow(context)
                            vm.toast(tr("Summary sent"))
                        }
                    },
                ) {}
            }

            Group(tr("Internet")) {
                SettingRow(Icons.Rounded.Public, tr("Live crypto prices"), tr("Only coin names are sent to CoinGecko"), accent) {
                    Switch(checked = s.canUseInternet, onCheckedChange = { vm.setLivePrices(it) })
                }
                Divider()
                SettingRow(Icons.Rounded.Info, tr("Why Budgy uses the internet"), tr("What's sent, and what never leaves your phone"), accent,
                    onClick = { showInternetInfo = true }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                }
            }

            Group(tr("Security")) {
                SettingRow(Icons.Rounded.Fingerprint, tr("App lock"), tr("Ask for fingerprint or screen lock when opening Budgy"), accent) {
                    Switch(checked = s.appLock, onCheckedChange = { on ->
                        val activity = context.findActivity()
                        when {
                            activity == null -> Unit
                            !AppLock.isAvailable(context) ->
                                vm.toast(tr("Set up a screen lock or fingerprint in your phone's settings first."))
                            else -> AppLock.prompt(
                                activity,
                                if (on) tr("Turn on app lock") else tr("Turn off app lock"),
                                onSuccess = { vm.setAppLock(on) },
                            )
                        }
                    })
                }
            }

            Group(tr("Your data")) {
                SettingRow(Icons.Rounded.CloudUpload, tr("Back up"), tr("Save everything to a file (Drive, Downloads…)"), accent,
                    onClick = { backupLauncher.launch("budgy-backup-$today.json") }) {}
                Divider()
                SettingRow(Icons.Rounded.Restore, tr("Restore from backup"), tr("Replaces all data on this phone"), accent,
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }) {}
                Divider()
                SettingRow(Icons.Rounded.TableChart, tr("Export to CSV"), tr("Open your transactions in Excel or Sheets"), accent,
                    onClick = { csvLauncher.launch("budgy-transactions-$today.csv") }) {}
                Divider()
                SettingRow(Icons.Rounded.FileUpload, tr("Import from CSV"), tr("From a Budgy export or your bank/e-wallet statement"), accent,
                    onClick = { csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.ms-excel", "*/*")) }) {}
            }

            BudgyCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BudgyLogo(44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(tr("Budgy 4.0"), style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, null, Modifier.padding(end = 4.dp).size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                tr("Your data stays on this phone. Back up now and then."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    importUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { importUri = null },
            title = { Text(tr("Import this CSV?")) },
            text = {
                Text(
                    tr("Transactions are added to what you already have. Rows already in Budgy are skipped. ") +
                        tr("Accounts and categories that don't exist yet are created. Needs at least a Date and an Amount ") +
                        tr("(or Debit/Credit) column.")
                )
            },
            confirmButton = { TextButton(onClick = { vm.importCsv(uri); importUri = null }) { Text(tr("Import")) } },
            dismissButton = { TextButton(onClick = { importUri = null }) { Text(tr("Cancel")) } },
        )
    }

    restoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text(tr("Restore this backup?")) },
            text = { Text(tr("Everything currently in Budgy on this phone will be replaced by the backup. This can't be undone.")) },
            confirmButton = {
                TextButton(onClick = { vm.importBackup(uri); restoreUri = null }) {
                    Text(tr("Replace my data"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { restoreUri = null }) { Text(tr("Cancel")) } },
        )
    }

    if (showInternetInfo) {
        AlertDialog(
            onDismissRequest = { showInternetInfo = false },
            title = { Text(tr("Why Budgy uses the internet")) },
            text = { Text(internetNoticeBody()) },
            confirmButton = { TextButton(onClick = { showInternetInfo = false }) { Text(tr("Close")) } },
        )
    }
    if (pickLanguage) {
        AlertDialog(
            onDismissRequest = { pickLanguage = false },
            title = { Text(tr("Language")) },
            text = {
                Column {
                    L10n.languages.forEach { (code, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.setLanguage(code); pickLanguage = false }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = s.language == code, onClick = { vm.setLanguage(code); pickLanguage = false })
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickLanguage = false }) { Text(tr("Close")) } },
        )
    }

    if (pickPeriod) {
        PeriodDialog(s.period, onSave = { mode, day -> vm.setPeriod(mode, day) }, onDismiss = { pickPeriod = false })
    }

    if (pickCurrency) {
        AlertDialog(
            onDismissRequest = { pickCurrency = false },
            title = { Text(tr("Currency")) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Money.currencies.forEach { (sym, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.setCurrency(sym); pickCurrency = false }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = s.currency == sym, onClick = { vm.setCurrency(sym); pickCurrency = false })
                            Text("$sym  $label")
                        }
                    }
                    Text(
                        tr("This only changes the symbol shown. Amounts aren't converted."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { pickCurrency = false }) { Text(tr("Close")) } },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
        )
        BudgyCard(padding = 4.dp) { content() }
    }
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint, 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

private fun periodSummary(p: PeriodSpec): String = when {
    p.mode == PeriodMode.TWICE -> tr("Twice a month (15th and 30th)")
    p.startDay <= 1 -> tr("Monthly, from the 1st")
    else -> tr("Monthly, from day {0}", p.startDay)
}

@Composable
private fun PeriodDialog(current: PeriodSpec, onSave: (String, Int) -> Unit, onDismiss: () -> Unit) {
    var mode by remember { mutableStateOf(current.mode) }
    var day by remember { mutableStateOf(current.startDay.coerceIn(1, 28)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Budget period")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    tr("Start budgets on payday. Each budget's limit applies to every period."),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { mode = PeriodMode.MONTHLY }) {
                    RadioButton(selected = mode == PeriodMode.MONTHLY, onClick = { mode = PeriodMode.MONTHLY })
                    Text(tr("Once a month"))
                }
                if (mode == PeriodMode.MONTHLY) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                        Text(tr("Starts on day"), modifier = Modifier.weight(1f))
                        IconButton(onClick = { if (day > 1) day-- }, enabled = day > 1) { Icon(Icons.Rounded.Remove, tr("Earlier")) }
                        Text("$day", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { if (day < 28) day++ }, enabled = day < 28) { Icon(Icons.Rounded.Add, tr("Later")) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { mode = PeriodMode.TWICE }) {
                    RadioButton(selected = mode == PeriodMode.TWICE, onClick = { mode = PeriodMode.TWICE })
                    Column {
                        Text(tr("Twice a month"))
                        Text(tr("15th and 30th (end of month in February)"), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (mode != current.mode) {
                    Text(
                        tr("Tip: switching between monthly and twice-a-month doesn't change your limits. Halve or double them in Budgets if needed."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(mode.name, day); onDismiss() }) { Text(tr("Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}
