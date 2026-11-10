package com.budgy.app

import android.app.Application
import com.budgy.app.data.BudgyDatabase
import com.budgy.app.data.BudgyRepository
import com.budgy.app.data.SettingsStore
import com.budgy.app.notify.ReminderWorker
import com.budgy.app.widget.BudgyWidget
import com.budgy.app.notify.Alerts
import com.budgy.app.notify.WeeklySummaryWorker
import com.budgy.app.i18n.L10n
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class BudgyApp : Application() {
    lateinit var repository: BudgyRepository
        private set
    lateinit var settings: SettingsStore
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        L10n.init(this)
        repository = BudgyRepository(BudgyDatabase.build(this))
        settings = SettingsStore(this)
        appScope.launch {
            repository.seedIfEmpty()
            repository.processRecurring()
        }
        // Keep the home screen widget in sync with your data.
        appScope.launch {
            combine(repository.transactions, repository.budgets, settings.settings) { _, _, _ -> Unit }
                .debounce(500)
                .collect {
                    runCatching { BudgyWidget.updateAll(this@BudgyApp) }
                    runCatching { Alerts.check(this@BudgyApp) }
                }
        }
        // Apply the in-app language everywhere (screens, widget, notifications).
        appScope.launch {
            settings.settings.map { it.language }.distinctUntilChanged().collect { L10n.lang = it }
        }
        ReminderWorker.createChannel(this)
        ReminderWorker.scheduleDaily(this)
        ReminderWorker.checkNow(this)
        Alerts.createChannels(this)
        WeeklySummaryWorker.schedule(this)
    }
}
