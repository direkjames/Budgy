package com.budgy.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "budgy_settings")

data class Settings(
    val darkMode: Boolean = false,          // Light is the default
    val currency: String = "₱",
    val reminders: Boolean = true,
    val lastAccountId: Long = -1,
    val lastExpenseCategoryId: Long = -1,
    val lastIncomeCategoryId: Long = -1,
    val appLock: Boolean = false,
    val envelopeMode: Boolean = false,
    val periodMode: String = "MONTHLY",   // V4: MONTHLY or TWICE
    val periodStartDay: Int = 1,          // V4: 1..28 for MONTHLY
    val budgetAlerts: Boolean = true,     // V4: notify at 80% and 100%
    val weeklySummary: Boolean = true,    // V4
    val smartAlerts: Boolean = true,      // V4: "Food is 40% higher than usual"
    val language: String = "en",          // V4: en, fil, ceb
    val helpSeen: Boolean = false,        // V4: in-app guide opened or dismissed
    val livePrices: Boolean = true,       // V5: get crypto prices from CoinGecko
    val internetNoticeSeen: Boolean = false, // V5: nothing goes online until you've read why
) {
    /** Budgy may go online for prices only after you've read why, and only when live prices are on. */
    val canUseInternet get() = livePrices && internetNoticeSeen

    val period get() = com.budgy.app.util.PeriodSpec(
        runCatching { com.budgy.app.util.PeriodMode.valueOf(periodMode) }.getOrDefault(com.budgy.app.util.PeriodMode.MONTHLY),
        periodStartDay,
    )
}

class SettingsStore(private val context: Context) {
    private object Keys {
        val dark = booleanPreferencesKey("dark_mode")
        val currency = stringPreferencesKey("currency")
        val reminders = booleanPreferencesKey("reminders")
        val lastAccount = longPreferencesKey("last_account")
        val lastExpense = longPreferencesKey("last_expense_category")
        val lastIncome = longPreferencesKey("last_income_category")
        val appLock = booleanPreferencesKey("app_lock")
        val envelope = booleanPreferencesKey("envelope_mode")
        val periodMode = stringPreferencesKey("period_mode")
        val periodDay = intPreferencesKey("period_start_day")
        val budgetAlerts = booleanPreferencesKey("budget_alerts")
        val weekly = booleanPreferencesKey("weekly_summary")
        val smart = booleanPreferencesKey("smart_alerts")
        val language = stringPreferencesKey("language")
        val helpSeen = booleanPreferencesKey("help_seen")
        val livePrices = booleanPreferencesKey("live_prices")
        val internetNotice = booleanPreferencesKey("internet_notice_seen")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            darkMode = p[Keys.dark] ?: false,
            currency = p[Keys.currency] ?: "₱",
            reminders = p[Keys.reminders] ?: true,
            lastAccountId = p[Keys.lastAccount] ?: -1,
            lastExpenseCategoryId = p[Keys.lastExpense] ?: -1,
            lastIncomeCategoryId = p[Keys.lastIncome] ?: -1,
            appLock = p[Keys.appLock] ?: false,
            envelopeMode = p[Keys.envelope] ?: false,
            periodMode = p[Keys.periodMode] ?: "MONTHLY",
            periodStartDay = p[Keys.periodDay] ?: 1,
            budgetAlerts = p[Keys.budgetAlerts] ?: true,
            weeklySummary = p[Keys.weekly] ?: true,
            smartAlerts = p[Keys.smart] ?: true,
            language = p[Keys.language] ?: "en",
            helpSeen = p[Keys.helpSeen] ?: false,
            livePrices = p[Keys.livePrices] ?: true,
            internetNoticeSeen = p[Keys.internetNotice] ?: false,
        )
    }

    suspend fun setDarkMode(v: Boolean) { context.dataStore.edit { it[Keys.dark] = v } }
    suspend fun setCurrency(v: String) { context.dataStore.edit { it[Keys.currency] = v } }
    suspend fun setReminders(v: Boolean) { context.dataStore.edit { it[Keys.reminders] = v } }
    suspend fun setAppLock(v: Boolean) { context.dataStore.edit { it[Keys.appLock] = v } }
    suspend fun setEnvelopeMode(v: Boolean) { context.dataStore.edit { it[Keys.envelope] = v } }
    suspend fun setPeriod(mode: String, day: Int) { context.dataStore.edit { it[Keys.periodMode] = mode; it[Keys.periodDay] = day } }
    suspend fun setBudgetAlerts(v: Boolean) { context.dataStore.edit { it[Keys.budgetAlerts] = v } }
    suspend fun setWeeklySummary(v: Boolean) { context.dataStore.edit { it[Keys.weekly] = v } }
    suspend fun setSmartAlerts(v: Boolean) { context.dataStore.edit { it[Keys.smart] = v } }
    suspend fun setLanguage(v: String) { context.dataStore.edit { it[Keys.language] = v } }
    suspend fun setHelpSeen(v: Boolean) { context.dataStore.edit { it[Keys.helpSeen] = v } }
    /** Choosing live or manual prices also counts as having read the internet notice. */
    suspend fun setLivePrices(v: Boolean) { context.dataStore.edit { it[Keys.livePrices] = v; it[Keys.internetNotice] = true } }

    suspend fun rememberLastUsed(type: TxType, accountId: Long, categoryId: Long?) {
        context.dataStore.edit {
            it[Keys.lastAccount] = accountId
            if (categoryId != null) {
                when (type) {
                    TxType.EXPENSE -> it[Keys.lastExpense] = categoryId
                    TxType.INCOME -> it[Keys.lastIncome] = categoryId
                    else -> Unit
                }
            }
        }
    }
}
