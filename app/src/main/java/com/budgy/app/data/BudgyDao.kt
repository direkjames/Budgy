package com.budgy.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgyDao {
    // Accounts
    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    fun accounts(): Flow<List<Account>>

    @Upsert
    suspend fun upsertAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :id OR toAccountId = :id")
    suspend fun txCountForAccount(id: Long): Int

    @Query("SELECT id FROM accounts ORDER BY sortOrder, id LIMIT 1")
    suspend fun firstAccountId(): Long?

    @Query("UPDATE subscriptions SET accountId = NULL WHERE accountId = :id")
    suspend fun clearSubscriptionAccount(id: Long)

    // Categories
    @Query("SELECT * FROM categories ORDER BY kind, sortOrder, id")
    fun categories(): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Insert
    suspend fun insertCategories(list: List<Category>)

    @Upsert
    suspend fun upsertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :id")
    suspend fun clearTxnCategory(id: Long)

    @Query("UPDATE subscriptions SET categoryId = NULL WHERE categoryId = :id")
    suspend fun clearSubscriptionCategory(id: Long)

    @Query("DELETE FROM budgets WHERE categoryId = :id")
    suspend fun deleteBudgetForCategory(id: Long)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun transactions(): Flow<List<Txn>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun txn(id: Long): Txn?

    @Upsert
    suspend fun upsertTxn(txn: Txn)

    @Delete
    suspend fun deleteTxn(txn: Txn)

    // Budgets
    @Query("SELECT * FROM budgets")
    fun budgets(): Flow<List<Budget>>

    @Upsert
    suspend fun upsertBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    // Subscriptions
    @Query("SELECT * FROM subscriptions ORDER BY nextBillingDate, name")
    fun subscriptions(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions")
    suspend fun subscriptionsOnce(): List<Subscription>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun subscription(id: Long): Subscription?

    @Upsert
    suspend fun upsertSubscription(subscription: Subscription)

    @Delete
    suspend fun deleteSubscription(subscription: Subscription)

    @Query("DELETE FROM price_changes WHERE subscriptionId = :id")
    suspend fun deletePriceChangesFor(id: Long)

    // ---------- V2 ----------

    @Insert
    suspend fun insertTxn(txn: Txn): Long

    // Recurring
    @Query("SELECT * FROM recurring ORDER BY nextDate, id")
    fun recurring(): Flow<List<Recurring>>

    @Query("SELECT * FROM recurring WHERE active = 1 AND autoLog = 1 AND nextDate <= :today")
    suspend fun dueAutoRecurring(today: Long): List<Recurring>

    @Query("SELECT * FROM recurring WHERE id = :id")
    suspend fun recurringById(id: Long): Recurring?

    @Upsert
    suspend fun upsertRecurring(r: Recurring)

    @Insert
    suspend fun insertRecurring(r: Recurring): Long

    @Delete
    suspend fun deleteRecurring(r: Recurring)

    // Goals
    @Query("SELECT * FROM goals ORDER BY archived, createdAt")
    fun goals(): Flow<List<Goal>>

    @Upsert
    suspend fun upsertGoal(g: Goal)

    @Delete
    suspend fun deleteGoal(g: Goal)

    @Query("DELETE FROM goal_entries WHERE goalId = :goalId")
    suspend fun deleteEntriesForGoal(goalId: Long)

    @Query("SELECT * FROM goal_entries ORDER BY date DESC, id DESC")
    fun goalEntries(): Flow<List<GoalEntry>>

    @Insert
    suspend fun insertGoalEntry(e: GoalEntry)

    @Delete
    suspend fun deleteGoalEntry(e: GoalEntry)

    // IOUs
    @Query("SELECT * FROM ious ORDER BY settled, date DESC, id DESC")
    fun ious(): Flow<List<Iou>>

    @Insert
    suspend fun insertIou(i: Iou): Long

    @Upsert
    suspend fun upsertIou(i: Iou)

    @Delete
    suspend fun deleteIou(i: Iou)

    // Price history
    @Query("SELECT * FROM price_changes ORDER BY date DESC, id DESC")
    fun priceChanges(): Flow<List<PriceChange>>

    @Insert
    suspend fun insertPriceChange(p: PriceChange)

    // ---------- V3 ----------
    @Query("SELECT * FROM debts ORDER BY createdAt")
    fun debts(): Flow<List<Debt>>

    @Upsert
    suspend fun upsertDebt(d: Debt)

    @Delete
    suspend fun deleteDebt(d: Debt)

    @Query("SELECT * FROM envelope_moves ORDER BY date DESC, id DESC")
    fun envelopeMoves(): Flow<List<EnvelopeMove>>

    @Insert
    suspend fun insertEnvelopeMove(m: EnvelopeMove)

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE AND kind = :kind LIMIT 1")
    suspend fun categoryByName(name: String, kind: String): Category?

    @Insert
    suspend fun insertCategory(c: Category): Long

    @Insert
    suspend fun insertAccount(a: Account): Long

    // ---------- Backup ----------
    @Query("SELECT * FROM accounts") suspend fun allAccounts(): List<Account>
    @Query("SELECT * FROM categories") suspend fun allCategories(): List<Category>
    @Query("SELECT * FROM transactions ORDER BY date, createdAt") suspend fun allTxns(): List<Txn>
    @Query("SELECT * FROM budgets") suspend fun allBudgets(): List<Budget>
    @Query("SELECT * FROM recurring") suspend fun allRecurring(): List<Recurring>
    @Query("SELECT * FROM goals") suspend fun allGoals(): List<Goal>
    @Query("SELECT * FROM goal_entries") suspend fun allGoalEntries(): List<GoalEntry>
    @Query("SELECT * FROM ious") suspend fun allIous(): List<Iou>
    @Query("SELECT * FROM price_changes") suspend fun allPriceChanges(): List<PriceChange>
    @Query("SELECT * FROM debts") suspend fun allDebts(): List<Debt>
    @Query("SELECT * FROM envelope_moves") suspend fun allEnvelopeMoves(): List<EnvelopeMove>

    @Query("DELETE FROM accounts") suspend fun wipeAccounts()
    @Query("DELETE FROM categories") suspend fun wipeCategories()
    @Query("DELETE FROM transactions") suspend fun wipeTxns()
    @Query("DELETE FROM budgets") suspend fun wipeBudgets()
    @Query("DELETE FROM subscriptions") suspend fun wipeSubscriptions()
    @Query("DELETE FROM recurring") suspend fun wipeRecurring()
    @Query("DELETE FROM goals") suspend fun wipeGoals()
    @Query("DELETE FROM goal_entries") suspend fun wipeGoalEntries()
    @Query("DELETE FROM ious") suspend fun wipeIous()
    @Query("DELETE FROM price_changes") suspend fun wipePriceChanges()
    @Query("DELETE FROM debts") suspend fun wipeDebts()
    @Query("DELETE FROM envelope_moves") suspend fun wipeEnvelopeMoves()

    @Insert suspend fun restoreAccounts(list: List<Account>)
    @Insert suspend fun restoreCategories(list: List<Category>)
    @Insert suspend fun restoreTxns(list: List<Txn>)
    @Insert suspend fun restoreBudgets(list: List<Budget>)
    @Insert suspend fun restoreSubscriptions(list: List<Subscription>)
    @Insert suspend fun restoreRecurring(list: List<Recurring>)
    @Insert suspend fun restoreGoals(list: List<Goal>)
    @Insert suspend fun restoreGoalEntries(list: List<GoalEntry>)
    @Insert suspend fun restoreIous(list: List<Iou>)
    @Insert suspend fun restorePriceChanges(list: List<PriceChange>)
    @Insert suspend fun restoreDebts(list: List<Debt>)
    @Insert suspend fun restoreEnvelopeMoves(list: List<EnvelopeMove>)

    // ---------- Investments (V5) ----------
    @Query("SELECT * FROM holdings ORDER BY createdAt, id")
    fun holdings(): Flow<List<Holding>>

    @Query("SELECT * FROM holdings WHERE id = :id")
    suspend fun holding(id: Long): Holding?

    @Insert
    suspend fun insertHolding(h: Holding): Long

    @Upsert
    suspend fun upsertHolding(h: Holding)

    @Delete
    suspend fun deleteHolding(h: Holding)

    @Query("UPDATE holdings SET price = :price, priceUpdated = :at WHERE id = :id")
    suspend fun setHoldingPrice(id: Long, price: Double, at: Long)

    @Query("SELECT * FROM invest_txns ORDER BY date DESC, id DESC")
    fun investTxns(): Flow<List<InvestTxn>>

    @Query("SELECT * FROM invest_txns WHERE holdingId = :holdingId")
    suspend fun investTxnsFor(holdingId: Long): List<InvestTxn>

    @Query("SELECT * FROM invest_txns WHERE txnId = :txnId LIMIT 1")
    suspend fun investTxnByTxn(txnId: Long): InvestTxn?

    @Insert
    suspend fun insertInvestTxn(t: InvestTxn): Long

    @Delete
    suspend fun deleteInvestTxn(t: InvestTxn)

    @Query("DELETE FROM invest_txns WHERE holdingId = :holdingId")
    suspend fun deleteInvestTxnsFor(holdingId: Long)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTxnById(id: Long)

    @Query("SELECT * FROM price_snaps ORDER BY date")
    fun priceSnaps(): Flow<List<PriceSnap>>

    @Upsert
    suspend fun upsertSnap(s: PriceSnap)

    @Query("DELETE FROM price_snaps WHERE holdingId = :holdingId")
    suspend fun deleteSnapsFor(holdingId: Long)

    @Query("SELECT * FROM holdings") suspend fun allHoldings(): List<Holding>
    @Query("SELECT * FROM invest_txns") suspend fun allInvestTxns(): List<InvestTxn>
    @Query("SELECT * FROM price_snaps") suspend fun allPriceSnaps(): List<PriceSnap>
    @Query("DELETE FROM holdings") suspend fun wipeHoldings()
    @Query("DELETE FROM invest_txns") suspend fun wipeInvestTxns()
    @Query("DELETE FROM price_snaps") suspend fun wipePriceSnaps()
    @Insert suspend fun restoreHoldings(list: List<Holding>)
    @Insert suspend fun restoreInvestTxns(list: List<InvestTxn>)
    @Insert suspend fun restorePriceSnaps(list: List<PriceSnap>)
}
