package com.budgy.app.ui

import com.budgy.app.i18n.tr
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.budgy.app.BudgyApp
import com.budgy.app.data.Account
import com.budgy.app.data.Backup
import com.budgy.app.data.BillingCycle
import com.budgy.app.data.Budget
import com.budgy.app.data.Category
import com.budgy.app.data.CsvImport
import com.budgy.app.data.Debt
import com.budgy.app.data.EnvelopeMove
import com.budgy.app.data.Goal
import com.budgy.app.data.GoalEntry
import com.budgy.app.data.Iou
import com.budgy.app.data.Recurring
import com.budgy.app.data.Settings
import com.budgy.app.data.Subscription
import com.budgy.app.data.TxType
import com.budgy.app.data.Txn
import com.budgy.app.data.AssetKind
import com.budgy.app.data.Holding
import com.budgy.app.data.isLive
import com.budgy.app.data.InvestTxn
import com.budgy.app.data.InvestType
import com.budgy.app.util.CoinGecko
import com.budgy.app.util.Portfolio
import com.budgy.app.util.formatUnits
import com.budgy.app.util.isForeign
import com.budgy.app.util.portfolio
import kotlinx.coroutines.flow.MutableStateFlow
import com.budgy.app.util.Dates
import com.budgy.app.util.advanceDate
import com.budgy.app.util.computeBalances
import com.budgy.app.util.homeBalances
import com.budgy.app.util.toHomeTxns
import com.budgy.app.util.Badge
import com.budgy.app.util.Insight
import com.budgy.app.util.computeBadges
import com.budgy.app.util.spendingInsights
import com.budgy.app.util.currentPeriod
import com.budgy.app.util.previousPeriod
import com.budgy.app.i18n.L10n
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(kotlinx.coroutines.FlowPreview::class)
class BudgyViewModel(private val app: BudgyApp) : ViewModel() {
    private val repo = app.repository
    private val store = app.settings

    val settings: StateFlow<Settings?> = store.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val language = snapshotFlow { L10n.lang }

