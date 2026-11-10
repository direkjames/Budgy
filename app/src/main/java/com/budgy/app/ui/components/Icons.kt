package com.budgy.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.budgy.app.data.AccountType

object CategoryIcons {
    val all: List<Pair<String, ImageVector>> = listOf(
        "restaurant" to Icons.Rounded.Restaurant,
        "groceries" to Icons.Rounded.LocalGroceryStore,
        "coffee" to Icons.Rounded.LocalCafe,
        "commute" to Icons.Rounded.DirectionsBus,
        "car" to Icons.Rounded.DirectionsCar,
        "fuel" to Icons.Rounded.LocalGasStation,
        "bolt" to Icons.Rounded.Bolt,
        "bills" to Icons.AutoMirrored.Rounded.ReceiptLong,
        "wifi" to Icons.Rounded.Wifi,
        "phone" to Icons.Rounded.PhoneAndroid,
        "home" to Icons.Rounded.Home,
        "shopping" to Icons.Rounded.ShoppingBag,
        "health" to Icons.Rounded.MedicalServices,
        "fitness" to Icons.Rounded.FitnessCenter,
        "beauty" to Icons.Rounded.Spa,
        "fun" to Icons.Rounded.SportsEsports,
        "movie" to Icons.Rounded.Movie,
        "music" to Icons.Rounded.MusicNote,
        "subscriptions" to Icons.Rounded.Subscriptions,
        "education" to Icons.Rounded.School,
        "travel" to Icons.Rounded.Flight,
        "gift" to Icons.Rounded.CardGiftcard,
        "pets" to Icons.Rounded.Pets,
        "kids" to Icons.Rounded.ChildCare,
        "salary" to Icons.Rounded.Payments,
        "allowance" to Icons.Rounded.AccountBalanceWallet,
        "business" to Icons.Rounded.Work,
        "invest" to Icons.AutoMirrored.Rounded.TrendingUp,
        "savings" to Icons.Rounded.Savings,
        "other" to Icons.Rounded.Category,
    )
    private val map = all.toMap()
    fun get(key: String?): ImageVector = map[key] ?: Icons.Rounded.Category
}

fun AccountType.icon(): ImageVector = when (this) {
    AccountType.CASH -> Icons.Rounded.Payments
    AccountType.BANK -> Icons.Rounded.AccountBalance
    AccountType.EWALLET -> Icons.Rounded.AccountBalanceWallet
    AccountType.CARD -> Icons.Rounded.CreditCard
}
