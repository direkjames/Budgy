package com.budgy.app.ui

import com.budgy.app.i18n.tr
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.budgy.app.ui.screens.AccountsScreen
import com.budgy.app.ui.screens.ActivityScreen
import com.budgy.app.ui.screens.BudgetsScreen
import com.budgy.app.ui.screens.CategoriesScreen
import com.budgy.app.ui.screens.EntryScreen
import com.budgy.app.ui.screens.HomeScreen
import com.budgy.app.ui.screens.SettingsScreen
import com.budgy.app.ui.screens.ReportsScreen
import com.budgy.app.ui.screens.RecurringScreen
import com.budgy.app.ui.screens.GoalsScreen
import com.budgy.app.ui.screens.IousScreen
import com.budgy.app.ui.screens.DebtsScreen
import com.budgy.app.ui.screens.NetWorthScreen
import com.budgy.app.ui.screens.TagsScreen
import com.budgy.app.ui.screens.YearReviewScreen
import com.budgy.app.ui.screens.BadgesScreen
import com.budgy.app.ui.screens.HelpScreen
import com.budgy.app.ui.screens.InvestmentsScreen
import com.budgy.app.ui.screens.HoldingScreen
import com.budgy.app.ui.screens.HoldingEditorScreen
import com.budgy.app.ui.screens.SubscriptionEditorScreen
import com.budgy.app.ui.screens.SubscriptionsScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Rounded.Home),
    Tab("activity", "Activity", Icons.AutoMirrored.Rounded.ReceiptLong),
    Tab("budgets", "Budgets", Icons.Rounded.DonutLarge),
    Tab("subs", "Subscriptions", Icons.Rounded.Autorenew),
)

/** Navigation helpers so screens don't need to know route strings. */
class BudgyNav(private val nav: NavHostController) {
    fun back() { nav.popBackStack() }
    fun newEntry(type: String = "EXPENSE") = nav.navigate("entry?id=-1&type=$type")
    fun editEntry(id: Long) = nav.navigate("entry?id=$id&type=EXPENSE")
    fun newSubscription() = nav.navigate("sub?id=-1")
    fun editSubscription(id: Long) = nav.navigate("sub?id=$id")
    fun accounts() = nav.navigate("accounts")
    fun categories() = nav.navigate("categories")
    fun settings() = nav.navigate("settings")
    fun reports() = nav.navigate("reports")
    fun recurring() = nav.navigate("recurring")
    fun goals() = nav.navigate("goals")
    fun ious() = nav.navigate("ious")
    fun debts() = nav.navigate("debts")
    fun networth() = nav.navigate("networth")
    fun tags() = nav.navigate("tags")
    fun year() = nav.navigate("year")
    fun badges() = nav.navigate("badges")
    fun help() = nav.navigate("help")
    fun investments() = nav.navigate("investments")
    fun holding(id: Long) = nav.navigate("holding?id=$id")
    fun editHolding(id: Long = -1L) = nav.navigate("holdingEdit?id=$id")
    /** After adding a new investment, open it instead of going back to the editor. */
    fun replaceWithHolding(id: Long) = nav.navigate("holding?id=$id") {
        popUpTo("holdingEdit?id={id}") { inclusive = true }
    }
    fun tab(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun BudgyRoot(vm: BudgyViewModel, quickAdd: String? = null, onQuickAddHandled: () -> Unit = {}) {
    val nav = rememberNavController()
    val go = remember(nav) { BudgyNav(nav) }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val isTab = tabs.any { it.route == route }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }
    // Opened from the home screen widget's "Add" button
    LaunchedEffect(quickAdd) {
        if (quickAdd != null) {
            go.newEntry(quickAdd)
            onQuickAddHandled()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (route == "home" || route == "activity") {
                ExtendedFloatingActionButton(
                    onClick = { go.newEntry() },
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text(tr("Add")) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        bottomBar = {
            if (isTab) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { go.tab(t.route) },
                            icon = { Icon(t.icon, null) },
                            label = { Text(com.budgy.app.i18n.tr(t.label), maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm, go) }
            composable("activity") { ActivityScreen(vm, go) }
            composable("budgets") { BudgetsScreen(vm, go) }
            composable("subs") { SubscriptionsScreen(vm, go) }
            composable(
                "entry?id={id}&type={type}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("type") { type = NavType.StringType; defaultValue = "EXPENSE" },
                ),
            ) { e ->
                EntryScreen(
                    vm, go,
                    id = e.arguments?.getLong("id") ?: -1L,
                    initialType = e.arguments?.getString("type") ?: "EXPENSE",
                )
            }
            composable(
                "sub?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { e ->
                SubscriptionEditorScreen(vm, go, id = e.arguments?.getLong("id") ?: -1L)
            }
            composable("accounts") { AccountsScreen(vm, go) }
            composable("categories") { CategoriesScreen(vm, go) }
            composable("settings") { SettingsScreen(vm, go) }
            composable("reports") { ReportsScreen(vm, go) }
            composable("recurring") { RecurringScreen(vm, go) }
            composable("goals") { GoalsScreen(vm, go) }
            composable("ious") { IousScreen(vm, go) }
            composable("debts") { DebtsScreen(vm, go) }
            composable("networth") { NetWorthScreen(vm, go) }
            composable("tags") { TagsScreen(vm, go) }
            composable("year") { YearReviewScreen(vm, go) }
            composable("badges") { BadgesScreen(vm, go) }
            composable("help") { HelpScreen(vm, go) }
            composable("investments") { InvestmentsScreen(vm, go) }
            composable(
                "holding?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { e -> HoldingScreen(vm, go, id = e.arguments?.getLong("id") ?: -1L) }
            composable(
                "holdingEdit?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { e -> HoldingEditorScreen(vm, go, id = e.arguments?.getLong("id") ?: -1L) }
        }
    }
}
