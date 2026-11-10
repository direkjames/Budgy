package com.budgy.app.data

import androidx.room.withTransaction
import com.budgy.app.util.Dates
import com.budgy.app.util.advance
import com.budgy.app.util.advanceDate
import com.budgy.app.util.myAmount

class BudgyRepository(val db: BudgyDatabase) {
    val dao = db.dao()

    val accounts = dao.accounts()
    val categories = dao.categories()
    val transactions = dao.transactions()
    val budgets = dao.budgets()
    val subscriptions = dao.subscriptions()
    val recurring = dao.recurring()
    val goals = dao.goals()
    val goalEntries = dao.goalEntries()
    val ious = dao.ious()
    val priceChanges = dao.priceChanges()
    val debts = dao.debts()
    val envelopeMoves = dao.envelopeMoves()
    val holdings = dao.holdings()
    val investTxns = dao.investTxns()
    val priceSnaps = dao.priceSnaps()

    suspend fun seedIfEmpty() {
        if (dao.categoryCount() > 0) return
        db.withTransaction {
            dao.insertCategories(DefaultData.categories())
            dao.upsertAccount(Account(name = "Cash", type = AccountType.CASH, color = 0xFF0E6E52))
        }
    }

    // Accounts
    suspend fun saveAccount(a: Account) = dao.upsertAccount(a)

    /** Returns false when the account still has transactions (we never silently delete history). */
    suspend fun deleteAccount(a: Account): Boolean {
        if (dao.txCountForAccount(a.id) > 0) return false
        db.withTransaction {
            dao.clearSubscriptionAccount(a.id)
            dao.deleteAccount(a)
        }
        return true
    }

    // Categories
    suspend fun saveCategory(c: Category) = dao.upsertCategory(c)

    suspend fun deleteCategory(c: Category) = db.withTransaction {
        dao.clearTxnCategory(c.id)
        dao.clearSubscriptionCategory(c.id)
        dao.deleteBudgetForCategory(c.id)
        dao.deleteCategory(c)
    }

    // Transactions
    suspend fun getTxn(id: Long) = dao.txn(id)
    suspend fun saveTxn(t: Txn) = dao.upsertTxn(t)
    suspend fun deleteTxn(t: Txn) = dao.deleteTxn(t)

    // Budgets
    suspend fun saveBudget(b: Budget) = dao.upsertBudget(b)
    suspend fun deleteBudget(b: Budget) = dao.deleteBudget(b)

    // Subscriptions
    suspend fun getSubscription(id: Long) = dao.subscription(id)
    suspend fun subscriptionsOnce() = dao.subscriptionsOnce()

    /** Saves and, when the price changed, records it in the price history. */
    suspend fun saveSubscription(s: Subscription) = db.withTransaction {
        val old = if (s.id != 0L) dao.subscription(s.id) else null
        if (old != null && old.amount != s.amount) {
            dao.insertPriceChange(
                PriceChange(subscriptionId = s.id, oldAmount = old.amount, newAmount = s.amount, date = Dates.today())
            )
        }
        dao.upsertSubscription(s)
    }

    suspend fun deleteSubscription(s: Subscription) = db.withTransaction {
        dao.deletePriceChangesFor(s.id)
        dao.deleteSubscription(s)
    }

    /**
     * Logs YOUR share of the charge as an expense today and moves the subscription to its next billing date.
     * A free trial that gets paid becomes a regular subscription. Returns false if there's no account.
     */
    suspend fun markSubscriptionPaid(sub: Subscription): Boolean {
        val accountId = sub.accountId ?: dao.firstAccountId() ?: return false
        db.withTransaction {
            val old = dao.subscription(sub.id)
            if (old != null && old.amount != sub.amount) {
                dao.insertPriceChange(
                    PriceChange(subscriptionId = sub.id, oldAmount = old.amount, newAmount = sub.amount, date = Dates.today())
                )
            }
            dao.upsertTxn(
                Txn(
                    type = TxType.EXPENSE,
                    amount = sub.myAmount(),
                    accountId = accountId,
                    categoryId = sub.categoryId,
                    note = sub.name,
                    date = Dates.today(),
                    subscriptionId = sub.id,
                )
            )
            dao.upsertSubscription(sub.copy(nextBillingDate = sub.advance(), isTrial = false, trialEndDate = null))
        }
        return true
    }

