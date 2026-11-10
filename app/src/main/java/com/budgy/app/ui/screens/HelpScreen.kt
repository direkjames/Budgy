@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgy.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgy.app.i18n.tr
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.BudgyLogo
import com.budgy.app.ui.components.IconBadge

/** One topic in the in-app guide. Text is English and translated when shown. */
private data class HelpSection(val title: String, val icon: ImageVector, val color: Long, val items: List<Pair<String, String>>)

private val guide = listOf(
    HelpSection(
        "Getting started", Icons.Rounded.Flag, 0xFF0E6E52, listOf(
            "Set up your accounts" to "Go to Settings → Accounts and add every place you keep money: cash, bank, e-wallet and credit cards. Enter each one's current balance. For a card you owe on, type a minus sign.",
            "Language, currency and dark mode" to "Settings → Language switches between English, Filipino and Cebuano. Settings → Currency changes the money symbol. The moon icon on Home turns on dark mode.",
            "Accounts in other currencies" to "When adding an account, pick its currency and set the rate (for example, 1 $ = ₱56). Totals stay in your main currency. Update the rate when it changes.",
        )
    ),
    HelpSection(
        "Adding transactions", Icons.Rounded.Add, 0xFFDF4A43, listOf(
            "Add an expense or income" to "Tap Add, choose Expense, Income or Transfer, type the amount, pick a category and save. Budgy remembers your last account and category.",
            "Shortcuts" to "Long-press the Budgy icon on your home screen for Add expense, Add income or Transfer. The home screen widget has an Add button too.",
            "Transfers" to "Moving money between your own accounts, like cashing in to GCash, is a Transfer. It doesn't count as spending.",
            "Repeat" to "When adding, pick Repeat (weekly, monthly or yearly) and Budgy logs it automatically from then on.",
            "Split a bill" to "Turn on Split with friends and add their names. Only your share counts as spending, and each friend gets an IOU.",
            "Tags" to "Add tags like Baguio trip to group spending across categories. See the totals in Settings → Tags.",
            "Edit, delete and search" to "Tap any transaction in Activity to edit or delete it. The search icon finds notes, #tags, categories and amounts. Switch to Calendar to see spending by day.",
        )
    ),
    HelpSection(
        "Budgets", Icons.Rounded.DonutLarge, 0xFF3E8EDE, listOf(
            "Set a budget" to "Budgets tab → Add. Pick a category and a limit. Budgy shows what's left and how much you can spend per day.",
            "What the colors mean" to "Green is under 80%, amber is 80–99%, and red is over the limit. You get a notification at 80% and at 100%.",
            "Start on payday" to "Settings → Budget period lets budgets start on your payday, or run twice a month on the 15th and 30th.",
            "Envelope mode" to "Turn it on in Settings. Unspent money rolls over to the next period, and you can move money between envelopes.",
        )
    ),
    HelpSection(
        "Subscriptions", Icons.Rounded.Autorenew, 0xFF5B6CF0, listOf(
            "Track a subscription" to "Subscriptions tab → Add. Enter the price, how often it bills and the next date. Budgy reminds you before you're charged.",
            "Free trials" to "Turn on Free trial and set when it ends. You'll get a reminder before it turns into a paid plan.",
            "Mark as paid" to "When a bill is near or overdue, tap Mark as paid. It logs the expense and moves to the next billing date.",
            "Shared plans and usage" to "Use Shared plan for family plans so only your share counts. Tap Used today now and then to spot subscriptions you don't use.",
        )
    ),
    HelpSection(
        "Planning tools", Icons.Rounded.Savings, 0xFFE5A50A, listOf(
            "Recurring" to "Settings → Recurring for rent, salary or allowance. Log it automatically, or confirm each time from Home.",
            "Savings goals" to "Set a target and a date, and Budgy tells you how much to save each week. Use Add money and Withdraw as you go.",
            "IOUs" to "Track who owes who. Settle up when it's paid back. It doesn't count as income or spending.",
            "Debt payoff" to "Add your cards and loans with their interest rate and minimum payment. Compare snowball (smallest balance first) and avalanche (highest interest first).",
        )
    ),
    HelpSection(
        "Investments", Icons.AutoMirrored.Rounded.ShowChart, 0xFF2BA3A0, listOf(
            "Add an investment" to "Settings → Investments → +. Pick Crypto, Pag-IBIG MP2 or Fund / ETF. For crypto, search CoinGecko for the coin, or choose Manual price for a coin it doesn't list.",
            "Funds and ETFs" to "For an S&P 500 ETF like VOO, an index fund or a UITF, pick Fund / ETF. Type the price in your currency or in US dollars with your exchange rate, and update it whenever you check your broker.",
            "Buy, sell and earnings" to "Open a holding and tap Buy, Sell or Earned. Pick an account and the money moves out of it or into it. Buying and selling don't count as spending or income.",
            "Gain and loss" to "Budgy compares what a holding is worth now with what you paid for it (average cost). Selling part of it shows your realized gain or loss.",
            "Pag-IBIG MP2" to "Add each savings and the yearly dividend. Budgy shows your total, your dividends, and a rough estimate of the value at maturity.",
            "Prices" to "Live coins update when you open Investments, or tap refresh. For manual coins, open the coin and tap Update price.",
            "Why Budgy uses the internet" to "Only for live crypto prices from CoinGecko. It sends coin names like bitcoin, never your amounts, accounts or transactions. Turn it off in Settings → Live crypto prices.",
        )
    ),
    HelpSection(
        "Seeing where you stand", Icons.AutoMirrored.Rounded.TrendingUp, 0xFF12A15F, listOf(
            "Home" to "Your balance, budget, streak, alerts, upcoming bills and recent transactions in one place.",
            "Reports" to "The chart icon compares this month with last month and shows spending by day and top categories. Year in review sums up your whole year.",
            "Net worth" to "Tap the green balance card to see what you own, including investments, minus what you owe, over the last 12 months.",
            "Streaks and badges" to "A no-spend day is a day with no expenses. Keep your streak going and unlock badges in Settings → Badges.",
            "Weekly summary" to "Every Sunday evening Budgy sends a short recap. Turn it off in Settings → Reminders.",
        )
    ),
    HelpSection(
        "Your data and privacy", Icons.Rounded.Lock, 0xFF8E6CEF, listOf(
            "Back up" to "Your data stays on this phone. Use Settings → Back up and save the file to Google Drive. Restore it on a new phone.",
            "CSV files" to "Export to CSV to open your transactions in Excel or Sheets. Import from CSV to add transactions from a bank or e-wallet statement.",
            "Fix a balance" to "If an account doesn't match your real balance, open Accounts and tap the tune icon. Budgy adds an adjustment that isn't income or spending.",
            "Archive an account" to "Not using an account anymore? Open it and tap Archive. It's hidden from pickers but keeps its history.",
            "App lock" to "Settings → App lock asks for your fingerprint or screen lock when opening Budgy.",
        )
    ),
)

@Composable
fun HelpScreen(vm: BudgyViewModel, go: BudgyNav) {
    var open by rememberSaveable { mutableIntStateOf(0) }
    // Opening the guide counts as having seen it, so the welcome card on Home goes away.
    LaunchedEffect(Unit) { vm.setHelpSeen() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tr("How to use Budgy")) },
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BudgyLogo(48.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            tr("Everything Budgy can do, in a few minutes. Tap a topic to open it."),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            itemsIndexed(guide) { i, section ->
                val expanded = open == i
                BudgyCard(padding = 0.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { open = if (expanded) -1 else i }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(section.icon, Color(section.color), 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr(section.title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                tr("{0} tips", section.items.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                    }
                    AnimatedVisibility(expanded) {
                        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                            section.items.forEachIndexed { j, (heading, body) ->
                                if (j > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                Text(tr(heading), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(tr(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    tr("Your data stays on this phone. Back up now and then."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
    }
}
