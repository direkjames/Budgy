package com.budgy.app

import com.budgy.app.i18n.tr
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgy.app.ui.AppLock
import com.budgy.app.i18n.L10n
import com.budgy.app.ui.BudgyRoot
import com.budgy.app.ui.BudgyViewModel
import com.budgy.app.ui.components.BudgyLogo
import com.budgy.app.ui.theme.BudgyTheme

class MainActivity : FragmentActivity() {
    private val vm: BudgyViewModel by viewModels { BudgyViewModel.Factory }

    /** Locked until the user authenticates (only when App lock is on). */
    private val locked = mutableStateOf(true)
    private var backgroundedAt = 0L
    /** Transaction type to open from a widget or app-icon shortcut, e.g. "EXPENSE". */
    private val quickAdd = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState != null) locked.value = savedInstanceState.getBoolean("locked", true)
        handleIntent(intent)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val s = settings
            if (s == null) {
                // Settings load in a few ms; show the plain background meanwhile to avoid a theme flash.
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFF6F7F4)))
            } else {
                DisposableEffect(s.darkMode) {
                    val style = if (s.darkMode) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    onDispose {}
                }
                if (L10n.lang != s.language) L10n.lang = s.language
                BudgyTheme(dark = s.darkMode, currency = s.currency) {
                    LaunchedEffect(s.appLock) { if (!s.appLock) locked.value = false }
                    val isLocked = s.appLock && locked.value
                    if (isLocked) {
                        LaunchedEffect(Unit) { unlock() }
                        LockScreen(onUnlock = ::unlock)
                    } else {
                        BudgyRoot(vm, quickAdd = quickAdd.value, onQuickAddHandled = { quickAdd.value = null })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val type = when (intent?.getStringExtra(EXTRA_ACTION)) {
            ACTION_ADD, "add_expense" -> "EXPENSE"
            "add_income" -> "INCOME"
            "transfer" -> "TRANSFER"
            else -> null
        }
        if (type != null) {
            quickAdd.value = type
            intent?.removeExtra(EXTRA_ACTION)
        }
    }

    private fun unlock() {
        AppLock.prompt(this, tr("Unlock Budgy"), onSuccess = { locked.value = false })
    }

    override fun onStop() {
        super.onStop()
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Lock again if Budgy was in the background for more than 30 seconds.
        if (backgroundedAt != 0L && SystemClock.elapsedRealtime() - backgroundedAt > 30_000) locked.value = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("locked", locked.value)
    }

    companion object {
        const val EXTRA_ACTION = "budgy_action"
        const val ACTION_ADD = "add"
    }
}

@androidx.compose.runtime.Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BudgyLogo(88.dp)
        Spacer(Modifier.height(16.dp))
        Text(tr("Budgy is locked"), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onUnlock, shape = RoundedCornerShape(16.dp)) { Text(tr("Unlock")) }
    }
}
