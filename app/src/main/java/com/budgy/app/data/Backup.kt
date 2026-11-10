package com.budgy.app.data

import com.budgy.app.i18n.tr
import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.budgy.app.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Full backup to a JSON file the user picks (Google Drive, Downloads, etc.), restore from it,
 * and a CSV export of transactions for spreadsheets.
 */
object Backup {
    private const val FORMAT = "budgy-backup"
    private const val VERSION = 6

    suspend fun exportJson(context: Context, repo: BudgyRepository, uri: Uri) = withContext(Dispatchers.IO) {
        val d = repo.dao
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedOn", LocalDate.now().toString())
            .put("accounts", JSONArray(d.allAccounts().map { it.toJson() }))
            .put("categories", JSONArray(d.allCategories().map { it.toJson() }))
            .put("transactions", JSONArray(d.allTxns().map { it.toJson() }))
            .put("budgets", JSONArray(d.allBudgets().map { it.toJson() }))
            .put("subscriptions", JSONArray(d.subscriptionsOnce().map { it.toJson() }))
            .put("recurring", JSONArray(d.allRecurring().map { it.toJson() }))
            .put("goals", JSONArray(d.allGoals().map { it.toJson() }))
            .put("goalEntries", JSONArray(d.allGoalEntries().map { it.toJson() }))
            .put("ious", JSONArray(d.allIous().map { it.toJson() }))
            .put("priceChanges", JSONArray(d.allPriceChanges().map { it.toJson() }))
            .put("debts", JSONArray(d.allDebts().map { it.toJson() }))
            .put("envelopeMoves", JSONArray(d.allEnvelopeMoves().map { it.toJson() }))
            .put("holdings", JSONArray(d.allHoldings().map { it.toJson() }))
            .put("investTxns", JSONArray(d.allInvestTxns().map { it.toJson() }))
            .put("priceSnaps", JSONArray(d.allPriceSnaps().map { it.toJson() }))
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(root.toString(2).toByteArray(Charsets.UTF_8))
        } ?: error(tr("Can't write to that file"))
    }

    /** Replaces ALL current data with the backup. Returns the number of transactions restored. */
    suspend fun importJson(context: Context, repo: BudgyRepository, uri: Uri): Int = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error(tr("Can't open that file"))
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT) { tr("That isn't a Budgy backup file") }

        fun arr(name: String): List<JSONObject> {
            val a = root.optJSONArray(name) ?: return emptyList()
            return (0 until a.length()).map { a.getJSONObject(it) }
        }

        // Parse everything first, so a broken file can't leave you with half your data.
        val accounts = arr("accounts").map { it.toAccount() }
        val categories = arr("categories").map { it.toCategory() }
        val txns = arr("transactions").map { it.toTxn() }
        val budgets = arr("budgets").map { it.toBudget() }
        val subs = arr("subscriptions").map { it.toSubscription() }
        val recurring = arr("recurring").map { it.toRecurring() }
        val goals = arr("goals").map { it.toGoal() }
        val goalEntries = arr("goalEntries").map { it.toGoalEntry() }
        val ious = arr("ious").map { it.toIou() }
        val prices = arr("priceChanges").map { it.toPriceChange() }
        val debts = arr("debts").map { it.toDebt() }
        val moves = arr("envelopeMoves").map { it.toEnvelopeMove() }
        val holdings = arr("holdings").map { it.toHolding() }
        val investTxns = arr("investTxns").map { it.toInvestTxn() }
        val snaps = arr("priceSnaps").map { it.toPriceSnap() }

        val d = repo.dao
        repo.db.withTransaction {
            d.wipeTxns(); d.wipeBudgets(); d.wipePriceChanges(); d.wipeSubscriptions(); d.wipeRecurring()
            d.wipeGoalEntries(); d.wipeGoals(); d.wipeIous(); d.wipeCategories(); d.wipeAccounts()
            d.wipeDebts(); d.wipeEnvelopeMoves()
            d.wipeHoldings(); d.wipeInvestTxns(); d.wipePriceSnaps()
            d.restoreAccounts(accounts)
            d.restoreCategories(categories)
            d.restoreTxns(txns)
            d.restoreBudgets(budgets)
            d.restoreSubscriptions(subs)
            d.restoreRecurring(recurring)
            d.restoreGoals(goals)
            d.restoreGoalEntries(goalEntries)
            d.restoreIous(ious)
            d.restorePriceChanges(prices)
            d.restoreDebts(debts)
            d.restoreEnvelopeMoves(moves)
            d.restoreHoldings(holdings)
            d.restoreInvestTxns(investTxns)
            d.restorePriceSnaps(snaps)
        }
        txns.size
    }

    /** Spreadsheet-friendly list of every transaction. Returns how many rows were written. */
    suspend fun exportCsv(context: Context, repo: BudgyRepository, uri: Uri): Int = withContext(Dispatchers.IO) {
        val d = repo.dao
        val accounts = d.allAccounts().associateBy { it.id }
        val cats = d.allCategories().associateBy { it.id }
        val txns = d.allTxns()
        val sb = StringBuilder("Date,Type,Amount,Currency,Category,Account,To account,Note,Tags\n")
        for (t in txns) {
            val signed = when (t.type) {
                TxType.EXPENSE, TxType.LEND, TxType.INVEST -> -t.amount
                else -> t.amount
            }
            sb.append(LocalDate.ofEpochDay(t.date)).append(',')
                .append(t.type.en).append(',')
                .append(Money.toInput(signed)).append(',')
                .append(csv(accounts[t.accountId]?.currency?.ifEmpty { null } ?: "")).append(',')
                .append(csv(t.categoryId?.let { cats[it]?.name } ?: "")).append(',')
                .append(csv(accounts[t.accountId]?.name ?: "")).append(',')
                .append(csv(t.toAccountId?.let { accounts[it]?.name } ?: "")).append(',')
                .append(csv(t.note)).append(',')
                .append(csv(t.tags.replace(",", "; "))).append('\n')
        }
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            // BOM so Excel opens ₱ and other symbols correctly
            out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        } ?: error(tr("Can't write to that file"))
        txns.size
    }

    private fun csv(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    // ---------- JSON mapping ----------
    private fun JSONObject.optLongOrNull(k: String): Long? = if (has(k) && !isNull(k)) getLong(k) else null
    private fun JSONObject.putOpt2(k: String, v: Any?): JSONObject = put(k, v ?: JSONObject.NULL)

    private fun Account.toJson() = JSONObject().put("id", id).put("name", name).put("type", type.name)
        .put("startingBalance", startingBalance).put("color", color).put("sortOrder", sortOrder)
        .put("archived", archived).put("currency", currency).put("fxRateMicros", fxRateMicros)
    private fun JSONObject.toAccount() = Account(
        id = getLong("id"), name = getString("name"), type = AccountType.valueOf(getString("type")),
        startingBalance = getLong("startingBalance"), color = getLong("color"), sortOrder = optInt("sortOrder"),
        archived = optBoolean("archived"), currency = optString("currency", ""), fxRateMicros = optLong("fxRateMicros", 1_000_000),
    )

    private fun Category.toJson() = JSONObject().put("id", id).put("name", name).put("kind", kind.name)
        .put("icon", icon).put("color", color).put("sortOrder", sortOrder)
    private fun JSONObject.toCategory() = Category(
        id = getLong("id"), name = getString("name"), kind = CategoryKind.valueOf(getString("kind")),
        icon = getString("icon"), color = getLong("color"), sortOrder = optInt("sortOrder"),
    )

    private fun Txn.toJson() = JSONObject().put("id", id).put("type", type.name).put("amount", amount)
        .put("accountId", accountId).putOpt2("toAccountId", toAccountId).putOpt2("categoryId", categoryId)
        .put("note", note).put("date", date).put("createdAt", createdAt)
        .putOpt2("subscriptionId", subscriptionId).putOpt2("recurringId", recurringId).putOpt2("iouId", iouId)
        .put("tags", tags).putOpt2("toAmount", toAmount)
    private fun JSONObject.toTxn() = Txn(
        id = getLong("id"), type = TxType.valueOf(getString("type")), amount = getLong("amount"),
        accountId = getLong("accountId"), toAccountId = optLongOrNull("toAccountId"),
        categoryId = optLongOrNull("categoryId"), note = optString("note"), date = getLong("date"),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        subscriptionId = optLongOrNull("subscriptionId"), recurringId = optLongOrNull("recurringId"),
        iouId = optLongOrNull("iouId"),
        tags = optString("tags", ""), toAmount = optLongOrNull("toAmount"),
    )

    private fun Budget.toJson() = JSONObject().put("id", id).put("categoryId", categoryId).put("monthlyLimit", monthlyLimit).put("startDate", startDate)
    private fun JSONObject.toBudget() = Budget(id = getLong("id"), categoryId = getLong("categoryId"), monthlyLimit = getLong("monthlyLimit"), startDate = optLong("startDate", 0))

    private fun Subscription.toJson() = JSONObject().put("id", id).put("name", name).put("amount", amount)
        .put("cycle", cycle.name).put("customDays", customDays).put("nextBillingDate", nextBillingDate)
        .putOpt2("accountId", accountId).putOpt2("categoryId", categoryId).put("isTrial", isTrial)
        .putOpt2("trialEndDate", trialEndDate).put("reminderDays", reminderDays).put("color", color)
        .put("note", note).put("active", active).put("sharedWith", sharedWith).putOpt2("lastUsed", lastUsed)
    private fun JSONObject.toSubscription() = Subscription(
        id = getLong("id"), name = getString("name"), amount = getLong("amount"),
        cycle = BillingCycle.valueOf(getString("cycle")), customDays = optInt("customDays", 30),
        nextBillingDate = getLong("nextBillingDate"), accountId = optLongOrNull("accountId"),
        categoryId = optLongOrNull("categoryId"), isTrial = optBoolean("isTrial"),
        trialEndDate = optLongOrNull("trialEndDate"), reminderDays = optInt("reminderDays", 1),
        color = getLong("color"), note = optString("note"), active = optBoolean("active", true),
        sharedWith = optInt("sharedWith", 1), lastUsed = optLongOrNull("lastUsed"),
    )

    private fun Recurring.toJson() = JSONObject().put("id", id).put("type", type.name).put("amount", amount)
        .put("accountId", accountId).putOpt2("toAccountId", toAccountId).putOpt2("categoryId", categoryId)
        .put("note", note).put("cycle", cycle.name).put("customDays", customDays).put("nextDate", nextDate)
        .put("autoLog", autoLog).put("active", active)
    private fun JSONObject.toRecurring() = Recurring(
        id = getLong("id"), type = TxType.valueOf(getString("type")), amount = getLong("amount"),
        accountId = getLong("accountId"), toAccountId = optLongOrNull("toAccountId"),
        categoryId = optLongOrNull("categoryId"), note = optString("note"),
        cycle = BillingCycle.valueOf(getString("cycle")), customDays = optInt("customDays", 30),
        nextDate = getLong("nextDate"), autoLog = optBoolean("autoLog", true), active = optBoolean("active", true),
    )

    private fun Goal.toJson() = JSONObject().put("id", id).put("name", name).put("target", target)
        .putOpt2("deadline", deadline).put("icon", icon).put("color", color).put("createdAt", createdAt)
        .put("archived", archived)
    private fun JSONObject.toGoal() = Goal(
        id = getLong("id"), name = getString("name"), target = getLong("target"), deadline = optLongOrNull("deadline"),
        icon = optString("icon", "savings"), color = getLong("color"),
        createdAt = optLong("createdAt", System.currentTimeMillis()), archived = optBoolean("archived"),
    )

    private fun GoalEntry.toJson() = JSONObject().put("id", id).put("goalId", goalId).put("amount", amount)
        .put("date", date).put("note", note)
    private fun JSONObject.toGoalEntry() = GoalEntry(
        id = getLong("id"), goalId = getLong("goalId"), amount = getLong("amount"), date = getLong("date"), note = optString("note"),
    )

    private fun Iou.toJson() = JSONObject().put("id", id).put("person", person).put("amount", amount)
        .put("direction", direction.name).put("note", note).put("date", date).put("settled", settled)
        .putOpt2("settledDate", settledDate)
    private fun JSONObject.toIou() = Iou(
        id = getLong("id"), person = getString("person"), amount = getLong("amount"),
        direction = IouDirection.valueOf(getString("direction")), note = optString("note"), date = getLong("date"),
        settled = optBoolean("settled"), settledDate = optLongOrNull("settledDate"),
    )

    private fun PriceChange.toJson() = JSONObject().put("id", id).put("subscriptionId", subscriptionId)
        .put("oldAmount", oldAmount).put("newAmount", newAmount).put("date", date)
    private fun JSONObject.toPriceChange() = PriceChange(
        id = getLong("id"), subscriptionId = getLong("subscriptionId"), oldAmount = getLong("oldAmount"),
        newAmount = getLong("newAmount"), date = getLong("date"),
    )

    private fun Debt.toJson() = JSONObject().put("id", id).put("name", name).put("balance", balance)
        .put("aprBps", aprBps).put("minPayment", minPayment).put("color", color).put("createdAt", createdAt)
    private fun JSONObject.toDebt() = Debt(
        id = getLong("id"), name = getString("name"), balance = getLong("balance"), aprBps = getInt("aprBps"),
        minPayment = getLong("minPayment"), color = getLong("color"), createdAt = optLong("createdAt", System.currentTimeMillis()),
    )

    private fun EnvelopeMove.toJson() = JSONObject().put("id", id).put("fromCategoryId", fromCategoryId)
        .put("toCategoryId", toCategoryId).put("amount", amount).put("date", date)
    private fun JSONObject.toEnvelopeMove() = EnvelopeMove(
        id = getLong("id"), fromCategoryId = getLong("fromCategoryId"), toCategoryId = getLong("toCategoryId"),
        amount = getLong("amount"), date = getLong("date"),
    )

    private fun Holding.toJson() = JSONObject().put("id", id).put("name", name).put("symbol", symbol)
        .put("kind", kind.name).put("coinId", coinId).put("price", price).put("priceUpdated", priceUpdated)
        .put("color", color).put("rateBps", rateBps).putOpt2("maturityDate", maturityDate).put("note", note)
        .put("createdAt", createdAt).put("priceCurrency", priceCurrency).put("fxRateMicros", fxRateMicros)
        .put("quotePrice", quotePrice)
    private fun JSONObject.toHolding() = Holding(
        id = getLong("id"), name = getString("name"), symbol = optString("symbol"),
        kind = AssetKind.valueOf(getString("kind")), coinId = optString("coinId"),
        price = optDouble("price", 0.0).takeIf { !it.isNaN() } ?: 0.0, priceUpdated = optLong("priceUpdated"),
        color = getLong("color"), rateBps = optInt("rateBps"), maturityDate = optLongOrNull("maturityDate"),
        note = optString("note"), createdAt = optLong("createdAt", System.currentTimeMillis()),
        priceCurrency = optString("priceCurrency", ""), fxRateMicros = optLong("fxRateMicros", 1_000_000),
        quotePrice = optDouble("quotePrice", 0.0).takeIf { !it.isNaN() } ?: 0.0,
    )

    private fun InvestTxn.toJson() = JSONObject().put("id", id).put("holdingId", holdingId).put("type", type.name)
        .put("units", units).put("amount", amount).put("date", date).putOpt2("accountId", accountId)
        .putOpt2("txnId", txnId).put("note", note)
    private fun JSONObject.toInvestTxn() = InvestTxn(
        id = getLong("id"), holdingId = getLong("holdingId"), type = InvestType.valueOf(getString("type")),
        units = getDouble("units"), amount = getLong("amount"), date = getLong("date"),
        accountId = optLongOrNull("accountId"), txnId = optLongOrNull("txnId"), note = optString("note"),
    )

    private fun PriceSnap.toJson() = JSONObject().put("holdingId", holdingId).put("date", date).put("price", price)
    private fun JSONObject.toPriceSnap() = PriceSnap(holdingId = getLong("holdingId"), date = getLong("date"), price = getDouble("price"))
}
