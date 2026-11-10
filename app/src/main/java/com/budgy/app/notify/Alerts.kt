package com.budgy.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.budgy.app.BudgyApp
import com.budgy.app.MainActivity
import com.budgy.app.R
import com.budgy.app.data.TxType
import com.budgy.app.i18n.L10n
import com.budgy.app.util.Money
import com.budgy.app.util.currentPeriod
import com.budgy.app.util.noSpendStreaks
import com.budgy.app.util.spendingInsights
import com.budgy.app.util.toHomeTxns
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/** Budget threshold alerts, spending alerts and the weekly summary (V4). */
object Alerts {
    const val CHANNEL_BUDGET = "budget_alerts"
    const val CHANNEL_SUMMARY = "weekly_summary"
    private const val LOG = "alert_log"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_BUDGET, "Budget & spending alerts", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "When a budget reaches 80% or 100%, or a category runs higher than usual" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SUMMARY, "Weekly summary", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "A short recap every Sunday evening" }
        )
    }

    private fun openApp(ctx: Context, code: Int): PendingIntent = PendingIntent.getActivity(
        ctx, code,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun post(ctx: Context, channel: String, id: Int, title: String, text: String) {
        if (!ReminderWorker.canNotify(ctx)) return
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_budgy)
            .setColor(0xFF0E6E52.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(ctx, id))
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) { }
    }

    /**
     * Called whenever your data changes. Sends each alert at most once:
     * per budget per period for 80% and 100%, and per category per month for spending alerts.
     */
    suspend fun check(app: BudgyApp) {
        val s = app.settings.settings.first()
        if (!s.budgetAlerts && !s.smartAlerts) return
        val repo = app.repository
        val accounts = repo.accounts.first()
        val txns = toHomeTxns(repo.transactions.first(), accounts, s.currency)
        val prefs = app.getSharedPreferences(LOG, Context.MODE_PRIVATE)
        val sent = prefs.getStringSet("sent", emptySet())!!.toMutableSet()
        val before = sent.size
        val categories = repo.categories.first().associateBy { it.id }

        if (s.budgetAlerts) {
            val period = currentPeriod(s.period)
            val spent = txns.filter { it.type == TxType.EXPENSE && it.date in period }
                .groupBy { it.categoryId }.mapValues { (_, v) -> v.sumOf { it.amount } }
            for (b in repo.budgets.first()) {
                if (b.monthlyLimit <= 0) continue
                val used = spent[b.categoryId] ?: 0L
                val pct = used * 100 / b.monthlyLimit
                val name = categories[b.categoryId]?.name?.let { L10n.tr(it) } ?: continue
                val k100 = "b:${b.id}:${period.first}:100"
                val k80 = "b:${b.id}:${period.first}:80"
                if (pct >= 100 && k100 !in sent) {
                    sent += k100; sent += k80
                    post(
                        app, CHANNEL_BUDGET, 5000 + b.id.toInt(),
                        L10n.tr("{0} budget used up", name),
                        L10n.tr("You've spent {0} of your {1} {2} budget.", Money.format(used, s.currency), Money.format(b.monthlyLimit, s.currency), name),
                    )
                } else if (pct >= 80 && k80 !in sent && k100 !in sent) {
                    sent += k80
                    post(
                        app, CHANNEL_BUDGET, 5000 + b.id.toInt(),
                        L10n.tr("{0} budget at {1}%", name, pct),
                        L10n.tr("{0} left of your {1} budget for this period.", Money.format(b.monthlyLimit - used, s.currency), name),
                    )
                }
            }
        }

        if (s.smartAlerts) {
            val ym = YearMonth.now()
            for (i in spendingInsights(txns, categories.values.toList()).filter { it.higher && it.pct >= 40 }) {
                val key = "i:${i.categoryId}:$ym"
                if (key in sent) continue
                sent += key
                post(
                    app, CHANNEL_BUDGET, 6000 + (i.categoryId ?: 0L).toInt(),
                    L10n.tr("Heads up: {0}", L10n.tr(i.name)),
                    L10n.tr("{0} is {1}% higher than your usual month so far ({2} vs {3}).", L10n.tr(i.name), i.pct,
                        Money.format(i.current, s.currency), Money.format(i.usual, s.currency)),
                )
            }
        }

        if (sent.size != before) {
            // Forget keys for old periods/months so the log stays small.
            val cutoff = currentPeriod(s.period).first - 120
            sent.removeAll { k -> k.startsWith("b:") && (k.split(':').getOrNull(2)?.toLongOrNull() ?: 0L) < cutoff }
            prefs.edit().putStringSet("sent", sent).apply()
        }
    }

    /** "This week you spent ₱3,240 (₱500 less than last week)…" */
    suspend fun weeklySummary(app: BudgyApp, force: Boolean) {
        val s = app.settings.settings.first()
        if (!s.weeklySummary && !force) return
        val repo = app.repository
        val raw = repo.transactions.first()
        val txns = toHomeTxns(raw, repo.accounts.first(), s.currency)
        val today = java.time.LocalDate.now().toEpochDay()
        val week = (today - 6)..today
        val prevWeek = (today - 13)..(today - 7)
        fun spentIn(r: LongRange) = txns.filter { it.type == TxType.EXPENSE && it.date in r }.sumOf { it.amount }
        val now = spentIn(week)
        val before = spentIn(prevWeek)
        val cur = s.currency
        val lines = mutableListOf<String>()
        lines += when {
            before == 0L -> L10n.tr("This week you spent {0}.", Money.format(now, cur))
            now <= before -> L10n.tr("This week you spent {0}, {1} less than last week.", Money.format(now, cur), Money.format(before - now, cur))
            else -> L10n.tr("This week you spent {0}, {1} more than last week.", Money.format(now, cur), Money.format(now - before, cur))
        }
        val budgets = repo.budgets.first()
        if (budgets.isNotEmpty()) {
            val period = currentPeriod(s.period)
            val ids = budgets.map { it.categoryId }.toSet()
            val used = txns.filter { it.type == TxType.EXPENSE && it.date in period && it.categoryId in ids }.sumOf { it.amount }
            val left = budgets.sumOf { it.monthlyLimit } - used
            lines += if (left >= 0) L10n.tr("{0} left in your budgets this period.", Money.format(left, cur))
            else L10n.tr("You're {0} over your budgets this period.", Money.format(-left, cur))
        }
        val streak = noSpendStreaks(raw).current
        if (streak > 0) lines += L10n.tr("No-spend streak: {0} day(s). Keep it up!", streak)
        post(app, CHANNEL_SUMMARY, 4300, L10n.tr("Your week in Budgy"), lines.joinToString(" "))
    }
}

class WeeklySummaryWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Alerts.weeklySummary(applicationContext as BudgyApp, inputData.getBoolean("force", false))
        return Result.success()
    }

    companion object {
        private const val NAME = "budgy_weekly_summary"

        /** Every Sunday around 6 PM. */
        fun schedule(ctx: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.of(18, 0))
            if (!next.isAfter(now)) next = next.plusWeeks(1)
            val req = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun sendNow(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<WeeklySummaryWorker>()
                .setInputData(androidx.work.workDataOf("force" to true))
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("$NAME-now", ExistingWorkPolicy.REPLACE, req)
        }
    }
}
