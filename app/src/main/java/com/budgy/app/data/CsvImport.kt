package com.budgy.app.data

import com.budgy.app.i18n.tr
import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ImportResult(val imported: Int, val duplicates: Int, val skipped: Int, val newAccounts: Int, val newCategories: Int)

/**
 * Imports transactions from a CSV file. Works with Budgy's own export, and with most bank/e-wallet
 * exports: it looks for date, amount (or debit/credit), description/note, category, account and type
 * columns by name. Nothing is deleted; rows already in Budgy are skipped as duplicates.
 */
object CsvImport {

    private val dateFormats = listOf(
        "yyyy-MM-dd", "yyyy/MM/dd", "M/d/yyyy", "MM/dd/yyyy", "d/M/yyyy", "dd/MM/yyyy",
        "M/d/yy", "MMM d, yyyy", "MMM d yyyy", "d MMM yyyy", "dd-MMM-yyyy", "MM-dd-yyyy", "yyyyMMdd",
    ).map { DateTimeFormatter.ofPattern(it, Locale.US) }

    suspend fun import(context: Context, repo: BudgyRepository, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error(tr("Can't open that file"))
        val rows = parse(text.removePrefix("﻿")).filter { r -> r.any { it.isNotBlank() } }
        require(rows.size >= 2) { tr("The file has no rows to import") }

        val header = rows.first().map { it.trim().lowercase() }
        fun col(vararg names: String): Int = header.indexOfFirst { h -> names.any { h == it } }
            .takeIf { it >= 0 } ?: header.indexOfFirst { h -> names.any { h.contains(it) } }

        val cDate = col("date", "transaction date", "posting date", "posted")
        val cAmount = col("amount", "value")
        val cDebit = col("debit", "withdrawal", "money out")
        val cCredit = col("credit", "deposit", "money in")
        val cType = col("type")
        val cCategory = col("category")
        val cAccount = col("account", "from account", "wallet")
        val cTo = col("to account").takeIf { it >= 0 } ?: header.indexOf("to")
        val cNote = col("note", "description", "memo", "details", "particulars", "remarks", "payee")
        val cTags = header.indexOfFirst { it == "tags" || it == "tag" }
        require(cDate >= 0) { tr("Couldn't find a Date column") }
        require(cAmount >= 0 || cDebit >= 0 || cCredit >= 0) { tr("Couldn't find an Amount (or Debit/Credit) column") }

        val dao = repo.dao
        var imported = 0
        var dupes = 0
        var skipped = 0
        var newAccounts = 0
        var newCategories = 0

        repo.db.withTransaction {
            val accounts = dao.allAccounts().associateBy { it.name.lowercase() }.toMutableMap()
            val categories = dao.allCategories().toMutableList()
            val existing = dao.allTxns().map { key(it) }.toMutableSet()

            suspend fun account(name: String?): Long {
                val n = name?.trim().orEmpty().ifEmpty { "Imported" }
                accounts[n.lowercase()]?.let { return it.id }
                // No account named in the file → use your first account rather than inventing one.
                if (name.isNullOrBlank()) accounts.values.firstOrNull()?.let { return it.id }
                val a = Account(
                    name = n, type = AccountType.BANK,
                    color = DefaultData.swatches[(accounts.size + 3) % DefaultData.swatches.size],
                    sortOrder = accounts.size,
                )
                val id = dao.insertAccount(a)
                accounts[n.lowercase()] = a.copy(id = id)
                newAccounts++
                return id
            }

            suspend fun category(name: String?, kind: CategoryKind): Long? {
                val n = name?.trim().orEmpty()
                if (n.isEmpty() || n.equals("uncategorized", true)) return null
                categories.firstOrNull { it.kind == kind && it.name.equals(n, true) }?.let { return it.id }
                val c = Category(name = n, kind = kind, icon = "other", color = 0xFF8A948F, sortOrder = 50 + categories.size)
                val id = dao.insertCategory(c)
                categories += c.copy(id = id)
                newCategories++
                return id
            }

            for (r in rows.drop(1)) {
                fun cell(i: Int) = if (i in r.indices) r[i].trim() else ""
                val date = parseDate(cell(cDate))
                val signed: Long? = when {
                    cAmount >= 0 && cell(cAmount).isNotEmpty() -> parseAmount(cell(cAmount))
                    else -> {
                        val credit: Long = if (cCredit >= 0) parseAmount(cell(cCredit)) ?: 0L else 0L
                        val debit: Long = if (cDebit >= 0) parseAmount(cell(cDebit)) ?: 0L else 0L
                        if (credit == 0L && debit == 0L) null else kotlin.math.abs(credit) - kotlin.math.abs(debit)
                    }
                }
                if (date == null || signed == null || signed == 0L) { skipped++; continue }

                val typeText = cell(cType).lowercase()
                val type = when {
                    typeText.isEmpty() -> if (signed < 0) TxType.EXPENSE else TxType.INCOME
                    typeText.startsWith("transfer") -> TxType.TRANSFER
                    typeText.startsWith("balance adj") || typeText == "adjust" -> TxType.ADJUST
                    typeText.startsWith("investment buy") -> TxType.INVEST
                    typeText.startsWith("investment sell") -> TxType.DIVEST
                    typeText.contains("money out") || typeText == "lend" -> TxType.LEND
                    typeText.contains("money in") || typeText == "collect" -> TxType.COLLECT
                    typeText.startsWith("income") || typeText == "credit" || typeText == "deposit" -> TxType.INCOME
                    typeText.startsWith("expense") || typeText == "debit" || typeText == "withdrawal" -> TxType.EXPENSE
                    else -> if (signed < 0) TxType.EXPENSE else TxType.INCOME
                }
                val accountId = account(cell(cAccount).ifEmpty { null })
                val toAccountId = if (type == TxType.TRANSFER) {
                    val toName = cell(cTo)
                    if (toName.isEmpty()) { skipped++; continue }
                    account(toName)
                } else null
                val categoryId = when (type) {
                    TxType.EXPENSE -> category(cell(cCategory), CategoryKind.EXPENSE)
                    TxType.INCOME -> category(cell(cCategory), CategoryKind.INCOME)
                    else -> null
                }
                val t = Txn(
                    type = type, amount = if (type == TxType.ADJUST) signed else kotlin.math.abs(signed),
                    accountId = accountId, toAccountId = toAccountId,
                    categoryId = categoryId, note = cell(cNote).take(120), date = date,
                    tags = if (cTags >= 0) cell(cTags).split(';', ',', '|').map { it.trim().removePrefix("#") }
                        .filter { it.isNotEmpty() }.distinct().joinToString(",") else "",
                )
                val k = key(t)
                if (k in existing) { dupes++; continue }
                existing += k
                dao.insertTxn(t)
                imported++
            }
        }
        ImportResult(imported, dupes, skipped, newAccounts, newCategories)
    }

