package com.budgy.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.budgy.app.BudgyApp
import com.budgy.app.MainActivity
import com.budgy.app.R
import com.budgy.app.data.TxType
import com.budgy.app.util.Dates
import com.budgy.app.util.Money
import com.budgy.app.util.currentPeriod
import com.budgy.app.util.daysLeftIn
import com.budgy.app.util.toHomeTxns
import com.budgy.app.i18n.L10n
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Home screen widget: how much you can still spend today (from your budgets),
 * plus a button that opens straight to "Add expense".
 */
class BudgyWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BudgyWidget::class.java))
            if (ids.isEmpty()) return
            val views = build(context)
            manager.updateAppWidget(ids, views)
        }

        private suspend fun build(context: Context): RemoteViews {
            val app = context.applicationContext as BudgyApp
            val settings = app.settings.settings.first()
            val accounts = app.repository.accounts.first()
            val txns = toHomeTxns(app.repository.transactions.first(), accounts, settings.currency)
            val budgets = app.repository.budgets.first()
            val cur = settings.currency
            val dark = settings.darkMode

            val today = Dates.today()
            val period = currentPeriod(settings.period)
            val monthRange = Dates.monthRange(YearMonth.now())
            val expenses = txns.filter { it.type == TxType.EXPENSE && (it.date in period || it.date in monthRange) }

            val title: String
            val amount: String
            val sub: String
            var negative = false
            if (budgets.isNotEmpty()) {
                val budgeted = budgets.map { it.categoryId }.toSet()
                val limit = budgets.sumOf { it.monthlyLimit }
                val mine = expenses.filter { it.categoryId in budgeted && it.date in period }
                val before = mine.filter { it.date < today }.sumOf { it.amount }
                val todaySpent = mine.filter { it.date == today }.sumOf { it.amount }
                val daysLeft = daysLeftIn(period)
                val allowance = (limit - before) / daysLeft
                val left = allowance - todaySpent
                negative = left < 0
                title = if (negative) L10n.tr("Over today's budget by") else L10n.tr("Left to spend today")
                amount = Money.format(if (negative) -left else left, cur)
                sub = L10n.tr("{0} left this period", Money.format(limit - before - todaySpent, cur))
            } else {
                title = L10n.tr("Spent today")
                amount = Money.format(expenses.filter { it.date == today }.sumOf { it.amount }, cur)
                sub = L10n.tr("{0} this month · set a budget for a daily limit", Money.format(expenses.filter { it.date in monthRange }.sumOf { it.amount }, cur))
            }

            val v = RemoteViews(context.packageName, R.layout.widget_budgy)
            v.setTextViewText(R.id.widget_title, title)
            v.setTextViewText(R.id.widget_amount, amount)
            v.setTextViewText(R.id.widget_add, "+  " + L10n.tr("Add expense"))
            v.setTextViewText(R.id.widget_sub, sub)

            v.setInt(R.id.widget_root, "setBackgroundResource", if (dark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light)
            v.setInt(R.id.widget_add, "setBackgroundResource", if (dark) R.drawable.widget_btn_dark else R.drawable.widget_btn_light)
            v.setTextColor(R.id.widget_add, if (dark) 0xFF00291C.toInt() else 0xFFFFFFFF.toInt())
            v.setTextColor(R.id.widget_title, if (dark) 0xFF9CA8A1.toInt() else 0xFF5F6A64.toInt())
            v.setTextColor(R.id.widget_sub, if (dark) 0xFF9CA8A1.toInt() else 0xFF5F6A64.toInt())
            v.setTextColor(
                R.id.widget_amount,
                when {
                    negative -> if (dark) 0xFFFF7A70.toInt() else 0xFFDF4A43.toInt()
                    dark -> 0xFFE9EFEB.toInt()
                    else -> 0xFF131915.toInt()
                },
            )

            val open = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val add = PendingIntent.getActivity(
                context, 2,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_ADD)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            v.setOnClickPendingIntent(R.id.widget_root, open)
            v.setOnClickPendingIntent(R.id.widget_add, add)
            return v
        }
    }
}
