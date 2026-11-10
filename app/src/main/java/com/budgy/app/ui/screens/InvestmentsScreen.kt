@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgy.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CurrencyBitcoin
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.data.Account
import com.budgy.app.data.AssetKind
import com.budgy.app.data.DefaultData
import com.budgy.app.data.Holding
import com.budgy.app.data.isLive
import com.budgy.app.data.InvestTxn
import com.budgy.app.data.InvestType
import com.budgy.app.i18n.tr
import com.budgy.app.ui.BudgyNav
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyCard
import com.budgy.app.ui.components.ColorPicker
import com.budgy.app.ui.components.ConfirmDialog
import com.budgy.app.ui.components.DateField
import com.budgy.app.ui.components.EmptyState
import com.budgy.app.ui.components.IconBadge
import com.budgy.app.ui.components.MoneyText
import com.budgy.app.ui.components.SectionHeader
import com.budgy.app.ui.components.SelectField
import com.budgy.app.ui.theme.Budgy
import com.budgy.app.ui.theme.Numeric
import com.budgy.app.util.CoinGecko
import com.budgy.app.util.Dates
import com.budgy.app.util.Money
import com.budgy.app.util.Position
import com.budgy.app.util.bpsText
import com.budgy.app.util.formatPct
import com.budgy.app.util.formatPrice
import com.budgy.app.util.formatUnits
import com.budgy.app.util.mp2Maturity
import com.budgy.app.util.mp2Projection
import com.budgy.app.util.parsePrice
import com.budgy.app.util.parseRateBps
import com.budgy.app.util.parseUnits
import com.budgy.app.util.position
import com.budgy.app.util.unitsInput
import com.budgy.app.util.hasForeignPrice
import com.budgy.app.util.fxRate
import com.budgy.app.util.parseRate
import androidx.compose.material.icons.rounded.PieChart
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToLong

/** Pag-IBIG's declared MP2 dividend for 2025, used only as a starting estimate you can change. */
private const val MP2_DEFAULT_RATE = "7.12"

fun holdingIcon(h: Holding): ImageVector = when (h.kind) {
    AssetKind.CRYPTO -> Icons.Rounded.CurrencyBitcoin
    AssetKind.MP2 -> Icons.Rounded.AccountBalance
    AssetKind.FUND -> Icons.Rounded.PieChart
}

/** "just now", "5 min ago", "3 h ago", or a date. */
private fun ago(millis: Long): String {
    if (millis <= 0) return tr("never")
    val mins = (System.currentTimeMillis() - millis) / 60_000
    return when {
        mins < 1 -> tr("just now")
        mins < 60 -> tr("{0} min ago", mins)
        mins < 24 * 60 -> tr("{0} h ago", mins / 60)
        else -> Dates.short(java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay())
    }
}

private fun hint(text: String?): (@Composable () -> Unit)? =
    if (text == null) null else { { Text(text) } }

private fun monthYear(epochDay: Long): String = Dates.month(YearMonth.from(LocalDate.ofEpochDay(epochDay)))

/** "$612.40" for a fund priced in dollars, otherwise the main-currency price. */
private fun priceLabel(h: Holding, home: String): String =
    if (h.hasForeignPrice(home)) formatPrice(h.quotePrice, h.priceCurrency) else formatPrice(h.price, home)

