package com.budgy.app.notify

import com.budgy.app.i18n.tr
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.budgy.app.BudgyApp
import com.budgy.app.MainActivity
import com.budgy.app.R
import com.budgy.app.util.Dates
import com.budgy.app.util.Money
import com.budgy.app.util.keyDate
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Runs once a day (around 9 AM) and whenever the app opens. Sends one notification per upcoming
 * bill or trial end inside each subscription's reminder window. Each (subscription, date) pair is
 * only notified once, unless [KEY_FORCE] is set (used by "Send a test reminder").
 */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as BudgyApp
        // Daily housekeeping: log due recurring entries and refresh the widget's "today" numbers.
        runCatching { app.repository.processRecurring() }
        runCatching { com.budgy.app.widget.BudgyWidget.updateAll(applicationContext) }
        val settings = app.settings.settings.first()
        val force = inputData.getBoolean(KEY_FORCE, false)
        if (!settings.reminders && !force) return Result.success()
        if (!canNotify(applicationContext)) return Result.success()

        val today = Dates.today()
        val prefs = applicationContext.getSharedPreferences("reminder_log", Context.MODE_PRIVATE)
        val sent = prefs.getStringSet("sent", emptySet())!!.toMutableSet()
        val lines = mutableListOf<String>()

        for (sub in app.repository.subscriptionsOnce().filter { it.active }) {
            val due = sub.keyDate()
            val days = due - today
            val window = if (force) 7 else sub.reminderDays.coerceAtLeast(0)
            if (days !in 0..window) continue
            val key = "${sub.id}:$due"
            if (!force && key in sent) continue
            sent += key
            val price = Money.format(sub.amount, settings.currency)
            val whenText = when (days) { 0L -> tr("today"); 1L -> tr("tomorrow"); else -> tr("in {0} days", days) }
            lines += if (sub.isTrial) {
                tr("{0} free trial ends {1} · then {2}", sub.name, whenText, price)
            } else {
                tr("{0} renews {1} · {2}", sub.name, whenText, price)
            }
        }

        // Keep the log small: forget entries for dates that have passed.
        sent.removeAll { it.substringAfter(':').toLongOrNull()?.let { d -> d < today - 1 } ?: true }
        prefs.edit().putStringSet("sent", sent).apply()

        if (lines.isEmpty() && force) {
            lines += tr("No bills or trials in the next 7 days. Reminders are working.")
        }
        if (lines.isNotEmpty()) show(lines)
        return Result.success()
    }

    private fun show(lines: List<String>) {
        val ctx = applicationContext
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val title = if (lines.size == 1) tr("Upcoming bill") else tr("{0} bills coming up", lines.size)
        val style = NotificationCompat.InboxStyle().also { s -> lines.forEach { s.addLine(it) } }
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_budgy)
            .setColor(0xFF0E6E52.toInt())
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(if (lines.size == 1) NotificationCompat.BigTextStyle().bigText(lines.first()) else style)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, n)
        } catch (e: SecurityException) {
            // Permission was revoked between the check and now; nothing to do.
        }
    }

    companion object {
        const val CHANNEL_ID = "bill_reminders"
        private const val NOTIFICATION_ID = 4201
        private const val KEY_FORCE = "force"
        private const val DAILY = "budgy_daily_reminders"
        private const val ON_OPEN = "budgy_check_on_open"
        private const val TEST = "budgy_test_reminder"

        fun createChannel(ctx: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID, tr("Bill & trial reminders"), NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Heads-up before subscriptions renew or free trials end" }
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun canNotify(ctx: Context): Boolean =
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        /** Daily check around 9:00 AM local time. */
        fun scheduleDaily(ctx: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(LocalTime.of(9, 0))
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next).toMinutes()
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        /** Quick check when the app opens, so nothing is missed if the phone was off at 9 AM. */
        fun checkNow(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<ReminderWorker>().build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(ON_OPEN, ExistingWorkPolicy.REPLACE, req)
        }

        /** Sends a notification immediately, ignoring the "already sent" log. */
        fun sendTest(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(workDataOf(KEY_FORCE to true))
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(TEST, ExistingWorkPolicy.REPLACE, req)
        }
    }
}
