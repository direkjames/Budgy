package com.budgy.app.data

import androidx.room.ColumnInfo
import com.budgy.app.i18n.L10n
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AccountType(val en: String) {
    CASH("Cash"), BANK("Bank"), EWALLET("E-wallet"), CARD("Credit card");

    val label: String get() = L10n.tr(en)
}

/**
 * LEND and COLLECT are money moving between you and other people (split bills, IOUs).
 * They change account balances but never count as spending or income.
 */
enum class TxType(val en: String) {
    EXPENSE("Expense"), INCOME("Income"), TRANSFER("Transfer"),
    LEND("Money out (IOU)"), COLLECT("Money in (IOU)"),
    /** V4: corrects an account balance. Amount is SIGNED (+ adds to the balance, − removes). */
    ADJUST("Balance adjustment"),
    /** V5: money moved from an account into an investment (buying crypto, MP2 savings). Not spending. */
    INVEST("Investment buy"),
    /** V5: money back from an investment into an account (selling crypto, MP2 withdrawal). Not income. */
    DIVEST("Investment sell");

    val label: String get() = L10n.tr(en)
    val isIou get() = this == LEND || this == COLLECT
    val isInvest get() = this == INVEST || this == DIVEST

    companion object {
        val entryTypes = listOf(EXPENSE, INCOME, TRANSFER)
    }
}

enum class CategoryKind(val en: String) {
    EXPENSE("Expense"), INCOME("Income");

    val label: String get() = L10n.tr(en)
}

enum class BillingCycle(val en: String) {
    WEEKLY("Weekly"), MONTHLY("Monthly"), YEARLY("Yearly"), CUSTOM("Custom");

    val label: String get() = L10n.tr(en)
}

enum class IouDirection(val en: String) {
    THEY_OWE("Owes you"), I_OWE("You owe");

    val label: String get() = L10n.tr(en)
}

/** All money values are stored as Long in cents (₱12.50 -> 1250). Dates are LocalDate epoch days. */

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val startingBalance: Long = 0,
    val color: Long,
    val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,          // V4
    @ColumnInfo(defaultValue = "''") val currency: String = "",             // V4: "" = your main currency
    @ColumnInfo(defaultValue = "1000000") val fxRateMicros: Long = 1_000_000, // V4: main-currency value of 1 unit × 1,000,000
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: CategoryKind,
    val icon: String,
    val color: Long,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "transactions",
    indices = [Index("date"), Index("accountId"), Index("categoryId")]
)
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val note: String = "",
    val date: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val subscriptionId: Long? = null,
    val recurringId: Long? = null,   // V2
    val iouId: Long? = null,         // V2
    @ColumnInfo(defaultValue = "''") val tags: String = "",   // V4: comma-separated, e.g. "Baguio trip,Food trip"
    val toAmount: Long? = null,      // V4: amount received in the destination account's currency (transfers)
)

@Entity(tableName = "budgets", indices = [Index(value = ["categoryId"], unique = true)])
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val monthlyLimit: Long,
    @ColumnInfo(defaultValue = "0") val startDate: Long = 0,  // V3: first day of the month it started (envelope rollover)
)

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    val cycle: BillingCycle,
    val customDays: Int = 30,
    val nextBillingDate: Long,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val isTrial: Boolean = false,
    val trialEndDate: Long? = null,
    val reminderDays: Int = 1,
    val color: Long,
    val note: String = "",
    val active: Boolean = true,
    @ColumnInfo(defaultValue = "1") val sharedWith: Int = 1,   // V2: people splitting the plan, including you
    val lastUsed: Long? = null,                                 // V2: epoch day you last used it
)

/** V2: a transaction that repeats (rent, salary, allowance). */
@Entity(tableName = "recurring")
data class Recurring(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val note: String = "",
    val cycle: BillingCycle,
    val customDays: Int = 30,
    val nextDate: Long,
    val autoLog: Boolean = true,
    val active: Boolean = true,
)

/** V2: something you're saving toward. Progress is the sum of its entries. */
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Long,
    val deadline: Long? = null,
    val icon: String = "savings",
    val color: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
)