    // ---------- Recurring ----------
    suspend fun getRecurring(id: Long) = dao.recurringById(id)
    suspend fun saveRecurring(r: Recurring) = dao.upsertRecurring(r)
    suspend fun addRecurring(r: Recurring): Long = dao.insertRecurring(r)
    suspend fun deleteRecurring(r: Recurring) = dao.deleteRecurring(r)

    private fun Recurring.toTxn(onDate: Long) = Txn(
        type = type, amount = amount, accountId = accountId,
        toAccountId = if (type == TxType.TRANSFER) toAccountId else null,
        categoryId = if (type == TxType.TRANSFER) null else categoryId,
        note = note, date = onDate, recurringId = id,
    )

    /** Logs every auto-log recurring entry that has come due (catching up if the app wasn't opened). */
    suspend fun processRecurring(today: Long = Dates.today()): Int {
        var logged = 0
        db.withTransaction {
            for (r in dao.dueAutoRecurring(today)) {
                var next = r.nextDate
                var guard = 0
                while (next <= today && guard < 120) {
                    dao.insertTxn(r.toTxn(next))
                    next = advanceDate(next, r.cycle, r.customDays)
                    logged++
                    guard++
                }
                dao.upsertRecurring(r.copy(nextDate = next))
            }
        }
        return logged
    }

    /** For "ask me first" entries: log this occurrence and move to the next date. */
    suspend fun confirmRecurring(r: Recurring) = db.withTransaction {
        dao.insertTxn(r.toTxn(r.nextDate))
        dao.upsertRecurring(r.copy(nextDate = advanceDate(r.nextDate, r.cycle, r.customDays)))
    }

    suspend fun skipRecurring(r: Recurring) =
        dao.upsertRecurring(r.copy(nextDate = advanceDate(r.nextDate, r.cycle, r.customDays)))

    // ---------- Goals ----------
    suspend fun saveGoal(g: Goal) = dao.upsertGoal(g)
    suspend fun deleteGoal(g: Goal) = db.withTransaction {
        dao.deleteEntriesForGoal(g.id)
        dao.deleteGoal(g)
    }
    suspend fun addGoalEntry(e: GoalEntry) = dao.insertGoalEntry(e)
    suspend fun deleteGoalEntry(e: GoalEntry) = dao.deleteGoalEntry(e)

    // ---------- IOUs ----------

    /**
     * Records an IOU. If [accountId] is given, money actually moved now:
     * they owe you = you handed money over (LEND); you owe = you received money (COLLECT).
     */
    suspend fun addIou(i: Iou, accountId: Long?) = db.withTransaction {
        val id = dao.insertIou(i)
        if (accountId != null) {
            dao.insertTxn(
                Txn(
                    type = if (i.direction == IouDirection.THEY_OWE) TxType.LEND else TxType.COLLECT,
                    amount = i.amount, accountId = accountId, date = i.date, iouId = id,
                    note = if (i.direction == IouDirection.THEY_OWE) "Lent to ${i.person}" else "Borrowed from ${i.person}",
                )
            )
        }
    }

    suspend fun updateIou(i: Iou) = dao.upsertIou(i)
    suspend fun deleteIou(i: Iou) = dao.deleteIou(i)

    /** Settles an IOU; if [accountId] is given, logs the money moving in or out of that account. */
    suspend fun settleIou(i: Iou, accountId: Long?) = db.withTransaction {
        val today = Dates.today()
        if (accountId != null) {
            dao.insertTxn(
                Txn(
                    type = if (i.direction == IouDirection.THEY_OWE) TxType.COLLECT else TxType.LEND,
                    amount = i.amount, accountId = accountId, date = today, iouId = i.id,
                    note = if (i.direction == IouDirection.THEY_OWE) "${i.person} paid you back" else "Paid back ${i.person}",
                )
            )
        }
        dao.upsertIou(i.copy(settled = true, settledDate = today))
    }