    private fun key(t: Txn) = "${t.date}|${t.type}|${t.amount}|${t.accountId}|${t.note.trim().lowercase()}"

    private fun parseDate(s: String): Long? {
        val v = s.trim().substringBefore('T').substringBefore("  ")
        if (v.isEmpty()) return null
        for (f in dateFormats) {
            try { return LocalDate.parse(v, f).toEpochDay() } catch (_: Exception) { }
        }
        // "2026-10-12 14:30" style
        return v.split(' ').firstOrNull()?.let { first ->
            dateFormats.firstNotNullOfOrNull { f -> runCatching { LocalDate.parse(first, f).toEpochDay() }.getOrNull() }
        }
    }

    /** "₱1,234.50", "-1234.5", "(1,234.50)", "1.234,50"? → cents. Returns null when not a number. */
    private fun parseAmount(s: String): Long? {
        var v = s.trim()
        if (v.isEmpty()) return null
        var negative = false
        if (v.startsWith("(") && v.endsWith(")")) { negative = true; v = v.substring(1, v.length - 1) }
        if (v.endsWith("-")) { negative = true; v = v.dropLast(1) }
        v = v.filter { it.isDigit() || it == '.' || it == '-' || it == ',' }
        if (v.startsWith("-")) { negative = !negative; v = v.drop(1) }
        // European style "1.234,50" → "1234.50"
        if (v.contains(',') && v.contains('.') && v.lastIndexOf(',') > v.lastIndexOf('.')) {
            v = v.replace(".", "").replace(',', '.')
        } else {
            v = v.replace(",", "")
        }
        if (v.isEmpty() || v == ".") return null
        return try {
            val cents = BigDecimal(v).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
            if (negative) -cents else cents
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** RFC-4180-ish CSV parser: quoted fields, escaped quotes, commas/newlines inside quotes. Also handles ; and tab files. */
    fun parse(text: String): List<List<String>> {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        val sep = listOf(',', ';', '\t').maxByOrNull { c -> firstLine.count { it == c } } ?: ','
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                quoted && ch == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                !quoted && ch == sep -> { row += field.toString(); field.clear() }
                !quoted && (ch == '\n' || ch == '\r') -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row += field.toString(); field.clear()
                    rows += row; row = mutableListOf()
                }
                else -> field.append(ch)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row += field.toString(); rows += row }
        return rows
    }
}