@Entity(tableName = "goal_entries", indices = [Index("goalId")])
data class GoalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Long,   // negative = withdrawal
    val date: Long,
    val note: String = "",
)

/** V2: who owes who. */
@Entity(tableName = "ious")
data class Iou(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val amount: Long,
    val direction: IouDirection,
    val note: String = "",
    val date: Long,
    val settled: Boolean = false,
    val settledDate: Long? = null,
)

/** V2: history of a subscription's price. */
@Entity(tableName = "price_changes", indices = [Index("subscriptionId")])
data class PriceChange(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subscriptionId: Long,
    val oldAmount: Long,
    val newAmount: Long,
    val date: Long,
)

/** V3: a loan or card balance for the payoff planner. APR is in basis points (24.5% = 2450). */
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balance: Long,
    val aprBps: Int,
    val minPayment: Long,
    val color: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

/** V3: money moved from one envelope (budget category) to another in envelope mode. */
@Entity(tableName = "envelope_moves")
data class EnvelopeMove(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromCategoryId: Long,
    val toCategoryId: Long,
    val amount: Long,
    val date: Long,
)

// ---------------- Investments (V5) ----------------

enum class AssetKind(val en: String) {
    CRYPTO("Crypto"), MP2("Pag-IBIG MP2"),
    /** V6: index funds, ETFs and UITFs (e.g. S&P 500) with a price you type, optionally in US dollars. */
    FUND("Fund / ETF");

    val label: String get() = L10n.tr(en)
}

/** BUY = money in (buy crypto / MP2 savings), SELL = money out (sell / withdraw), EARN = rewards or dividends. */
enum class InvestType(val en: String) {
    BUY("Buy"), SELL("Sell"), EARN("Earned");

    val label: String get() = L10n.tr(en)
}

/**
 * Something you invest in. [price] is the value of ONE unit in your main currency (not cents).
 * Crypto with a [coinId] gets live prices from CoinGecko; an empty coinId means you type the price.
 * MP2 is tracked in pesos: 1 unit = 1 peso, so the price is always 1.
 */
@Entity(tableName = "holdings")
data class Holding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val symbol: String = "",
    val kind: AssetKind,
    val coinId: String = "",
    val price: Double = 0.0,
    val priceUpdated: Long = 0,          // epoch millis
    val color: Long,
    val rateBps: Int = 0,                // MP2: expected yearly dividend, 7.12% = 712
    val maturityDate: Long? = null,      // MP2: epoch day
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /** V6 (funds): currency the price is quoted in, e.g. "$". "" = your main currency. */
    @ColumnInfo(defaultValue = "''") val priceCurrency: String = "",
    /** V6 (funds): main-currency value of 1 unit of [priceCurrency] × 1,000,000 (e.g. 1 $ = ₱56.20). */
    @ColumnInfo(defaultValue = "1000000") val fxRateMicros: Long = 1_000_000,
    /** V6 (funds): price of one unit in [priceCurrency]. [price] is this converted to your main currency. */
    @ColumnInfo(defaultValue = "0") val quotePrice: Double = 0.0,
)

/** Crypto that gets its price from CoinGecko (kept outside the class so Room doesn't treat it as a column). */
val Holding.isLive: Boolean get() = kind == AssetKind.CRYPTO && coinId.isNotEmpty()

/**
 * One buy, sell or earning. [amount] is money in cents in your main currency
 * (what you paid, what you received, or for MP2 the dividend). [txnId] links to the account
 * transaction when money moved in or out of one of your accounts.
 */
@Entity(tableName = "invest_txns", indices = [Index("holdingId")])
data class InvestTxn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val holdingId: Long,
    val type: InvestType,
    val units: Double,
    val amount: Long,
    val date: Long,
    val accountId: Long? = null,
    val txnId: Long? = null,
    val note: String = "",
)

/** The price of a holding on a day, so net worth history can use the price at the time. */
@Entity(tableName = "price_snaps", primaryKeys = ["holdingId", "date"])
data class PriceSnap(
    val holdingId: Long,
    val date: Long,
    val price: Double,
)