    /** Default names ("Cash", "Food & Dining"…) show in the chosen language; your own names stay as typed. */
    val accounts = combine(repo.accounts, language) { list, _ -> list.map { it.copy(name = L10n.tr(it.name)) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories = combine(repo.categories, language) { list, _ -> list.map { it.copy(name = L10n.tr(it.name)) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val transactions = repo.transactions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val budgets = repo.budgets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val subscriptions = repo.subscriptions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recurring = repo.recurring.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val goals = repo.goals.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val goalEntries = repo.goalEntries.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val ious = repo.ious.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val priceChanges = repo.priceChanges.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val debts = repo.debts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val envelopeMoves = repo.envelopeMoves.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val holdings = repo.holdings.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val investTxns = repo.investTxns.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val priceSnaps = repo.priceSnaps.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Every holding's units, cost, value and gain (V5). */
    val portfolio: StateFlow<Portfolio> = combine(holdings, investTxns) { h, t -> portfolio(h, t) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Portfolio(emptyList()))

    /** accountId -> current balance in cents, in each account's own currency */
    val balances: StateFlow<Map<Long, Long>> = combine(accounts, transactions) { a, t -> computeBalances(a, t) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** accountId -> balance converted to the main currency (for totals). */
    val homeBalances: StateFlow<Map<Long, Long>> = combine(accounts, balances, store.settings) { a, b, s -> homeBalances(a, b, s.currency) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Accounts you still use (not archived) — for pickers. */
    val activeAccounts: StateFlow<List<Account>> = accounts.map { list -> list.filter { !it.archived } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Transactions with amounts in the main currency — use for budgets, reports and other totals. */
    val statTxns: StateFlow<List<Txn>> = combine(transactions, accounts, store.settings) { t, a, s -> toHomeTxns(t, a, s.currency) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** goalId -> amount saved so far */
    val goalSaved: StateFlow<Map<Long, Long>> = goalEntries
        .map { list -> list.groupBy { it.goalId }.mapValues { (_, v) -> v.sumOf { it.amount } } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** "Ask me first" recurring entries that are due and waiting for a tap. */
    val dueRecurring: StateFlow<List<Recurring>> = recurring
        .map { list -> list.filter { it.active && !it.autoLog && it.nextDate <= Dates.today() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Did every budget stay under its limit in the last finished period? null = not enough history. */
    private val budgetsUnderLastPeriod: StateFlow<Boolean?> = combine(budgets, statTxns, store.settings) { b, t, s ->
        if (b.isEmpty()) return@combine null
        val prev = previousPeriod(currentPeriod(s.period), s.period)
        val active = b.filter { it.startDate in 1L..prev.first }
        if (active.isEmpty()) return@combine null
        val spent = spendingInRange(t, prev)
        active.all { (spent[it.categoryId] ?: 0L) <= it.monthlyLimit }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val badges: StateFlow<List<Badge>> = combine(transactions, goals, goalSaved, debts, budgetsUnderLastPeriod) { t, g, gs, d, b ->
        computeBadges(t, g, gs, d, b)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** "Food is 40% higher than usual" — shown on Home. */
    val insights: StateFlow<List<Insight>> = combine(statTxns, categories) { t, c -> spendingInsights(t, c) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    fun toast(text: String) { _messages.tryEmit(text) }

    init {
        // Celebrate newly unlocked badges (each one once).
        viewModelScope.launch {
            val prefs = app.getSharedPreferences("badges", android.content.Context.MODE_PRIVATE)
            badges.debounce(1500).collect { list ->
                if (list.isEmpty()) return@collect
                val seen = prefs.getStringSet("seen", null)
                val unlocked = list.filter { it.unlocked }.map { it.id }.toSet()
                if (seen == null) {
                    // First run with badges: remember what's already unlocked without a flood of messages.
                    prefs.edit().putStringSet("seen", unlocked).apply()
                    return@collect
                }
                val fresh = list.filter { it.unlocked && it.id !in seen }
                if (fresh.isNotEmpty()) {
                    prefs.edit().putStringSet("seen", seen + fresh.map { it.id }).apply()
                    toast(L10n.tr("Badge unlocked: {0}", fresh.joinToString(", ") { L10n.tr(it.title) }))
                }
            }
        }
        viewModelScope.launch {
            val n = repo.processRecurring()
            if (n > 0) toast(tr("Logged {0} recurring transaction{1}", n, if (n == 1) "" else "s"))
        }
    }

    // Settings
    fun setDarkMode(v: Boolean) = viewModelScope.launch { store.setDarkMode(v) }
    fun setCurrency(v: String) = viewModelScope.launch {
        store.setCurrency(v)
        fetchPrices(force = true) // live prices follow your main currency
    }
    fun setReminders(v: Boolean) = viewModelScope.launch { store.setReminders(v) }
    fun setAppLock(v: Boolean) = viewModelScope.launch { store.setAppLock(v) }
    fun setEnvelopeMode(v: Boolean) = viewModelScope.launch { store.setEnvelopeMode(v) }
    fun setPeriod(mode: String, day: Int) = viewModelScope.launch { store.setPeriod(mode, day) }
    fun setBudgetAlerts(v: Boolean) = viewModelScope.launch { store.setBudgetAlerts(v) }
    fun setWeeklySummary(v: Boolean) = viewModelScope.launch { store.setWeeklySummary(v) }
    fun setSmartAlerts(v: Boolean) = viewModelScope.launch { store.setSmartAlerts(v) }
    fun setHelpSeen() = viewModelScope.launch { store.setHelpSeen(true) }
    fun setLanguage(v: String) = viewModelScope.launch {
        L10n.lang = v
        store.setLanguage(v)
    }

    // Transactions
    suspend fun getTxn(id: Long): Txn? = repo.getTxn(id)

    /**
     * Saves a transaction. When [repeat] is set on a NEW transaction, also creates a recurring rule
     * that logs it automatically from the next occurrence on.
     */
    fun saveTxn(t: Txn, repeat: BillingCycle? = null) = viewModelScope.launch {
        repo.saveTxn(t)
        store.rememberLastUsed(t.type, t.accountId, t.categoryId)
        if (repeat != null && t.id == 0L) {
            repo.addRecurring(
                Recurring(
                    type = t.type, amount = t.amount, accountId = t.accountId, toAccountId = t.toAccountId,
                    categoryId = t.categoryId, note = t.note, cycle = repeat,
                    nextDate = advanceDate(t.date, repeat, 30), autoLog = true,
                )
            )
            val n = repo.processRecurring()
            toast(tr("Saved. Repeats {0}", repeat.label.lowercase()) + if (n > 0) tr(" ({0} past entries logged)", n) else "")
        }
    }

    fun saveSplit(t: Txn, myShare: Long, others: List<Pair<String, Long>>) = viewModelScope.launch {
        repo.saveSplitExpense(t, myShare, others)
        store.rememberLastUsed(t.type, t.accountId, t.categoryId)
        toast(tr("Split saved. {0} IOU{1} added", others.size, if (others.size == 1) "" else "s"))
    }

    fun deleteTxn(t: Txn) = viewModelScope.launch {
        repo.deleteTxn(t)
        toast(tr("Transaction deleted"))
    }

    // Accounts
    // Screens show default names translated; keep the stored English name when it wasn't really renamed.
    fun saveAccount(a: Account) = viewModelScope.launch {
        val orig = repo.accounts.first().firstOrNull { it.id == a.id }
        repo.saveAccount(if (orig != null && L10n.tr(orig.name) == a.name) a.copy(name = orig.name) else a)
    }
    fun deleteAccount(a: Account) = viewModelScope.launch {
        if (repo.deleteAccount(a)) toast(tr("{0} deleted", a.name))
        else toast(tr("{0} has transactions, so it can't be deleted. Archive it instead to hide it.", a.name))
    }

    /** Logs a balance adjustment so the account matches [actual]. */
    fun adjustBalance(a: Account, current: Long, actual: Long, note: String) = viewModelScope.launch {
        repo.saveTxn(Txn(type = TxType.ADJUST, amount = actual - current, accountId = a.id, note = note.ifBlank { "Balance adjustment" }, date = Dates.today()))
        toast(tr("{0} adjusted", a.name))
    }

    // Categories
    fun saveCategory(c: Category) = viewModelScope.launch {
        val orig = repo.categories.first().firstOrNull { it.id == c.id }
        repo.saveCategory(if (orig != null && L10n.tr(orig.name) == c.name) c.copy(name = orig.name) else c)
    }
    fun deleteCategory(c: Category) = viewModelScope.launch {
        repo.deleteCategory(c)
        toast(tr("{0} deleted. Its transactions are now uncategorized.", c.name))
    }

    // Budgets
    fun saveBudget(b: Budget) = viewModelScope.launch { repo.saveBudget(b) }
    fun deleteBudget(b: Budget) = viewModelScope.launch { repo.deleteBudget(b) }

    // Subscriptions
    suspend fun getSubscription(id: Long): Subscription? = repo.getSubscription(id)
    fun saveSubscription(s: Subscription) = viewModelScope.launch { repo.saveSubscription(s) }
    fun deleteSubscription(s: Subscription) = viewModelScope.launch {
        repo.deleteSubscription(s)
        toast(tr("{0} removed", s.name))
    }

    fun markPaid(s: Subscription) = viewModelScope.launch {
        if (repo.markSubscriptionPaid(s)) toast(tr("{0} logged as paid", s.name))
        else toast(tr("Add an account first so Budgy knows where the payment came from."))
    }

    fun markUsedToday(s: Subscription) = viewModelScope.launch {
        repo.saveSubscription(s.copy(lastUsed = Dates.today()))
        toast(tr("Noted: you used {0} today", s.name))
    }

    // Recurring
    suspend fun getRecurring(id: Long) = repo.getRecurring(id)
    fun saveRecurring(r: Recurring) = viewModelScope.launch {
        repo.saveRecurring(r)
        val n = repo.processRecurring()
        if (n > 0) toast(tr("Logged {0} due entr{1}", n, if (n == 1) "y" else "ies"))
    }
    fun deleteRecurring(r: Recurring) = viewModelScope.launch {
        repo.deleteRecurring(r)
        toast(tr("Stopped repeating. Past entries were kept."))
    }
    fun confirmRecurring(r: Recurring) = viewModelScope.launch { repo.confirmRecurring(r) }
    fun skipRecurring(r: Recurring) = viewModelScope.launch { repo.skipRecurring(r) }

    // Goals
    fun saveGoal(g: Goal) = viewModelScope.launch { repo.saveGoal(g) }
    fun deleteGoal(g: Goal) = viewModelScope.launch {
        repo.deleteGoal(g)
        toast(tr("{0} deleted", g.name))
    }
    fun addGoalEntry(e: GoalEntry, goalName: String) = viewModelScope.launch {
        repo.addGoalEntry(e)
        toast(if (e.amount >= 0) tr("Added to {0}", goalName) else tr("Withdrew from {0}", goalName))
    }
    fun deleteGoalEntry(e: GoalEntry) = viewModelScope.launch { repo.deleteGoalEntry(e) }

    // IOUs
    fun addIou(i: Iou, accountId: Long?) = viewModelScope.launch { repo.addIou(i, accountId) }
    fun updateIou(i: Iou) = viewModelScope.launch { repo.updateIou(i) }
    fun settleIou(i: Iou, accountId: Long?) = viewModelScope.launch {
        repo.settleIou(i, accountId)
        toast(tr("Settled with {0}", i.person))
    }
    fun deleteIou(i: Iou) = viewModelScope.launch { repo.deleteIou(i) }

    // Debts (V3)
    fun saveDebt(d: Debt) = viewModelScope.launch { repo.saveDebt(d) }
    fun deleteDebt(d: Debt) = viewModelScope.launch {
        repo.deleteDebt(d)
        toast(tr("{0} removed", d.name))
    }
    fun recordDebtPayment(d: Debt, amount: Long, accountId: Long?) = viewModelScope.launch {
        repo.recordDebtPayment(d, amount, accountId)
        toast(if (amount >= d.balance) tr("{0} is paid off!", d.name) else tr("Payment recorded"))
    }

    // Envelopes (V3)
    fun moveEnvelope(m: EnvelopeMove) = viewModelScope.launch {
        repo.moveEnvelope(m)
        toast(tr("Money moved"))
    }

    // ---------- Investments (V5) ----------

    data class PriceStatus(val loading: Boolean = false, val error: String? = null)

    private val _priceStatus = MutableStateFlow(PriceStatus())
    val priceStatus: StateFlow<PriceStatus> = _priceStatus
    private var lastPriceFetch = 0L

    /** Turning live prices on or off also marks the internet notice as read. */
    fun setLivePrices(v: Boolean) = viewModelScope.launch {
        store.setLivePrices(v)
        if (v) fetchPrices(force = true) else _priceStatus.value = PriceStatus()
    }

    /** Gets CoinGecko prices when they're older than 15 minutes, or right away when [force]d (refresh button). */
    fun refreshPrices(force: Boolean = false) { viewModelScope.launch { fetchPrices(force) } }

    private suspend fun fetchPrices(force: Boolean) {
        val s = store.settings.first()
        if (!s.canUseInternet || _priceStatus.value.loading) return
        val live = repo.holdingsOnce().filter { it.isLive }
        if (live.isEmpty()) return
        val now = System.currentTimeMillis()
        val stale = live.any { now - it.priceUpdated > 15 * 60_000L }
        if (!force && (!stale || now - lastPriceFetch < 60_000L)) return
        if (now - lastPriceFetch < 10_000L) return // don't hammer the free API
        val vs = CoinGecko.vsCurrency(s.currency)
        if (vs == null) {
            _priceStatus.value = PriceStatus(error = tr("Live prices aren't available for your currency. Use manual prices."))
            return
        }
        lastPriceFetch = now
        _priceStatus.value = PriceStatus(loading = true)
        val ids = live.map { it.coinId }.distinct()
        runCatching { CoinGecko.prices(ids, vs) }
            .onSuccess { prices ->
                repo.applyLivePrices(prices)
                _priceStatus.value = PriceStatus(
                    error = if (prices.size < ids.size) tr("Some coins didn't get a price. Open them to check, or switch them to a manual price.") else null
                )
            }
            .onFailure { e ->
                _priceStatus.value = PriceStatus(
                    error = if (e is CoinGecko.RateLimited) tr("CoinGecko is busy right now. Try again in a minute.")
                    else tr("Couldn't get prices. Check your internet connection.")
                )
            }
    }

    suspend fun searchCoins(query: String): Result<List<CoinGecko.Coin>> = runCatching { CoinGecko.search(query) }

    suspend fun getHolding(id: Long): Holding? = repo.getHolding(id)

    fun saveHolding(h: Holding, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repo.saveHolding(h)
        onSaved(id)
        if (h.isLive) { lastPriceFetch = 0L; fetchPrices(force = true) }
    }

    fun deleteHolding(h: Holding) = viewModelScope.launch {
        repo.deleteHolding(h)
        toast(tr("{0} removed", h.name))
    }

    /** Funds: price in the fund's own currency plus the rate to your main currency. */
    fun setFundPrice(h: Holding, quote: Double, fxRateMicros: Long) = viewModelScope.launch {
        repo.saveHolding(
            h.copy(
                quotePrice = quote, fxRateMicros = fxRateMicros,
                price = quote * fxRateMicros / 1_000_000.0, priceUpdated = System.currentTimeMillis(),
            )
        )
        toast(tr("Price updated"))
    }

    fun setManualPrice(h: Holding, price: Double) = viewModelScope.launch {
        repo.setPrice(h.id, price)
        toast(tr("Price updated"))
    }

    /**
     * Saves a buy, sell or earning. With an [account], the money also leaves or enters that account
     * (converted to the account's currency when it's foreign).
     */
    fun addInvestTxn(h: Holding, t: InvestTxn, account: Account?) = viewModelScope.launch {
        val home = store.settings.first().currency
        val moves = account != null && t.amount > 0 && t.type != InvestType.EARN
        val ledger = if (moves && account != null) {
            val amt = if (account.isForeign(home)) Math.round(t.amount / (account.fxRateMicros / 1_000_000.0)) else t.amount
            val what = h.symbol.ifBlank { h.name }
            Txn(
                type = if (t.type == InvestType.BUY) TxType.INVEST else TxType.DIVEST,
                amount = amt, accountId = account.id, date = t.date,
                note = when {
                    h.kind == AssetKind.MP2 && t.type == InvestType.BUY -> tr("Savings · {0}", h.name)
                    h.kind == AssetKind.MP2 -> tr("Withdrawal · {0}", h.name)
                    t.type == InvestType.BUY -> tr("Bought {0} {1}", formatUnits(t.units), what)
                    else -> tr("Sold {0} {1}", formatUnits(t.units), what)
                },
            )
        } else null
        repo.addInvestTxn(t.copy(accountId = ledger?.accountId), ledger)
        toast(tr("Saved"))
    }

    fun deleteInvestTxn(t: InvestTxn) = viewModelScope.launch {
        repo.deleteInvestTxn(t)
        toast(tr("Record deleted"))
    }

    /** The investment record behind an account transaction (for "Investment buy/sell" rows in Activity). */
    suspend fun investTxnForLedger(txnId: Long): InvestTxn? = repo.investTxnForLedger(txnId)

    fun importCsv(uri: Uri) = viewModelScope.launch {
        runCatching { CsvImport.import(app, repo, uri) }
            .onSuccess { r ->
                toast(
                    buildString {
                        append(tr("Imported {0} transaction{1}", r.imported, if (r.imported == 1) "" else "s"))
                        if (r.duplicates > 0) append(tr(" · {0} duplicates skipped", r.duplicates))
                        if (r.skipped > 0) append(tr(" · {0} unreadable rows", r.skipped))
                        if (r.newAccounts > 0) append(tr(" · {0} new account{1}", r.newAccounts, if (r.newAccounts == 1) "" else "s"))
                        if (r.newCategories > 0) append(tr(" · {0} new categor{1}", r.newCategories, if (r.newCategories == 1) "y" else "ies"))
                    }
                )
            }
            .onFailure { toast(tr("Import failed: {0}", it.message)) }
    }

    // Backup
    fun exportBackup(uri: Uri) = viewModelScope.launch {
        runCatching { Backup.exportJson(app, repo, uri) }
            .onSuccess { toast(tr("Backup saved")) }
            .onFailure { toast(tr("Backup failed: {0}", it.message)) }
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        runCatching { Backup.importJson(app, repo, uri) }
            .onSuccess { toast(tr("Restored {0} transactions", it)) }
            .onFailure { toast(tr("Couldn't restore: {0}", it.message)) }
    }

    fun exportCsv(uri: Uri) = viewModelScope.launch {
        runCatching { Backup.exportCsv(app, repo, uri) }
            .onSuccess { toast(tr("Exported {0} transactions", it)) }
            .onFailure { toast(tr("Export failed: {0}", it.message)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { BudgyViewModel(this[APPLICATION_KEY] as BudgyApp) }
        }
    }
}

data class MonthTotals(val income: Long, val expense: Long) {
    val net get() = income - expense
}

fun monthTotals(txns: List<Txn>, ym: YearMonth): MonthTotals {
    val r = Dates.monthRange(ym)
    var inc = 0L
    var exp = 0L
    for (t in txns) {
        if (t.date !in r) continue
        when (t.type) {
            TxType.INCOME -> inc += t.amount
            TxType.EXPENSE -> exp += t.amount
            else -> Unit
        }
    }
    return MonthTotals(inc, exp)
}

/** categoryId (null = uncategorized) -> spent this month */
fun spendingByCategory(txns: List<Txn>, ym: YearMonth): Map<Long?, Long> {
    val r = Dates.monthRange(ym)
    return txns.asSequence()
        .filter { it.type == TxType.EXPENSE && it.date in r }
        .groupBy { it.categoryId }
        .mapValues { (_, v) -> v.sumOf { it.amount } }
}

/** categoryId (null = uncategorized) -> spent within [range] (e.g. the current budget period). */
fun spendingInRange(txns: List<Txn>, range: LongRange): Map<Long?, Long> =
    txns.asSequence()
        .filter { it.type == TxType.EXPENSE && it.date in range }
        .groupBy { it.categoryId }
        .mapValues { (_, v) -> v.sumOf { it.amount } }