@Composable
private fun gainColor(v: Long): Color = when {
    v > 0 -> Budgy.colors.income
    v < 0 -> Budgy.colors.expense
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun GainText(gain: Long, pct: Double?, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium) {
    val cur = Budgy.currency
    Text(
        Money.format(gain, cur, showPlus = true) + (pct?.let { "  (" + formatPct(it) + ")" } ?: ""),
        style = style.merge(Numeric),
        color = gainColor(gain),
    )
}

/** Why Budgy now goes online, shown before anything is sent. */
@Composable
fun InternetNoticeCard(onLive: () -> Unit, onManual: () -> Unit) {
    BudgyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Public, MaterialTheme.colorScheme.primary, 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(tr("Why Budgy uses the internet"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text(internetNoticeBody(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onManual) { Text(tr("Manual prices only")) }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onLive) { Text(tr("Use live prices")) }
        }
    }
}

fun internetNoticeBody(): String =
    tr("Budgy only goes online to get crypto prices from CoinGecko (coingecko.com). It sends the names of the coins you track, like \"bitcoin\", and your currency. Your amounts, accounts and transactions never leave your phone.") +
        "\n\n" + tr("Pag-IBIG MP2 and coins with a manual price work offline. You can change this anytime in Settings → Live crypto prices.")

// ====================================================================================
// Portfolio
// ====================================================================================

@Composable
fun InvestmentsScreen(vm: BudgyViewModel, go: BudgyNav) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val portfolio by vm.portfolio.collectAsStateWithLifecycle()
    val status by vm.priceStatus.collectAsStateWithLifecycle()
    val s = settings
    val cur = Budgy.currency
    val hasLive = portfolio.positions.any { it.holding.isLive }
    LaunchedEffect(Unit) { vm.refreshPrices() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tr("Investments")) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) } },
                actions = {
                    if (s?.canUseInternet == true && hasLive) {
                        IconButton(onClick = { vm.refreshPrices(force = true) }, enabled = !status.loading) {
                            if (status.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Rounded.Refresh, tr("Refresh prices"))
                        }
                    }
                    IconButton(onClick = { go.editHolding() }) { Icon(Icons.Rounded.Add, tr("Add investment")) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (s != null && !s.internetNoticeSeen) {
                item { InternetNoticeCard(onLive = { vm.setLivePrices(true) }, onManual = { vm.setLivePrices(false) }) }
            }
            if (portfolio.positions.isEmpty()) {
                item {
                    BudgyCard {
                        EmptyState(
                            title = tr("Track your investments"),
                            body = tr("Add crypto like Bitcoin, an S&P 500 fund or ETF, or your Pag-IBIG MP2 savings. Budgy shows what they're worth and how much you've gained."),
                            action = tr("Add investment"),
                            onAction = { go.editHolding() },
                        )
                    }
                }
            } else {
                item {
                    BudgyCard {
                        Text(tr("Total value"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(portfolio.value, style = MaterialTheme.typography.headlineLarge)
                        GainText(portfolio.gain, portfolio.gainPct, MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(10.dp))
                        StatRow(tr("Invested"), Money.format(portfolio.cost, cur))
                        if (portfolio.realized != 0L) {
                            StatRow(tr("Realized from sales"), Money.format(portfolio.realized, cur, showPlus = true), gainColor(portfolio.realized))
                        }
                    }
                }
                if (hasLive || status.error != null) {
                    item {
                        val newest = portfolio.positions.filter { it.holding.isLive }.maxOfOrNull { it.holding.priceUpdated } ?: 0L
                        val text = when {
                            status.loading -> tr("Updating prices…")
                            status.error != null -> status.error!!
                            s?.canUseInternet == true -> tr("Prices by CoinGecko · updated {0}", ago(newest))
                            else -> tr("Live prices are off. Values use the last price Budgy got.")
                        }
                        Text(
                            text,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                items(portfolio.positions, key = { it.holding.id }) { p ->
                    HoldingCard(p) { go.holding(p.holding.id) }
                }
                item {
                    Text(
                        tr("Budgy is a tracker, not financial advice. Prices change quickly, so values are estimates."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.merge(Numeric), color = color, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
private fun HoldingCard(p: Position, onClick: () -> Unit) {
    val h = p.holding
    val cur = Budgy.currency
    BudgyCard(onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(holdingIcon(h), Color(h.color), 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(h.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        h.kind == AssetKind.MP2 -> h.maturityDate?.let { tr("Matures {0}", monthYear(it)) } ?: tr("Pag-IBIG MP2")
                        !p.hasPrice -> formatUnits(p.units) + " " + h.symbol + " · " + tr("no price yet")
                        else -> formatUnits(p.units) + " " + h.symbol + " · " + priceLabel(h, cur)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(p.value, style = MaterialTheme.typography.titleSmall)
                if (p.cost > 0 || p.gain != 0L) {
                    Text(
                        Money.compact(p.gain, cur).let { if (p.gain > 0) "+$it" else it } + (p.gainPct?.let { " · " + formatPct(it) } ?: ""),
                        style = MaterialTheme.typography.labelMedium.merge(Numeric),
                        color = gainColor(p.gain),
                    )
                }
            }
        }
    }
}

// ====================================================================================
// One holding
// ====================================================================================

private fun InvestType.labelFor(kind: AssetKind): String = when (kind) {
    AssetKind.CRYPTO -> when (this) {
        InvestType.BUY -> tr("Buy")
        InvestType.SELL -> tr("Sell")
        InvestType.EARN -> tr("Earned")
    }
    AssetKind.FUND -> when (this) {
        InvestType.BUY -> tr("Buy")
        InvestType.SELL -> tr("Sell")
        InvestType.EARN -> tr("Dividend")
    }
    AssetKind.MP2 -> when (this) {
        InvestType.BUY -> tr("Savings")
        InvestType.SELL -> tr("Withdrawal")
        InvestType.EARN -> tr("Dividend")
    }
}

@Composable
fun HoldingScreen(vm: BudgyViewModel, go: BudgyNav, id: Long) {
    val holdings by vm.holdings.collectAsStateWithLifecycle()
    val txns by vm.investTxns.collectAsStateWithLifecycle()
    val accounts by vm.activeAccounts.collectAsStateWithLifecycle()
    val allAccounts by vm.accounts.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.priceStatus.collectAsStateWithLifecycle()
    val h = holdings.firstOrNull { it.id == id }
    var action by remember { mutableStateOf<InvestType?>(null) }
    var editPrice by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<InvestTxn?>(null) }
    val cur = Budgy.currency
    LaunchedEffect(Unit) { vm.refreshPrices() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(h?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) } },
                actions = {
                    if (h != null) {
                        IconButton(onClick = { go.editHolding(h.id) }) { Icon(Icons.Rounded.Edit, tr("Edit")) }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, tr("Delete")) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        if (h == null) return@Scaffold
        val pos = remember(h, txns) { position(h, txns) }
        val history = remember(txns, id) { txns.filter { it.holdingId == id } }
        val isMp2 = h.kind == AssetKind.MP2
        val acctMap = remember(allAccounts) { allAccounts.associateBy { it.id } }

        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                BudgyCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(holdingIcon(h), Color(h.color), 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("Current value"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MoneyText(pos.value, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    GainText(pos.gain, pos.gainPct)
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                    if (isMp2) {
                        StatRow(tr("Total saved"), Money.format(pos.cost, cur))
                        StatRow(tr("Dividends earned"), Money.format(pos.earned, cur), if (pos.earned > 0) Budgy.colors.income else Color.Unspecified)
                        if (h.rateBps > 0) StatRow(tr("Expected dividend rate"), bpsText(h.rateBps) + "%")
                        h.maturityDate?.let { StatRow(tr("Matures"), monthYear(it)) }
                        mp2Projection(pos)?.let { StatRow(tr("Estimated at maturity"), Money.format(it, cur), Budgy.colors.income) }
                    } else {
                        StatRow(tr("You hold"), formatUnits(pos.units) + " " + h.symbol)
                        StatRow(tr("Price"), if (h.price > 0) priceLabel(h, cur) else tr("Not set"))
                        if (h.hasForeignPrice(cur) && h.price > 0) {
                            StatRow(tr("In {0}", cur), formatPrice(h.price, cur))
                            StatRow(tr("Exchange rate"), "1 ${h.priceCurrency} = " + formatPrice(h.fxRate(), cur))
                        }
                        if (pos.units > 0) StatRow(tr("Average cost"), formatPrice(pos.avgCost, cur))
                        StatRow(tr("Invested"), Money.format(pos.cost, cur))
                    }
                    if (pos.realized != 0L) {
                        StatRow(tr("Realized from sales"), Money.format(pos.realized, cur, showPlus = true), gainColor(pos.realized))
                    }
                }
            }

            if (!isMp2) {
                item {
                    BudgyCard(padding = 14.dp) {
                        if (h.isLive) {
                            Text(tr("Live price from CoinGecko"), style = MaterialTheme.typography.titleSmall)
                            Text(
                                when {
                                    status.loading -> tr("Updating prices…")
                                    status.error != null -> status.error!!
                                    settings?.canUseInternet == true -> tr("Updated {0}", ago(h.priceUpdated))
                                    else -> tr("Live prices are off. Showing the last price Budgy got.")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (status.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (settings?.canUseInternet == true) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = { vm.refreshPrices(force = true) }, enabled = !status.loading) {
                                    Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr("Refresh price"))
                                }
                            }
                        } else {
                            Text(tr("Manual price"), style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (h.priceUpdated > 0) tr("You last updated it {0}", ago(h.priceUpdated)) else tr("Set the price so Budgy can show what it's worth."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { editPrice = true }) { Text(tr("Update price")) }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { action = InvestType.BUY }, modifier = Modifier.weight(1f)) {
                        Text(InvestType.BUY.labelFor(h.kind), maxLines = 1)
                    }
                    OutlinedButton(onClick = { action = InvestType.EARN }, modifier = Modifier.weight(1f)) {
                        Text(InvestType.EARN.labelFor(h.kind), maxLines = 1)
                    }
                    OutlinedButton(onClick = { action = InvestType.SELL }, modifier = Modifier.weight(1f), enabled = pos.units > 0) {
                        Text(if (isMp2) tr("Withdraw") else tr("Sell"), maxLines = 1)
                    }
                }
            }

            item { SectionHeader(tr("History")) }
            if (history.isEmpty()) {
                item {
                    Text(
                        if (isMp2) tr("No records yet. Add your first savings above.") else tr("No records yet. Add your first buy above."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            items(history, key = { it.id }) { t ->
                InvestRow(h, t, t.accountId?.let { acctMap[it]?.name }) { deleting = t }
            }
            item {
                Text(
                    if (isMp2) tr("Pag-IBIG declares MP2 dividends once a year. The estimate uses your expected rate and doesn't count future savings.")
                    else if (h.kind == AssetKind.FUND) tr("Update the price whenever you check your broker or fund. Gains compare today's value with what you paid, using the average cost of your buys. Tap a record to delete it.")
                    else tr("Gains compare today's value with what you paid, using the average cost of your buys. Tap a record to delete it."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }

        action?.let { type ->
            InvestDialog(
                h = h, type = type, pos = pos, accounts = accounts,
                defaultAccountId = settings?.lastAccountId ?: -1,
                onSave = { t, acct -> vm.addInvestTxn(h, t, acct) },
                onDismiss = { action = null },
            )
        }
        if (editPrice) {
            PriceDialog(
                h,
                onSave = { quote, fx -> if (h.kind == AssetKind.FUND) vm.setFundPrice(h, quote, fx) else vm.setManualPrice(h, quote) },
                onDismiss = { editPrice = false },
            )
        }
        if (confirmDelete) {
            ConfirmDialog(
                title = tr("Delete {0}?", h.name),
                text = tr("Its buys, sells and earnings are deleted too, and the account transactions they made are undone."),
                onConfirm = { vm.deleteHolding(h); go.back() },
                onDismiss = { confirmDelete = false },
            )
        }
        deleting?.let { t ->
            ConfirmDialog(
                title = tr("Delete this record?"),
                text = if (t.txnId != null) tr("The matching account transaction is removed too, so your balance goes back.") else tr("This can't be undone."),
                onConfirm = { vm.deleteInvestTxn(t) },
                onDismiss = { deleting = null },
            )
        }
    }
}

@Composable
private fun InvestRow(h: Holding, t: InvestTxn, accountName: String?, onClick: () -> Unit) {
    val cur = Budgy.currency
    val isMp2 = h.kind == AssetKind.MP2
    val (icon, tint) = when (t.type) {
        InvestType.BUY -> Icons.Rounded.Add to MaterialTheme.colorScheme.primary
        InvestType.SELL -> Icons.Rounded.Remove to MaterialTheme.colorScheme.tertiary
        InvestType.EARN -> Icons.Rounded.AutoAwesome to Budgy.colors.gold
    }
    val subtitle = buildList {
        add(Dates.dayLabel(t.date))
        if (!isMp2) add(formatUnits(t.units) + " " + h.symbol)
        accountName?.let { add(it) }
        if (t.note.isNotBlank()) add(t.note)
    }.joinToString(" · ")
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint, 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.type.labelFor(h.kind), style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        when {
            t.type == InvestType.EARN && !isMp2 ->
                Text("+" + formatUnits(t.units) + " " + h.symbol, style = MaterialTheme.typography.bodyMedium.merge(Numeric), color = Budgy.colors.income, fontWeight = FontWeight.SemiBold)
            t.type == InvestType.BUY -> MoneyText(t.amount, style = MaterialTheme.typography.bodyMedium)
            else -> MoneyText(t.amount, style = MaterialTheme.typography.bodyMedium, color = Budgy.colors.income, showPlus = true)
        }
    }
}

@Composable
private fun InvestDialog(
    h: Holding,
    type: InvestType,
    pos: Position,
    accounts: List<Account>,
    defaultAccountId: Long,
    onSave: (InvestTxn, Account?) -> Unit,
    onDismiss: () -> Unit,
) {
    val cur = Budgy.currency
    val isMp2 = h.kind == AssetKind.MP2
    val movesMoney = type != InvestType.EARN
    // Funds priced in dollars: you type amounts in dollars and Budgy converts them with the fund's rate.
    val foreign = h.hasForeignPrice(cur)
    val moneySym = if (foreign) h.priceCurrency else cur
    fun toHome(c: Long): Long = if (foreign) (c * h.fxRate()).roundToLong() else c
    var units by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(Dates.today()) }
    var account by remember {
        mutableStateOf(if (movesMoney) accounts.firstOrNull { it.id == defaultAccountId } ?: accounts.firstOrNull() else null)
    }
    var error by remember { mutableStateOf<String?>(null) }

    val title = when {
        isMp2 && type == InvestType.BUY -> tr("Add savings")
        isMp2 && type == InvestType.EARN -> tr("Add dividend")
        isMp2 -> tr("Withdraw from MP2")
        type == InvestType.BUY -> tr("Buy {0}", h.symbol.ifBlank { h.name })
        type == InvestType.SELL -> tr("Sell {0}", h.symbol.ifBlank { h.name })
        else -> tr("Add earnings")
    }

    fun numberField(text: String) = text.filter { it.isDigit() || it == '.' }.take(20)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isMp2) {
                    OutlinedTextField(
                        value = units,
                        onValueChange = { units = numberField(it); error = null },
                        label = {
                            Text(
                                when (type) {
                                    InvestType.BUY -> tr("How many {0} you got", h.symbol.ifBlank { tr("units") })
                                    InvestType.SELL -> tr("How many {0} you sold", h.symbol.ifBlank { tr("units") })
                                    InvestType.EARN -> tr("How many {0} you earned", h.symbol.ifBlank { tr("units") })
                                }
                            )
                        },
                        supportingText = hint(
                            when {
                                type == InvestType.SELL -> tr("You have {0}", formatUnits(pos.units))
                                type == InvestType.BUY && h.price > 0 && parseUnits(units) != null ->
                                    tr("Worth {0} at today's price", Money.format(((parseUnits(units) ?: 0.0) * (if (foreign) h.quotePrice else h.price) * 100).roundToLong(), moneySym))
                                else -> null
                            }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (isMp2 || type != InvestType.EARN) {
                    OutlinedTextField(
                        value = total,
                        onValueChange = { total = numberField(it); error = null },
                        label = {
                            Text(
                                when {
                                    isMp2 && type == InvestType.BUY -> tr("Amount saved")
                                    isMp2 && type == InvestType.EARN -> tr("Dividend amount")
                                    isMp2 -> tr("Amount withdrawn")
                                    type == InvestType.BUY -> tr("Total paid, including fees")
                                    else -> tr("Total received, after fees")
                                }
                            )
                        },
                        supportingText = hint(
                            when {
                                isMp2 && type == InvestType.SELL -> tr("You have {0}", Money.format(pos.value, cur))
                                isMp2 && type == InvestType.EARN -> tr("Dividends stay in your MP2 unless you withdraw them.")
                                foreign && Money.parse(total) != null ->
                                    tr("≈ {0} at 1 {1} = {2}", Money.format(toHome(Money.parse(total) ?: 0L), cur), h.priceCurrency, formatPrice(h.fxRate(), cur))
                                else -> null
                            }
                        ),
                        prefix = { Text(moneySym) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (movesMoney) {
                    SelectField(
                        label = if (type == InvestType.BUY) tr("Paid from") else tr("Money goes into"),
                        value = account?.name ?: tr("None (don't change any account)"),
                        options = listOf<Account?>(null) + accounts,
                        optionLabel = { it?.name ?: tr("None (don't change any account)") },
                        onSelect = { account = it },
                    )
                }
                DateField(tr("Date"), date) { date = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(40) },
                    label = { Text(tr("Note (optional)")) },
                    placeholder = hint(
                        when {
                            type != InvestType.EARN || isMp2 -> null
                            h.kind == AssetKind.FUND -> tr("Reinvested dividends")
                            else -> tr("Staking, rewards, airdrop…")
                        }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val u = parseUnits(units)
                val cents = Money.parse(total)
                error = when {
                    !isMp2 && u == null -> tr("Enter how many units")
                    (isMp2 || type != InvestType.EARN) && (cents == null || cents <= 0) -> tr("Enter an amount")
                    !isMp2 && type == InvestType.SELL && (u ?: 0.0) > pos.units * 1.000001 -> tr("You only have {0}", formatUnits(pos.units))
                    isMp2 && type == InvestType.SELL && (cents ?: 0L) > pos.value -> tr("You only have {0}", Money.format(pos.value, cur))
                    else -> null
                }
                if (error != null) return@TextButton
                val t = if (isMp2) {
                    val c = cents ?: 0L
                    InvestTxn(holdingId = h.id, type = type, units = c / 100.0, amount = c, date = date, note = note.trim())
                } else {
                    InvestTxn(
                        holdingId = h.id, type = type, units = u ?: 0.0,
                        amount = if (type == InvestType.EARN) 0L else toHome(cents ?: 0L),
                        date = date, note = note.trim(),
                    )
                }
                onSave(t, if (movesMoney) account else null)
                onDismiss()
            }) { Text(tr("Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

@Composable
private fun PriceDialog(h: Holding, onSave: (Double, Long) -> Unit, onDismiss: () -> Unit) {
    val cur = Budgy.currency
    val foreign = h.hasForeignPrice(cur)
    val start = if (h.kind == AssetKind.FUND) h.quotePrice.takeIf { it > 0 } ?: h.price else h.price
    var text by remember { mutableStateOf(if (start > 0) unitsInput(start) else "") }
    var fx by remember { mutableStateOf(com.budgy.app.util.rateText(h.fxRateMicros).replace(",", "")) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Price of 1 {0}", h.symbol.ifBlank { h.name })) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' }.take(20); error = null },
                    prefix = { Text(if (foreign) h.priceCurrency else cur) },
                    singleLine = true,
                    isError = error != null,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (foreign) {
                    OutlinedTextField(
                        value = fx,
                        onValueChange = { fx = it.filter { c -> c.isDigit() || c == '.' }.take(12); error = null },
                        label = { Text(tr("1 {0} in {1}", h.priceCurrency, cur)) },
                        prefix = { Text(cur) },
                        supportingText = hint(
                            parsePrice(text)?.let { q -> parseRate(fx)?.let { r -> tr("≈ {0} per unit", formatPrice(q * r / 1_000_000.0, cur)) } }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = parsePrice(text)
                val r = if (foreign) parseRate(fx) else 1_000_000L
                error = when {
                    p == null -> tr("Enter a price")
                    r == null -> tr("Enter the exchange rate")
                    else -> null
                }
                if (p != null && r != null) { onSave(p, r); onDismiss() }
            }) { Text(tr("Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

// ====================================================================================
// Add or edit a holding
// ====================================================================================

@Composable
fun HoldingEditorScreen(vm: BudgyViewModel, go: BudgyNav, id: Long) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val cur = Budgy.currency
    var existing by remember { mutableStateOf<Holding?>(null) }
    var loaded by rememberSaveable { mutableStateOf(id < 0) }
    var kind by rememberSaveable { mutableStateOf(AssetKind.CRYPTO) }
    var live by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf("") }
    var symbol by rememberSaveable { mutableStateOf("") }
    var coinId by rememberSaveable { mutableStateOf("") }
    var priceText by rememberSaveable { mutableStateOf("") }
    var rateText by rememberSaveable { mutableStateOf(MP2_DEFAULT_RATE) }
    var fundCur by rememberSaveable { mutableStateOf(if (cur == "$") cur else "$") }
    var fxText by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableLongStateOf(Dates.today()) }
    var maturity by rememberSaveable { mutableLongStateOf(mp2Maturity(Dates.today())) }
    var color by rememberSaveable { mutableLongStateOf(DefaultData.swatches[10]) }
    var error by remember { mutableStateOf<String?>(null) }

    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CoinGecko.Coin>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        if (id >= 0 && existing == null) {
            vm.getHolding(id)?.let { h ->
                existing = h
                if (!loaded) {
                    kind = h.kind
                    live = h.coinId.isNotEmpty()
                    name = h.name
                    symbol = h.symbol
                    coinId = h.coinId
                    priceText = when {
                        h.kind == AssetKind.CRYPTO && h.price > 0 -> unitsInput(h.price)
                        h.kind == AssetKind.FUND && h.quotePrice > 0 -> unitsInput(h.quotePrice)
                        else -> ""
                    }
                    if (h.kind == AssetKind.FUND) {
                        fundCur = h.priceCurrency.ifEmpty { cur }
                        if (h.priceCurrency.isNotEmpty()) fxText = com.budgy.app.util.rateText(h.fxRateMicros).replace(",", "")
                    }
                    rateText = if (h.rateBps > 0) bpsText(h.rateBps) else ""
                    h.maturityDate?.let { maturity = it; start = LocalDate.ofEpochDay(it).minusYears(5).toEpochDay() }
                    color = h.color
                    loaded = true
                }
            }
        }
    }

    fun search() {
        val q = query.trim()
        if (q.length < 2 || searching) return
        searching = true
        searchError = null
        scope.launch {
            vm.searchCoins(q)
                .onSuccess { results = it; if (it.isEmpty()) searchError = tr("No coins found. Try another name, or use a manual price.") }
                .onFailure {
                    searchError = if (it is CoinGecko.RateLimited) tr("CoinGecko is busy right now. Try again in a minute.")
                    else tr("Couldn't search. Check your internet connection.")
                }
            searching = false
        }
    }

    fun save() {
        val isCrypto = kind == AssetKind.CRYPTO
        val isFund = kind == AssetKind.FUND
        val manualPrice = (isCrypto && !live) || isFund
        val price = if (manualPrice && priceText.isNotBlank()) parsePrice(priceText) else null
        val rate = if (kind == AssetKind.MP2 && rateText.isNotBlank()) parseRateBps(rateText) else 0
        val fundForeign = isFund && fundCur != cur
        val fx = if (fundForeign) parseRate(fxText) else 1_000_000L
        error = when {
            isCrypto && live && coinId.isEmpty() -> tr("Search and pick a coin, or switch to a manual price")
            name.isBlank() -> tr("Give it a name")
            manualPrice && priceText.isNotBlank() && price == null -> tr("Enter a valid price")
            fundForeign && fx == null -> tr("Enter the exchange rate")
            kind == AssetKind.MP2 && rate == null -> tr("Enter a rate like 7.12")
            else -> null
        }
        if (error != null) return
        val base = existing ?: Holding(name = "", kind = kind, color = color)
        val h = when (kind) {
            AssetKind.CRYPTO -> base.copy(
                name = name.trim(), symbol = symbol.trim().uppercase(), kind = kind,
                coinId = if (live) coinId else "",
                // A live coin keeps its last price unless you switched coins; a manual one uses what you typed.
                price = when {
                    live && base.coinId == coinId -> base.price
                    live -> 0.0
                    else -> price ?: base.price.takeIf { base.coinId.isEmpty() } ?: 0.0
                },
                priceUpdated = when {
                    live && base.coinId == coinId -> base.priceUpdated
                    live -> 0L
                    price != null && price != base.price -> System.currentTimeMillis()
                    else -> base.priceUpdated
                },
                rateBps = 0, maturityDate = null, color = color,
            )
            AssetKind.FUND -> {
                val rateMicros = fx ?: 1_000_000L
                val quote = price ?: base.quotePrice.takeIf { base.kind == AssetKind.FUND } ?: 0.0
                val changed = price != null && (price != base.quotePrice || rateMicros != base.fxRateMicros)
                base.copy(
                    name = name.trim(), symbol = symbol.trim().uppercase(), kind = kind, coinId = "",
                    priceCurrency = if (fundForeign) fundCur else "", fxRateMicros = rateMicros, quotePrice = quote,
                    price = quote * rateMicros / 1_000_000.0,
                    priceUpdated = if (changed) System.currentTimeMillis() else base.priceUpdated,
                    rateBps = 0, maturityDate = null, color = color,
                )
            }
            AssetKind.MP2 -> base.copy(
                name = name.trim(), symbol = "", kind = kind, coinId = "", price = 1.0,
                rateBps = rate ?: 0, maturityDate = maturity, color = color,
            )
        }
        vm.saveHolding(h) { newId -> if (id < 0) go.replaceWithHolding(newId) else go.back() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (id < 0) tr("Add investment") else tr("Edit investment")) },
                navigationIcon = { IconButton(onClick = go::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back")) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        if (!loaded) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (existing == null) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    AssetKind.entries.forEachIndexed { i, k ->
                        SegmentedButton(
                            selected = kind == k,
                            onClick = {
                                kind = k; error = null
                                if (k == AssetKind.MP2 && name.isBlank()) name = tr("Pag-IBIG MP2")
                                if (k == AssetKind.CRYPTO && name == tr("Pag-IBIG MP2")) name = ""
                                color = when (k) {
                                    AssetKind.MP2 -> DefaultData.swatches[3]
                                    AssetKind.FUND -> DefaultData.swatches[1]
                                    AssetKind.CRYPTO -> DefaultData.swatches[10]
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(i, AssetKind.entries.size),
                            label = { Text(k.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }

            if (kind == AssetKind.CRYPTO) {
                Text(tr("Price"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = live, onClick = { live = true; error = null }, shape = SegmentedButtonDefaults.itemShape(0, 2), label = { Text(tr("Live (CoinGecko)")) })
                    SegmentedButton(selected = !live, onClick = { live = false; error = null }, shape = SegmentedButtonDefaults.itemShape(1, 2), label = { Text(tr("Manual price")) })
                }
                val s = settings
                if (live) {
                    when {
                        s == null -> Unit
                        !s.internetNoticeSeen -> InternetNoticeCard(onLive = { vm.setLivePrices(true) }, onManual = { vm.setLivePrices(false); live = false })
                        !s.livePrices -> BudgyCard(padding = 14.dp) {
                            Text(tr("Live prices are off"), style = MaterialTheme.typography.titleSmall)
                            Text(
                                tr("Turn them on to search CoinGecko, or use a manual price for this coin."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(onClick = { vm.setLivePrices(true) }) { Text(tr("Turn on live prices")) }
                        }
                        else -> {
                            if (coinId.isNotEmpty()) {
                                BudgyCard(padding = 14.dp) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconBadge(Icons.Rounded.CurrencyBitcoin, Color(color), 38.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(name.ifBlank { coinId }, style = MaterialTheme.typography.titleSmall)
                                            Text(symbol + " · " + tr("CoinGecko ID: {0}", coinId), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        TextButton(onClick = { coinId = ""; results = emptyList() }) { Text(tr("Change")) }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { query = it.take(40); searchError = null },
                                    label = { Text(tr("Search coins")) },
                                    placeholder = { Text(tr("Bitcoin, ETH, Solana…")) },
                                    singleLine = true,
                                    trailingIcon = {
                                        if (searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                        else IconButton(onClick = { search() }) { Icon(Icons.Rounded.Search, tr("Search")) }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { search() }),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                searchError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                                val shown = if (results.isNotEmpty()) results else if (query.isBlank()) CoinGecko.popular else emptyList()
                                if (results.isEmpty() && shown.isNotEmpty()) {
                                    Text(tr("Popular coins"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (shown.isNotEmpty()) {
                                    BudgyCard(padding = 6.dp) {
                                        shown.forEachIndexed { i, c ->
                                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        coinId = c.id; name = c.name; symbol = c.symbol
                                                        results = emptyList(); error = null
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(c.name, style = MaterialTheme.typography.bodyLarge)
                                                    Text(c.symbol, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                c.rank?.let { Text("#$it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                            }
                                        }
                                    }
                                }
                                Text(
                                    tr("Can't find it? Switch to Manual price to track any coin."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(30); error = null },
                        label = { Text(tr("Coin name")) },
                        placeholder = { Text(tr("My Token")) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it.filter { c -> !c.isWhitespace() }.take(10).uppercase() },
                        label = { Text(tr("Symbol (optional)")) },
                        placeholder = { Text("MTK") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' }.take(20); error = null },
                        label = { Text(tr("Price of 1 coin")) },
                        supportingText = { Text(tr("You can update it anytime from the coin's page.")) },
                        prefix = { Text(cur) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else if (kind == AssetKind.FUND) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40); error = null },
                    label = { Text(tr("Fund or ETF name")) },
                    placeholder = { Text(tr("S&P 500 ETF, index fund, UITF…")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.filter { c -> !c.isWhitespace() }.take(10).uppercase() },
                    label = { Text(tr("Ticker (optional)")) },
                    placeholder = { Text("VOO") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                val curOptions = listOf(cur) + Money.currencies.map { it.first }.filter { it != cur }
                SelectField(
                    label = tr("Price currency"),
                    value = Money.currencies.firstOrNull { it.first == fundCur }?.let { "${it.first}  ·  ${it.second}" } ?: fundCur,
                    options = curOptions,
                    optionLabel = { sym -> Money.currencies.firstOrNull { it.first == sym }?.let { "${it.first}  ·  ${it.second}" } ?: sym },
                    onSelect = { fundCur = it; error = null },
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' }.take(20); error = null },
                    label = { Text(tr("Price of 1 unit")) },
                    supportingText = { Text(tr("Check your broker or fund's website. You can update it anytime.")) },
                    prefix = { Text(fundCur) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (fundCur != cur) {
                    OutlinedTextField(
                        value = fxText,
                        onValueChange = { fxText = it.filter { c -> c.isDigit() || c == '.' }.take(12); error = null },
                        label = { Text(tr("1 {0} in {1}", fundCur, cur)) },
                        placeholder = { Text("56.20") },
                        supportingText = hint(
                            parsePrice(priceText)?.let { q -> parseRate(fxText)?.let { r -> tr("≈ {0} per unit", formatPrice(q * r / 1_000_000.0, cur)) } }
                                ?: tr("Budgy converts the price and your buys to {0}.", cur)
                        ),
                        prefix = { Text(cur) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30); error = null },
                    label = { Text(tr("Name")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it.filter { c -> c.isDigit() || c == '.' }.take(6); error = null },
                    label = { Text(tr("Expected yearly dividend")) },
                    suffix = { Text("%") },
                    supportingText = { Text(tr("Pag-IBIG's 2025 MP2 dividend was 7.12%. Use your own estimate; past rates don't guarantee future ones.")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                DateField(tr("Started"), start) { start = it; maturity = mp2Maturity(it) }
                DateField(tr("Matures"), maturity) { maturity = it }
                Text(
                    tr("MP2 matures 5 years after you open it. Add your savings and dividends from the MP2 page."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(tr("Color"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ColorPicker(DefaultData.swatches, color) { color = it }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(
                onClick = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text(tr("Save"), style = MaterialTheme.typography.titleMedium) }
        }
    }
}