    /**
     * Split bill: you paid [total] but only [myShare] is your spending.
     * Each other person's share is money out (LEND) plus an IOU saying they owe you.
     */
    suspend fun saveSplitExpense(base: Txn, myShare: Long, others: List<Pair<String, Long>>) = db.withTransaction {
        dao.insertTxn(base.copy(id = 0, amount = myShare))
        for ((person, share) in others) {
            val iouId = dao.insertIou(
                Iou(person = person, amount = share, direction = IouDirection.THEY_OWE, note = base.note, date = base.date)
            )
            dao.insertTxn(
                Txn(
                    type = TxType.LEND, amount = share, accountId = base.accountId, date = base.date,
                    note = "Paid for $person" + if (base.note.isNotBlank()) " · ${base.note}" else "",
                    iouId = iouId,
                )
            )
        }
    }

    // ---------- Debts (V3) ----------
    suspend fun saveDebt(d: Debt) = dao.upsertDebt(d)
    suspend fun deleteDebt(d: Debt) = dao.deleteDebt(d)

    /** Lowers the debt's balance; when [accountId] is set, also logs the payment as an expense. */
    suspend fun recordDebtPayment(d: Debt, amount: Long, accountId: Long?) = db.withTransaction {
        dao.upsertDebt(d.copy(balance = (d.balance - amount).coerceAtLeast(0)))
        if (accountId != null) {
            val cat = dao.categoryByName("Debt payments", CategoryKind.EXPENSE.name)?.id
                ?: dao.insertCategory(
                    Category(name = "Debt payments", kind = CategoryKind.EXPENSE, icon = "bills", color = 0xFF8A948F, sortOrder = 99)
                )
            dao.insertTxn(
                Txn(type = TxType.EXPENSE, amount = amount, accountId = accountId, categoryId = cat,
                    note = "Debt payment · ${d.name}", date = Dates.today())
            )
        }
    }

    // ---------- Envelopes (V3) ----------
    suspend fun moveEnvelope(m: EnvelopeMove) = dao.insertEnvelopeMove(m)

    // ---------- Investments (V5) ----------
    suspend fun getHolding(id: Long) = dao.holding(id)
    suspend fun holdingsOnce() = dao.allHoldings()
    suspend fun investTxnForLedger(txnId: Long) = dao.investTxnByTxn(txnId)

    /** Saves a holding and, when its price is set or changed, remembers today's price. Returns the id. */
    suspend fun saveHolding(h: Holding): Long = db.withTransaction {
        val id = if (h.id == 0L) dao.insertHolding(h) else { dao.upsertHolding(h); h.id }
        if (h.price > 0) dao.upsertSnap(PriceSnap(id, Dates.today(), h.price))
        id
    }

    /** Deletes a holding with its history, and the account transactions it created. */
    suspend fun deleteHolding(h: Holding) = db.withTransaction {
        for (t in dao.investTxnsFor(h.id)) t.txnId?.let { dao.deleteTxnById(it) }
        dao.deleteInvestTxnsFor(h.id)
        dao.deleteSnapsFor(h.id)
        dao.deleteHolding(h)
    }

    suspend fun setPrice(holdingId: Long, price: Double) = db.withTransaction {
        dao.setHoldingPrice(holdingId, price, System.currentTimeMillis())
        dao.upsertSnap(PriceSnap(holdingId, Dates.today(), price))
    }

    /** Live prices from CoinGecko: coin id -> price. */
    suspend fun applyLivePrices(prices: Map<String, Double>) = db.withTransaction {
        val now = System.currentTimeMillis()
        val today = Dates.today()
        for (h in dao.allHoldings()) {
            if (!h.isLive) continue
            val p = prices[h.coinId] ?: continue
            dao.setHoldingPrice(h.id, p, now)
            dao.upsertSnap(PriceSnap(h.id, today, p))
        }
    }

    /**
     * Records a buy, sell or earning. When [ledger] is given, money also moved in or out of an account:
     * buying is INVEST (money out), selling is DIVEST (money in). Neither counts as spending or income.
     */
    suspend fun addInvestTxn(t: InvestTxn, ledger: Txn?) = db.withTransaction {
        val txnId = ledger?.let { dao.insertTxn(it) }
        dao.insertInvestTxn(t.copy(txnId = txnId))
    }

    /** Deletes a record and the account transaction it created, so balances go back. */
    suspend fun deleteInvestTxn(t: InvestTxn) = db.withTransaction {
        t.txnId?.let { dao.deleteTxnById(it) }
        dao.deleteInvestTxn(t)
    }
}
