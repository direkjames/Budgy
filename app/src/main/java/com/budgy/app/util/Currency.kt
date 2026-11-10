package com.budgy.app.util

import com.budgy.app.data.Account
import com.budgy.app.data.TxType
import com.budgy.app.data.Txn
import kotlin.math.roundToLong

/*
 * Multi-currency (V4): each account can hold a different currency with a rate you set
 * ("1 USD = ₱56.20"). Rates are typed in, not downloaded.
 * Totals, budgets and reports are always shown in your main currency.
 */

fun Account.isForeign(home: String): Boolean = currency.isNotEmpty() && currency != home

/** The symbol amounts in this account are shown with. */
fun Account.symbol(home: String): String = if (isForeign(home)) currency else home

/** Converts an amount held in this account to your main currency. */
fun Account.toHome(amount: Long, home: String): Long =
    if (!isForeign(home)) amount else (amount * (fxRateMicros / 1_000_000.0)).roundToLong()

fun rateText(micros: Long): String {
    val v = micros / 1_000_000.0
    return if (v >= 1) String.format(java.util.Locale.US, "%,.2f", v) else String.format(java.util.Locale.US, "%.4f", v)
}

/** Parses "56.2" → 56_200_000 micros. */
fun parseRate(text: String): Long? = text.replace(",", "").trim().toDoubleOrNull()
    ?.takeIf { it > 0 && it < 1_000_000 }?.let { (it * 1_000_000).roundToLong() }

/**
 * Same transactions with amounts converted to the main currency, for anything that adds money up
 * across accounts (budgets, reports, streak-free stats, widget). When nothing is foreign, returns the list as is.
 */
fun toHomeTxns(txns: List<Txn>, accounts: List<Account>, home: String): List<Txn> {
    val foreign = accounts.filter { it.isForeign(home) }.associateBy { it.id }
    if (foreign.isEmpty()) return txns
    return txns.map { t ->
        val a = foreign[t.accountId] ?: return@map t
        t.copy(amount = a.toHome(t.amount, home))
    }
}

/** Current balance per account, in each account's own currency. */
fun computeBalances(accounts: List<Account>, txns: List<Txn>): Map<Long, Long> {
    val m = accounts.associate { it.id to it.startingBalance }.toMutableMap()
    for (t in txns) {
        when (t.type) {
            TxType.EXPENSE, TxType.LEND, TxType.INVEST -> m[t.accountId] = (m[t.accountId] ?: 0) - t.amount
            TxType.INCOME, TxType.COLLECT, TxType.DIVEST -> m[t.accountId] = (m[t.accountId] ?: 0) + t.amount
            TxType.ADJUST -> m[t.accountId] = (m[t.accountId] ?: 0) + t.amount
            TxType.TRANSFER -> {
                m[t.accountId] = (m[t.accountId] ?: 0) - t.amount
                t.toAccountId?.let { to -> m[to] = (m[to] ?: 0) + (t.toAmount ?: t.amount) }
            }
        }
    }
    return m
}

/** Balances converted to the main currency. */
fun homeBalances(accounts: List<Account>, balances: Map<Long, Long>, home: String): Map<Long, Long> =
    accounts.associate { a -> a.id to a.toHome(balances[a.id] ?: a.startingBalance, home) }
